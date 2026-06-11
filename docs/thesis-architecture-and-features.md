# 社区医院管理系统 — 架构与功能说明（论文素材，2026-06 修订版）

> 本文档汇总系统的整体架构、前后端技术栈、模块划分、接口清单、关键实现机制、安全设计与工程化体系，可直接用作毕业论文/学位论文的「需求分析」「系统设计与实现」「关键技术」「系统测试」「安全设计」等章节素材。
> 本版与代码库当前状态对齐：Flyway V1–V54、后端 154 个测试、前端 18 个单测、CI 流水线与生产部署链路均已落地。

---

## 1. 项目概述

### 1.1 背景与目标

本系统是一套面向社区医院（基层医疗机构）的综合管理平台。原系统为 Spring Boot 2 + Vue 2 + MySQL 单体架构（MyBatis、拼音命名、Session 鉴权、无测试无迁移管理），本项目将其全栈重构为 **Spring Boot 3 + Next.js 15 + PostgreSQL** 的现代化架构，并在重构基础上完成三类增量建设：

1. **智能化**：引入大语言模型能力——病历照片 OCR 结构化识别、患者私域 RAG 问询（带强制隐私隔离）、社区 AI 问诊（SSE 流式多轮对话）；
2. **业务深化**：将孤立 CRUD 串联为完整诊疗链路（科室→医生→挂号→病历→处方自动扣库存），新增慢病随访、家医签约、患者 360° 视图、可视化权限管理；
3. **工程化**：Flyway 版本化迁移、TestContainers 全模块集成测试、GitHub Actions 持续集成、多阶段 Docker 镜像与一键生产部署。

### 1.2 技术构成

- 后端：`server/backend-rewrite/`（Java 21 + Spring Boot 3.3.5）
- 前端：`web/`（Next.js 15 App Router + React 19 + TypeScript 5 strict）
- 数据库：PostgreSQL 16（业务数据，Flyway V1–V54） + Qdrant（AI 向量）
- AI 服务：火山引擎方舟 Doubao（OpenAI 兼容协议）
- 基础设施：Docker / Docker Compose（开发数据层 + 生产四容器编排）、GitHub Actions（CI）

### 1.3 角色与能力矩阵

| 角色 | 角色码 | 典型能力 |
|---|---|---|
| 管理员 | ADMIN | 全部模块管理、角色权限矩阵配置、AI 管理（向量回填等） |
| 医生 | DOCTOR | 就诊/病历读写、患者查阅、AI 病历识别、患者私域问询 |
| 家庭医生 | FAMILY_DOCTOR | 签约患者管理、慢病随访、患者查阅 |
| 前台 | RECEPTION | 挂号登记、患者建档读写、医生/科室/签约/随访只读（V52 授权） |
| 患者 | PATIENT | 个人信息、社区 AI 问诊 |
| 普通注册用户 | USER | 注册默认角色，仅基础访问 |

权限并非按角色硬编码，而是经「角色 ↔ 权限码」多对多关系动态决定，管理员可在 `/roles` 页面随时调整（见 §5.1）。

---

## 2. 总体架构

### 2.1 架构分层

```
┌─────────────────────────────────────────────────────────────┐
│  Web 前端（Next.js 15 App Router + React 19 + Tailwind）       │
│  - Pages（app/，17 个业务页）                                  │
│  - 业务组件（components/business，配置驱动 CRUD 通用页）        │
│  - UI 原语（components/ui）  - API 客户端（lib/api.ts）        │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTPS / JSON / SSE
┌──────────────────────────▼──────────────────────────────────┐
│  Spring Boot 3 后端                                          │
│  ├─ 接入层：SecurityConfig + JwtAuthenticationFilter         │
│  │            LoginAttemptService（登录失败限流）              │
│  │            TraceIdFilter + ApiMetricsFilter               │
│  ├─ Controller 层（@RestController, /api/v1/*, 权限注解）     │
│  ├─ Service 层（DefaultXxxService，业务规则与事务）            │
│  ├─ Repository 层（JPA Repository 写 + JdbcClient 复杂读）    │
│  ├─ 跨切面：ApiExceptionHandler / AuditService                │
│  └─ AI 子系统：AiCallTemplate + Vision / RAG / Consult       │
└──────────┬───────────────────────┬──────────────────────────┘
           │                       │
   ┌───────▼─────────┐     ┌───────▼─────────┐     ┌──────────────┐
   │  PostgreSQL 16   │     │  Qdrant 向量库   │     │ Doubao（火山）│
   │  - 业务表 / RBAC │     │  - patient_     │     │ - 文本/视觉   │
   │  - 审计 / 字典    │     │    knowledge    │     │ - Embedding  │
   └─────────────────┘     └─────────────────┘     └──────────────┘
```

### 2.2 一次典型请求的生命周期

1. 浏览器发起 `fetch`，`lib/api.ts` 统一注入 `Authorization: Bearer <JWT>` 与 `X-Trace-Id`；
2. `TraceIdFilter` 接收/生成 TraceId 写入 MDC → `JwtAuthenticationFilter` 解析 Token，从数据库**实时加载**该用户权限码集合注入 `Authentication`（权限调整即时生效，无需重新登录）；
3. Controller 上的 `@PreAuthorize("hasAuthority('xxx:write')")` 做细粒度授权；
4. Service 执行业务规则（唯一性校验、状态机、库存差量等），写路径走 JPA，读路径走 JdbcClient 联表 SQL；
5. 异常由 `ApiExceptionHandler` 统一翻译为 HTTP 语义（404/400/409/412/429/500）；
6. `ApiMetricsFilter` 记录耗时指标；前端 `lib/api.ts` 对 401 统一登出重定向。

### 2.3 部署形态

**开发态**（docker-compose.yml 只起数据层，应用本地运行）：

| 组件 | 端口 | 启动方式 |
|---|---|---|
| PostgreSQL（pgvector/pgvector:pg16 镜像） | 5432 | `docker compose up -d postgres` |
| Qdrant v1.12（gRPC / REST） | 6334 / 6333 | `docker compose up -d qdrant` |
| Spring Boot | 8089 | `mvn spring-boot:run` |
| Next.js | 3000 | `npm run dev`（`NEXT_PUBLIC_API_BASE_URL` 指向后端） |

**生产态**（docker-compose.prod.yml，单机四容器，详见 `docs/deployment.md`）：

| 服务 | 镜像来源 | 要点 |
|---|---|---|
| postgres | pgvector/pgvector:pg16 | 命名卷持久化；**不暴露宿主端口**，仅 compose 内网可达；pg_isready 健康检查 |
| qdrant | qdrant/qdrant:v1.12.4 | 命名卷持久化 |
| backend | 多阶段 Dockerfile：maven 编译层 → 仅 JRE 运行层 | 非 root 用户运行；`SPRING_PROFILES_ACTIVE=prod`；对外 8080 |
| web | 多阶段 Dockerfile：pnpm 依赖层 → next build（standalone 自包含产物）→ node 运行层 | 非 root；对外 3000；`NEXT_PUBLIC_API_BASE_URL` 构建期内联 |

生产配置遵循 **fail-fast 原则**：`application-prod.yml` 中数据库口令（`SPRING_DATASOURCE_PASSWORD`）与 JWT 密钥（`HOSPITAL_JWT_SECRET`，≥32 字节）**没有默认值**，compose 层再以 `${VAR:?}` 强制必填——环境变量缺失时部署直接失败，从机制上杜绝占位符密钥混入生产。AI 默认关闭（`HOSPITAL_AI_ENABLED=false`），提供 API Key 后可开启。

部署、升级（`git pull` + `up -d --build`，Flyway 自动迁移）、备份（`pg_dump` + 卷备份）流程均已文档化。

---

## 3. 后端架构

### 3.1 技术栈

| 类别 | 技术 | 用途 |
|---|---|---|
| 运行时 | Java 21 + Spring Boot 3.3.5 | Web + IoC 容器 |
| Web | Spring MVC + Spring WebFlux（WebClient） | REST + SSE 流式响应 |
| 安全 | Spring Security + JJWT 0.12 | JWT 鉴权、方法级授权、登录限流 |
| 持久化 | Spring Data JPA（Hibernate） + Spring JdbcClient | 实体写路径 + 复杂联表/分页读路径 |
| 数据库 | PostgreSQL 16 + Flyway 10 | 版本化迁移 V1–V54 |
| 向量库 | Qdrant Java SDK（gRPC 6334） | 患者私域向量检索 |
| AI 接入 | 自研 `AiCallTemplate`（OpenAI 兼容协议直连） | 统一审计/限流/重试/超时入口 |
| 限流 | Bucket4j（AI 配额） + 自研内存计数器（登录防爆破） | QPM、token 预算、账号锁定 |
| 监控 | Spring Actuator + Micrometer | API/AI 自定义指标 |
| 校验 | Jakarta Validation | DTO 参数校验（`@NotBlank`、`@Size` 等） |
| 测试 | JUnit 5 + Spring Test + Testcontainers 1.21.4 | 集成测试拉真实 PG 容器 |

### 3.2 模块结构与数据模型

每个业务模块统一目录约定：

```
com.hospital.<module>/
├── controller/   REST 端点（@RestController + @PreAuthorize 权限注解）
├── service/      Service 接口 + DefaultXxxService 实现（@Transactional）
├── repository/   JPA Repository（含唯一性 existsBy* 派生查询）
├── domain/       JPA Entity + 状态枚举
└── dto/          Request / Response DTO（record 类型）
```

业务模块全景（包名全部英文化，均已 JPA 持久化）：

| 模块 | 包名 | 主要表 | 关键字段/约束 |
|---|---|---|---|
| 认证与 RBAC | `auth` | `app_user`、`app_role`、`app_user_role`、`app_permission`、`app_role_permission` | username 唯一；BCrypt 哈希；权限码 `resource:action` 二段式 |
| 患者 | `patient` | `patient_profile` | V47 扩展出生日期/住址/过敏史/既往病史/紧急联系人；`ai_consent_at` AI 授权时间戳 |
| 医生 | `doctor` | `doctor_profile` | 工号 `uuid_number`/手机/身份证唯一；V51 增 `department_id` 归属科室 |
| 家庭医生 | `familydoctor` | `family_doctor_profile` | 同 app_user+profile 模式，无工号字段 |
| 家医签约 | `familydoctor`（ContractController） | `family_doctor_contract` | **部分唯一索引**：`WHERE status='ACTIVE'` 时 patient_id 唯一 → 一患者一份生效签约；状态 ACTIVE/TERMINATED/EXPIRED |
| 前台 | `reception` | `reception_profile` | 工号/手机唯一 |
| 科室 | `department` | `department` | 名称唯一；删除校验就诊引用 |
| 药品 | `medication` | `medication`、`medication_inventory_log` | `CHECK (stock >= 0)`；流水记 delta/stock_after/reason/operator |
| 就诊 | `visit` | `visit_record` | 就诊号唯一（`JZ+时间戳+随机` 自动生成）；V51 增 `doctor_id` |
| 病历 | `medicalrecord` | `medical_record` | 病历号自动生成；`prescription_items`/`attachments`/`ai_extracted` JSONB；V51 增 `visit_id`；状态 DRAFT/ACTIVE/ARCHIVED |
| 慢病随访 | `followup` | `patient_followup` | 血压/血糖/身高体重/心率；BMI 服务端计算 |
| 字典 | `dictionary` | `dictionary_item`、`dictionary_operation_log` | type/code 树形；写操作审计 |
| 系统配置 | `systemconfig` | `system_config` | key-value，含 `/effective` 公开生效项查询 |
| 照片 | `photo` | `photo` | BYTEA 入库；**UUID 主键防遍历** |
| 首页聚合 | `dashboard` | —（跨模块只读） | 按当前用户权限裁剪返回指标 |
| 可观测性 | `observability` | — | TraceIdFilter / ApiMetricsFilter |
| AI 子系统 | `ai.{client,vision,embedding,ingestion,rag,consult,qdrant,ratelimit,audit,common,admin,config}` | `ai_audit_log`、`ai_extraction_history`、`ai_consult_session`、`ai_consult_message`、`ai_dead_letter` | 见 §6 |

> **重构演进说明**（可作论文「迭代过程」素材）：legacy「健康档案」（自由文本）于 V50 整体下线，由患者档案结构化扩展（V47）+ 慢病随访指标（V49）承接；早期磁盘文件存储模块于 V54 删除，统一收敛到数据库照片表。两次「做了再删」的决策依据都是结构化数据可检索、可预警、可被 AI 摄取，优于自由文本/文件堆。

### 3.3 REST API 清单

所有接口位于 `/api/v1/*`，标准模块遵循统一动词约定（以医生模块为例，其余同构）：

| 方法 | 路径 | 权限码 | 说明 |
|---|---|---|---|
| GET | `/api/v1/doctors` | `doctors:read` | 列表（keyword/工号/姓名/性别过滤 + 分页） |
| GET | `/api/v1/doctors/{id}` | `doctors:read` | 详情 |
| POST | `/api/v1/doctors` | `doctors:write` | 创建（同时建 app_user 账号） |
| PUT | `/api/v1/doctors/{id}` | `doctors:write` | 更新档案 |
| PATCH | `/api/v1/doctors/{id}/status` | `doctors:status` | 启用/停用（落 app_user.enabled） |
| PATCH | `/api/v1/doctors/{id}/reset-password` | `doctors:reset-password` | 管理员重置密码 |
| DELETE | `/api/v1/doctors/{id}` | `doctors:delete` | 删除（级联清账号） |

同构模块：patients、family-doctors、receptions（均含 status/reset-password）、departments、medications、visits、medical-records、followups、family-doctor-contracts、dictionaries、configs。

特殊接口：

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/v1/auth/register` / `/login` | 公开 | 登录返回 `{accessToken, tokenType, expiresIn}` |
| GET | `/api/v1/auth/me` | 登录态 | 当前用户 + 角色 + 权限码集合（前端 RBAC 渲染依据） |
| GET | `/api/v1/rbac/permissions` / `/roles` | `rbac:read` | 权限全集 / 角色-权限矩阵 |
| PUT | `/api/v1/rbac/roles/{roleId}/permissions` | `rbac:write` | 整体替换某角色权限（ADMIN 角色拒改，防自锁） |
| PATCH | `/api/v1/medications/{id}/inventory` | `medications:inventory` | 库存差量调整 `{delta, reason}`，行锁 + 流水 |
| GET | `/api/v1/dashboard/summary` | 登录态 | 按权限裁剪的首页运营指标 |
| POST | `/api/v1/photos`（multipart） / GET `/api/v1/photos/{uuid}/content` | 登录态 | 照片上传 / 读取 |
| GET | `/api/v1/common/dictionaries` | 登录态 | 表单字典联动取启用项 |
| POST | `/api/v1/ai/vision/parse-medical-record` | `ai:vision` | 病历照片 OCR |
| POST | `/api/v1/ai/patient/{id}/ask` / `/consent` | `ai:patient-rag` | 私域问询 / 患者授权 |
| POST/GET/PATCH/DELETE | `/api/v1/ai/consult/sessions*` | `ai:consult` | 会话管理；`POST …/{id}/messages` 为 SSE |
| POST | `/api/v1/ai/admin/backfill` | `ai:admin` | 历史数据向量回填 |
| GET | `/api/v1/health`、`/actuator/health` | 公开 | 健康检查 |

### 3.4 权限码体系

权限码采用 `resource:action` 二段式，按模块成族：

- 通用动作族：`<module>:read / write / delete / status`（status = 启停或状态机流转）
- 账号类附加：`doctors|family-doctors|patients|receptions:reset-password`
- 专项动作：`medications:inventory`（库存调整）、`rbac:read|write`（权限矩阵管理）
- AI 族：`ai:vision`、`ai:patient-rag`、`ai:consult`、`ai:admin`

前后端**双重校验**：后端 `@PreAuthorize` 注解为准绳；前端以 `/me` 返回的权限码集合控制菜单、按钮、列的渲染（`lib/permissions.ts`），无权限的入口直接不可见。

### 3.5 跨切面机制

- **`SecurityConfig`**：自定义 `JwtAuthenticationFilter`；`/api/v1/*` 默认要求 Bearer Token；`/api/v1/auth/**`、`/api/v1/health`、`/error` 放行（放行 `/error` 是必要细节：否则 Boot 的 ERROR dispatch 被拦截，所有未知路由都被翻译成 401，前端会误判 Token 失效强制登出）。
- **`LoginAttemptService`（登录防爆破）**：按「用户名+来源 IP」组合键内存计数，连续失败 5 次锁定 15 分钟，锁定期间即使密码正确也返回 **429 + `Retry-After` 头**，登录成功即清零；距上次失败超过锁定时长自动开新窗口；阈值经 `security.login-throttle.max-attempts / lock-duration` 配置；反向代理场景取 `X-Forwarded-For` 首跳为来源 IP。内存实现单实例有效，多实例扩展时需替换为 Redis 等共享存储（已在部署文档注明）。
- **`ApiExceptionHandler`（@RestControllerAdvice）统一错误语义**：

| 异常 | HTTP | 语义 |
|---|---|---|
| `NotFoundException` | 404 | 资源不存在 |
| `MethodArgumentNotValidException` | 400 | 参数校验失败 |
| `IllegalArgumentException` | 409 | 业务冲突（重复编号、库存不足等） |
| `AiConsentRequiredException` | 412 | 患者未授权 AI（携带 `code: AI_CONSENT_REQUIRED`） |
| `TooManyLoginAttemptsException` | 429 + Retry-After | 登录限流 |
| `AiRateLimitException` | 429 | AI 配额超限 |
| 兜底 `Exception` | 500 | 统一 JSON `{message}`，不泄漏堆栈；安全异常与 `ResponseStatusException` 按原语义放行 |

- **`TraceIdFilter`**：每请求生成/透传 `X-Trace-Id`，写 MDC，AI 调用链一并携带。
- **`ApiMetricsFilter` + `AiMetricsRecorder`**：暴露 `hospital_api_*` 与 `hospital_ai_*` Micrometer 指标。
- **`AuditService`**：字典等敏感写操作落操作日志。

### 3.6 持久化与查询策略

- **读写分径**：写路径走 JPA（实体校验、级联、乐观语义清晰）；复杂读路径走 JdbcClient 原生 SQL（多表 JOIN 反规范化患者/医生信息、词典翻译、分页）。混用要点：JPA setter 更新后若以 JdbcClient 重读作为响应，必须 `saveAndFlush` 先刷脏数据，否则返回旧值——该缺陷由集成测试首跑在三个模块同时发现并全量修复，是「读写分径」模式的代表性陷阱。
- **数据库分页**：就诊/病历/随访等增长型列表采用「共享 WHERE 的 COUNT + `LIMIT :limit OFFSET :offset`」双查询，或 JPA `Specification + PageRequest`（病历模块），统一返回 `PageResponse{records,total,page,size}`；字典等小表保留内存分页——按数据量权衡而非一刀切。
- **PostgreSQL 可空参数**：JdbcClient 下可空过滤参数须写 `CAST(:param AS TEXT) IS NULL`，否则类型推断报错。
- **库存并发安全**：药品库存调整 `SELECT … FOR UPDATE` 行锁 + 差量更新 + 流水表；处方保存在同一事务内逐项扣减，任一不足整体 409 回滚。
- **自动编号**：药品 `YP+yyyyMMddHHmmss+3位`、就诊 `JZ+…`、病历 `BL+…`，服务端生成并查重重试。

### 3.7 数据库迁移规范（Flyway V1–V54）

- 路径 `src/main/resources/db/migration/`，命名 `V{n}__{description}.sql`；**历史脚本只增不改**，回滚以新版本前滚表达。
- **三层规范**：L1 基础实体（app_user/app_role）→ L2 RBAC 结构（权限表）→ L3 数据补丁/命名对齐，层间有先后依赖。
- **两阶段重命名**：先 JPA `@Column/@Table` 映射旧名上线，再以独立新版本脚本重命名，两步绝不合并。
- 版本里程碑：

| 版本段 | 内容 |
|---|---|
| V1–V14 | 初始 schema、角色/权限种子、字典、RBAC 结构与命名对齐 |
| V15–V24 | 药品/医生/就诊/配置/病历/健康档案/患者/前台各表创建 |
| V25–V36 | 实体重建与英文化（doctor/visit/family_doctor 重建、表重命名、照片表、删除权限补齐） |
| V37–V45 | AI 模块：权限、审计表、抽取历史、pgvector → Qdrant 迁移（V45 删遗留向量表） |
| V46–V49 | 字典码对齐、患者档案医疗字段扩展、家医签约、慢病随访 |
| V50 | 健康档案模块整体下线（被 V47+V49 结构化承接） |
| V51 | 业务链路打通：医生↔科室、就诊↔医生、病历↔就诊 |
| V52 | 前台（RECEPTION）角色工作权限 |
| V53 | `rbac:read/write`（角色权限管理页） |
| V54 | 磁盘文件模块下线，删 `file_metadata` |

---

## 4. 前端架构

### 4.1 技术栈

| 类别 | 技术 |
|---|---|
| 框架 | Next.js 15.3（App Router）+ React 19 |
| 语言 | TypeScript 5.8（strict） |
| 样式 | Tailwind CSS 3.4 + PostCSS |
| 状态/数据 | 原生 `fetch` + React Hooks（刻意未引入状态库，降低复杂度） |
| 鉴权 | localStorage 持久 Token + 路由守卫 + 401 统一登出 |
| 测试 | Vitest（纯逻辑单测，`npm run test`） |
| 包管理 | pnpm（lockfile 入库，支撑 CI 与 Docker `--frozen-lockfile` 可重现构建） |

### 4.2 目录结构与页面清单

```
web/
├── app/                       # App Router 页面（17 个）
│   ├── login/  register/      # 登录、注册
│   ├── dashboard/             # 首页：按权限裁剪的真实运营指标
│   ├── patients/              # 患者管理 + [id] 360° 视图
│   ├── doctors/  family-doctors/  receptions/   # 三类账号管理
│   ├── departments/  medications/               # 科室、药品（含库存行操作）
│   ├── visits/  medical-records/                # 挂号就诊、病历（跨页预填）
│   ├── family-doctor-contracts/  followups/     # 家医签约、慢病随访
│   ├── dictionaries/  configs/                  # 数据字典、系统配置
│   ├── roles/                 # 角色-权限矩阵管理（仅 ADMIN）
│   └── ai-consult/            # 社区 AI 问诊（SSE 流式）
├── components/
│   ├── ui/                    # 通用原语：button、input、card、data-table、file-upload
│   ├── layout/                # auth-layout、app-shell、sidebar（按权限过滤菜单项）
│   └── business/
│       ├── entity-management-page.tsx   # CRUD 通用页（状态与编排）
│       ├── entity-page/                 # 拆分子模块：types（类型与表单工具）/
│       │                                #   modal / detail-value / entity-form-fields
│       ├── forms/entity-form-configs.ts # 各模块声明式页面配置
│       ├── patient-ai-ask-panel.tsx     # 患者私域 AI 问询面板（引用可点击溯源）
│       ├── ai-suggestion-panel.tsx      # 病历 OCR 结果面板（一键回填）
│       └── ai-consent-modal.tsx         # AI 授权弹窗（412 触发）
└── lib/
    ├── api.ts                 # HTTP 客户端 + SSE + TraceId 注入 + 401 处理
    ├── api-contract.ts        # 端点常量 + DTO 类型（前后端契约单一来源）
    ├── auth.ts  permissions.ts  token-storage.ts
```

### 4.3 关键抽象：EntityManagementPage（配置驱动 CRUD）

`entity-management-page.tsx` 及其 `entity-page/` 子模块构成一套配置驱动的 CRUD 通用页。患者、医生、家庭医生、前台、科室、药品、就诊、病历、签约、随访、字典、配置等十余个模块均为其薄包装层，仅需声明一个 `EntityPageConfig`：

| 配置项 | 能力 |
|---|---|
| `columns` | 列定义，类型 `text/photo/currency/badge`，`dictCode` 自动词典翻译，自定义 `render` |
| `formFields` | 表单字段，类型 `text/password/textarea/number/select/dict-select/photo/datetime/date/custom`；`dict-select` 选项实时来自数据字典启用项 |
| `searchFields` | 顶部搜索栏（文本/下拉/字典联动），值作为 query 参数传给列表接口 |
| `rowActions` | 行级操作（重置密码、调库存、写病历…），带 `permission` 门槛与确认/输入弹窗 helper |
| `statusField` | `enabled`（启停）或 `status`（状态机）两种状态语义 |
| `labelMap` | 详情弹窗字段中文名与展示顺序 |
| `createPayload/updatePayload` | 表单 → 请求体的映射（含类型转换） |
| `autoOpenCreate` | URL 直达「预填新建」（就诊列表「写病历」→ 病历页自动弹出预填表单） |

权限驱动渲染贯穿始终：列、按钮、行操作均与当前用户权限码比对后再呈现。

**论文价值**：新增一个管理页面的边际成本从「整页开发」降为「一份配置对象」，且分页、权限、字典翻译、错误提示等横切关注点天然一致；该组件后期按单一职责拆分为 `entity-page/` 子模块（类型与表单工具 / 弹窗 / 详情值渲染 / 共用表单字段渲染器），公共类型经原路径 re-export 保持十余个调用方零改动——可作为「大组件治理」的实例。

### 4.4 类型契约与 API 客户端

- `lib/api-contract.ts` 是前后端类型/路由的**单一来源**：`API_ROUTES` 端点常量 + 各模块 Request/Response 类型，与后端 DTO 一一对应；字段变更先改契约，由 TypeScript 编译器把影响传导到所有引用点。
- `lib/api.ts`：统一注入 TraceId；统一解析后端错误 JSON 为人类可读消息；401 时清 Token 并重定向登录（登录/注册接口除外）；对 412 AI 授权码抛出专用异常类型供页面弹授权框；SSE 解析问诊流式增量。

---

## 5. 主要功能模块

### 5.1 认证、授权与权限管理

- **注册/登录**：登录成功返回 JWT（HS256，TTL 8h）；BCrypt 密码哈希。
- **登录防爆破**：见 §3.5，5 次/15 分钟锁定，429 + Retry-After。
- **RBAC 三件套**：`app_role` × `app_permission`（约 60 个细粒度权限码）× `app_role_permission` 多对多；用户—角色亦为多对多。
- **可视化权限管理页 `/roles`**（V53）：ADMIN 以矩阵勾选方式调整每个角色的权限；**ADMIN 角色权限固定不可改（防自锁）**；未知权限码、整体替换语义均有 409 校验；权限每请求从数据库实时加载，调整即时生效无需重新登录。
- **统一账号服务 `UserAccountService`**：医生/家庭医生/患者/前台共用「`app_user`（账号）+ profile 表（档案）」模式，统一提供 createUser / setEnabled / resetPassword / deleteUser；账号停用即时阻断登录。

### 5.2 业务链路串联（V51/V52）

重构不止于单模块 CRUD，核心诊疗链路已打通：

1. **医生 ↔ 科室**：医生档案归属科室；挂号界面选科室后联动过滤该科室医生；
2. **就诊 ↔ 医生**：挂号记录落医生，列表反规范化展示医生姓名；
3. **病历 ↔ 就诊**：就诊列表「写病历」一键跳转病历页并自动弹出预填表单（患者、就诊号已带入）；
4. **处方 ↔ 库存**：病历保存时按处方明细在同一事务内自动扣减药品库存并写流水；编辑病历按**差量**调整；任一药品库存不足整体 409 回滚；删除病历自动返还库存；
5. **前台工作台**：RECEPTION 角色拥有挂号/患者读写 + 医生/科室/签约/随访只读（V52），覆盖前台完整作业面；
6. **首页 Dashboard**：`/api/v1/dashboard/summary` 返回按当前用户权限裁剪的实时指标（患者数、今日就诊、病历数、库存预警等），无权限的卡片不返回也不渲染。

### 5.3 患者管理与 360° 视图

- 患者档案：账号信息 + 姓名/头像/性别（词典）/手机/身份证/邮箱 + **出生日期、住址、过敏史、既往病史、紧急联系人**（V47）+ AI 授权时间戳。
- `/patients/[id]` 360° 视图五区聚合：基本信息（过敏史以醒目警示条突出）｜家医签约状态卡｜健康指标趋势 sparkline｜就诊/病历时间线（深链接回原始记录）｜AI 智能问询面板。各业务列表中的患者姓名均可点击直达。

### 5.4 慢病随访与家医签约

- **慢病随访** `patient_followup`（V49）：结构化记录收缩压/舒张压、血糖、身高、体重、心率与随访备注；**BMI 由服务端按身高体重计算**（前端不可篡改）；360° 视图内置趋势折线与超标预警（血压 ≥140/90、空腹血糖 ≥7.0、BMI ≥28 标红）；列表数据库分页。
- **家医签约** `family_doctor_contract`（V48）：患者 ↔ 家庭医生服务契约（服务包、起止日期）；**数据库部分唯一索引**保证一名患者至多一份 ACTIVE 签约（应用层重复建约返回 409）；到期自动转 EXPIRED（查询期换算 + 建约时惰性落库）；TERMINATED 后可重新签约。

### 5.5 药品与库存

- 台账字段：自动编号、名称、价格（NUMERIC）、库存、主要药效、副作用、详情，启停状态。
- 库存调整接口（`medications:inventory` 权限）：`{delta, reason}` 差量语义，行锁防并发超卖，`stock_after` 随流水落库可追溯；与处方扣减共用同一套并发与回滚机制。

### 5.6 就诊与病历

- 就诊：自动就诊号、费用、科室（词典）、就诊医生、挂号备注/详情；创建时先校验患者存在（不存在返回 404 而非数据库外键 500）；多条件检索（就诊号/科室/患者名/patientId/doctorId）+ 数据库分页。
- 病历：主诉、体征、诊断等临床字段 + `prescription_items`（JSONB 处方数组）+ `attachments`（JSONB 附件）+ `ai_extracted`（OCR 原始结果留痕）；状态机 DRAFT→ACTIVE→ARCHIVED；保存触发库存扣减与（已授权患者的）异步向量摄取。

### 5.7 科室 / 字典 / 系统配置 / 照片

- 科室：名称唯一，删除前校验就诊引用，避免悬挂数据。
- 数据字典：type/code/label 维护 + 启停 + 写操作审计日志；业务表以 INTEGER 引用词典码（如性别、科室类型），前端列与详情自动翻译为中文标签。
- 系统配置：key-value + `/effective` 生效项查询。
- 照片：multipart 上传入库（BYTEA），UUID 主键防顺序遍历，统一供头像/附件复用。

---

## 6. AI 智能化子系统

### 6.1 病历 AI 识别（Vision OCR）

- 接口：`POST /api/v1/ai/vision/parse-medical-record`（`ai:vision`）。
- 流程：上传病历照片（base64）→ `AiCallTemplate.chat` 调用多模态模型 `doubao-seed-2.0-lite`（强制 `temperature=1`，火山方舟约束）→ 按 JSON Schema 抽取主诉/现病史/体征/诊断/处方建议 → 结果写 `ai_extraction_history` 留痕 → 前端 `ai-suggestion-panel` 呈现并支持**一键回填病历表单**（医生可改后再存，AI 仅辅助不直写）。
- 解析失败入 `ai_dead_letter` 死信表，供审计与人工重放。

### 6.2 患者私域 RAG（一对一问询）

- 接口：`POST /api/v1/ai/patient/{patientId}/ask`（`ai:patient-rag`）；患者未授权（`ai_consent_at IS NULL`）时返回 **412 + `AI_CONSENT_REQUIRED`**，前端弹 `AiConsentModal` 引导显式授权（`POST …/consent`）。
- 数据管道：病历/就诊记录创建或更新 → 事件驱动异步分块 → `doubao-embedding-vision-250615` 生成 **2048 维**向量 → 写入 Qdrant collection `patient_knowledge`（gRPC，启动自动建库，Cosine 距离）；失败入死信；`POST /api/v1/ai/admin/backfill`（`ai:admin`）支持历史数据一键回填。
- **强制隐私围栏（本系统安全设计核心点）**：`PatientKnowledgeStore.search()` 在实现内部**无条件**追加 `match(patient_id, ?)` MUST 过滤，且类中**不存在任何不带该过滤的重载**——查询 A 患者时从向量检索层面就不可能命中 B 患者的数据，隔离不依赖调用方自觉。
- 回答质量：检索片段连同患者基础信息组装提示词；LLM 回答以 `[#N]` 内联标注引用，前端解析为可点击按钮，点击侧滑展示原文片段并深链接跳回原始病历/就诊记录——医生可逐条核实 AI 依据。

### 6.3 社区 AI 问诊（多轮 SSE 聊天）

- 会话生命周期：`POST/GET/PATCH/DELETE /api/v1/ai/consult/sessions*`（`ai:consult`）；发消息 `POST …/{id}/messages` 为 **SSE 流式**（`text/event-stream`）。
- 流式实现：`AiCallTemplate.chatStream()` 以 WebClient 透传 OpenAI 协议 `data:` 增量行，前端逐 token 渲染。
- 安全围栏：命中 `guardrail/refused_keywords.txt` 危急/违规关键词（自杀、处方药滥用等）触发兜底拒答并提示就医；系统提示词明确「AI 不能替代执业医师诊断」。
- 全部会话与消息持久化（`ai_consult_session` / `ai_consult_message`），可回看可审计。

### 6.4 AI 统一基础设施

- **`AiCallTemplate`**：所有上游调用唯一入口（unary + 流式），统一强制 `temperature=1`、超时 60s、重试、异常归一为 `AiException`。
- **审计**：unary 经 `AiAuditInterceptor`、流式在模板内部，统一落 `ai_audit_log`（feature/model/输入输出 tokens/延迟/结果/trace_id）。
- **限流 `AiRateLimiter`**（Bucket4j）：每用户 10 QPM + 全局日 token 预算 100,000，超限 429。
- **可降级**：`hospital.ai.enabled=false` 即跳过整个 AI Bean 装配，无 API Key 环境（含生产默认）系统其余功能完全正常——AI 是增强而非依赖。
- **密钥管理**：API Key 仅经环境变量 / gitignored 的 `application-local.yml` 注入，仓库零密钥。

---

## 7. 工程化：测试、CI/CD 与可观测性

### 7.1 测试体系

| 层 | 工具 | 规模 | 覆盖 |
|---|---|---|---|
| 后端集成测试 | JUnit 5 + Testcontainers（pgvector/pg16 真实容器） | 16 个 `*IntegrationTests` 类，合计 **154** 个测试 | 每类独立拉起 PostgreSQL，完整验证 Flyway 全量迁移 + HTTP 链路 + Spring Security 鉴权（401/403）+ 业务规则（重复签约 409、库存不足回滚、登录限流 429、就诊创建患者校验 404、数据库分页 total 正确性等） |
| 后端单元测试 | JUnit 5（无容器） | 含于上（AI 算法 13 类 + JWT + 限流时钟） | OCR 抽取、问诊守护、上下文组装、嵌入分块、限流（可拨动 Clock 验证锁定/过期/清零）、流式模板 |
| 前端单元测试 | Vitest | **18** 个 | 权限判断真值表、Token 存取（含 SSR 无 window 分支）、表单工具（ISO↔本地时区往返、datetime/date/custom 字段初始化） |
| 静态检查 | ESLint + TypeScript strict | 全量 | CI 阻断 |

测试的实证价值（可作论文论据）：集成测试**首次全量运行即发现 3 个真实缺陷**——①三个模块更新接口因 JPA 脏数据未 flush 返回旧值；②就诊创建未校验患者存在导致外键异常 500；③异常兜底处理误吞框架自带状态码——均在上线前修复，直接论证了「真实数据库集成测试」相对 Mock 测试的价值。

### 7.2 持续集成与交付

- **CI**（`.github/workflows/ci.yml`，push main / PR 触发，双 job 并行）：
  - `backend`：`mvn test` 全量 154 测试（GitHub Runner 自带 Docker，Testcontainers 开箱即用），Maven 依赖缓存，全程约 2 分钟；
  - `web`：pnpm 冻结安装 → ESLint → Vitest → `next build` 生产构建。
- **交付**：多阶段 Docker 镜像（依赖层缓存，源码改动秒级重建）；`docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build` 一键部署/升级；Flyway 启动自动迁移，无手工 SQL。

### 7.3 可观测性

- **TraceId**：`X-Trace-Id` 从前端注入贯穿后端日志（MDC）与 AI 调用链，单请求全链路可追。
- **指标**（Micrometer）：`hospital_api_requests_total / latency_seconds`；`hospital_ai_calls_total{feature,model,outcome}`、`hospital_ai_latency_seconds`、`hospital_ai_tokens_total{direction}`。
- **健康检查**：`/actuator/health`（生产 profile 隐藏细节）。
- **审计三表**：字典操作日志、AI 调用审计、药品库存流水；另有 AI 死信表支撑失败重放。

### 7.4 安全设计汇总

| 层面 | 机制 |
|---|---|
| 认证 | JWT（HS256，TTL 8h）+ BCrypt 哈希；账号停用即时生效 |
| 防爆破 | 登录失败限流：用户名+IP 连错 5 次锁 15 分钟（429 + Retry-After），成功清零 |
| 授权 | ~60 个 `resource:action` 权限码；后端 `@PreAuthorize` 准绳 + 前端按权限渲染的双重校验 |
| 权限治理 | 可视化矩阵管理、ADMIN 防自锁、权限实时加载即时生效 |
| 数据隔离 | 患者向量检索强制 `patient_id` MUST 过滤，无旁路 API（隔离不依赖调用方） |
| 隐私门控 | 患者 AI 显式授权（412 门控 + 授权时间戳留痕） |
| 机密管理 | 全部经环境变量注入；生产 profile 无默认值 + compose `${VAR:?}` 双层 fail-fast；`.gitignore` 覆盖密钥文件全部形态，仓库零密钥 |
| 输入与错误 | Jakarta Validation 参数校验；兜底 500 统一 JSON 不泄漏堆栈 |
| 资源防护 | AI 每用户 QPM + 日 token 预算；照片 UUID 主键防遍历；数据库不暴露宿主端口 |
| 容器加固 | 生产镜像非 root 用户运行；多阶段构建运行层不含编译工具链 |

---

## 8. 与 Legacy 系统的对比

| 维度 | Legacy（旧） | Rewrite（新） |
|---|---|---|
| 后端 | Spring Boot 2 + MyBatis + MySQL | Spring Boot 3.3 + Spring Data JPA/JdbcClient + PostgreSQL 16 |
| 前端 | Vue 2 + ElementUI | Next.js 15 + React 19 + Tailwind + TypeScript strict |
| 代码命名 | 拼音（yisheng/jiuzhen/bingli…） | 全英文领域命名，模块结构统一 |
| 鉴权 | Session + 自定义拦截器 | JWT + Spring Security + ~60 细粒度权限码 + 登录限流 + 可视化权限管理 |
| 账号体系 | 各角色表独立 username/password | 统一 `app_user` + profile + 多对多角色 |
| 业务链路 | 模块孤立 | 科室→医生→挂号→病历→处方自动扣库存全链路 |
| 健康档案 | 自由文本表 | 结构化拆解：档案医疗字段 + 随访指标 + BMI 计算 + 趋势预警 |
| 数据库变更 | 手工 SQL | Flyway V1–V54 版本化、三层规范、两阶段重命名 |
| 分页 | 全量加载 | 增长型表数据库分页（LIMIT/OFFSET + COUNT） |
| 错误处理 | 控制器零散返回 | `@ControllerAdvice` 集中映射 + 兜底 500 |
| 测试 | 几乎为零 | Testcontainers 154 集成测试 + 18 前端单测，CI 阻断 |
| CI/CD | 无 | GitHub Actions 双流水线（约 2 分钟）+ 多阶段镜像 + 一键 compose 部署 |
| 可观测性 | 仅日志 | TraceId 贯穿 + Micrometer 指标 + 审计三表 |
| AI 能力 | 无 | Vision OCR / 患者私域 RAG / SSE 流式问诊 + 审计限流死信治理 |
| 向量检索 | 无 | Qdrant + 强制租户隔离 + 显式授权门控 |

---

## 9. 主要功能点清单（一句话总览，可直接做答辩 PPT 列点）

1. **统一账户与 RBAC**：JWT + BCrypt，五类角色共用 `app_user+profile`，约 60 个 `resource:action` 权限码前后端双重校验。
2. **可视化权限管理**：`/roles` 矩阵勾选即时生效，ADMIN 防自锁。
3. **登录安全**：失败限流（用户名+IP，5 次/15 分钟，429 + Retry-After）防口令爆破。
4. **患者 360° 视图**：档案 + 过敏史警示 + 签约状态 + 健康趋势 + 就诊/病历时间线 + AI 问询一页聚合。
5. **慢病随访**：结构化体征指标，BMI 服务端计算，趋势折线 + 三项超标预警。
6. **家医签约**：数据库部分唯一索引保证「一患者一份生效签约」，到期自动失效。
7. **全链路诊疗**：科室联动选医生 → 挂号 → 一键写病历（预填直达）→ 处方保存自动扣库存（差量、不足回滚、删除返还）。
8. **药品库存**：行锁防并发 + 全量流水审计 + 专项 `medications:inventory` 权限。
9. **就诊/病历**：单号自动生成、JSONB 处方与附件、状态机、数据库分页与多条件检索。
10. **数据字典/系统配置**：可视化维护 + 写操作审计，前端表单字典联动（`dict-select`）。
11. **AI 病历识别**：视觉大模型结构化抽取，一键回填表单，医生确认后生效，失败入死信。
12. **患者私域 RAG**：向量层强制患者隔离（无旁路 API）+ 显式授权门控（412），回答带可点击溯源引用。
13. **社区 AI 问诊**：多轮会话 SSE 流式渲染，关键词守护兜底拒答，全量对话留痕。
14. **AI 治理**：统一调用模板（强制 temperature=1）、审计落库、每用户 QPM + 日 token 预算、整体可降级关闭。
15. **可观测性**：TraceId 全链路贯穿、Micrometer API/AI 指标、健康检查、审计三表。
16. **数据库工程化**：Flyway V1–V54 只增不改、三层规范、两阶段重命名。
17. **测试体系**：154 后端集成测试（真实 PG 容器）+ 18 前端单测；首跑即捕获 3 个真实缺陷。
18. **CI/CD 与部署**：GitHub Actions 约 2 分钟全绿；多阶段镜像 + 单机 compose 一键部署；机密双层 fail-fast。
19. **配置驱动前端**：EntityManagementPage 通用页，新增管理页≈一份配置对象，横切关注点全局一致。
20. **前后端类型契约**：`api-contract.ts` 单一来源，字段变更由编译器全量传导。

---

## 10. 论文写作建议章节映射

| 论文章节 | 推荐取材小节 |
|---|---|
| 绪论 / 研究背景 | §1.1、§8（Legacy 对比） |
| 需求分析 | §1.3（角色矩阵）、§5（功能模块）、§9（功能清单） |
| 系统总体设计 | §2（架构分层 / 请求生命周期 / 部署形态）、§3.1 / §4.1（技术选型） |
| 数据库设计 | §3.2（表与约束）、§3.7（Flyway 规范与版本里程碑） |
| 详细设计与实现 | §3.3–§3.6（接口/权限/跨切面/持久化策略）、§4.2–§4.4（前端抽象与契约） |
| 关键技术 | §3.5（限流与异常语义）、§5.2（链路串联与事务）、§6（AI 三大能力 + 隐私围栏） |
| 系统测试 | §7.1（测试体系 + 真实缺陷案例）、§7.2（CI） |
| 安全设计 | §7.4（安全机制汇总表） |
| 部署与运行 | §2.3、§7.2 |
| 总结与展望 | §8、§9；展望可写：登录限流多实例化（Redis）、刷新令牌机制、AI 能力扩展（随访建议生成、用药冲突检查）、多院区数据隔离 |
