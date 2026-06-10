# 社区医院管理系统 — 架构与功能说明（论文素材）

> 本文档汇总系统的整体架构、前后端技术栈、模块划分、关键实现机制与主要功能点，可直接用作毕业论文/学位论文的「系统设计与实现」「关键技术」「功能介绍」等章节的素材。

---

## 1. 项目概述

本系统是一套面向社区医院（基层医疗机构）的综合管理平台，将原有的 Spring Boot 2 + Vue 2 + MySQL 单体系统全栈重构为 **Spring Boot 3 + Next.js 15 + PostgreSQL** 的现代化架构，并在重构基础上引入了基于大语言模型的智能化能力（病历 OCR 识别、患者私域 RAG、社区 AI 问诊）。

- 后端：`server/backend-rewrite/`（唯一活跃后端，Java 21 + Spring Boot 3.3）
- 前端：`web/`（Next.js 15 App Router + React 19 + TypeScript 5）
- 数据库：PostgreSQL 16（业务数据） + Qdrant（向量数据） + pgvector（仅扩展保留）
- AI 服务：火山引擎方舟 Doubao（OpenAI 兼容协议）

系统服务于患者、医生、家庭医生、前台、管理员五类角色，覆盖问诊、就诊、病历、患者档案与慢病随访、家医签约、药品、字典、AI 辅助等全流程业务。

---

## 2. 总体架构

### 2.1 架构分层

```
┌─────────────────────────────────────────────────────────────┐
│  Web 前端（Next.js 15 App Router + React 19 + Tailwind）       │
│  - Pages（app/）  - 业务组件（components/business）            │
│  - UI 原语（components/ui）  - API 客户端（lib/api.ts）        │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTPS / JSON / SSE
┌──────────────────────────▼──────────────────────────────────┐
│  Spring Boot 3 后端                                          │
│  ├─ 接入层：SecurityConfig + JwtAuthenticationFilter         │
│  │            TraceIdFilter + ApiMetricsFilter               │
│  ├─ Controller 层（@RestController, /api/v1/*）              │
│  ├─ Service 层（DefaultXxxService）                          │
│  ├─ Repository 层（JPA Repository）                          │
│  ├─ 跨切面：ApiExceptionHandler / AuditService                │
│  └─ AI 子系统：AiCallTemplate + Vision/RAG/Consult           │
└──────────┬───────────────────────┬──────────────────────────┘
           │                       │
   ┌───────▼─────────┐     ┌───────▼─────────┐     ┌──────────────┐
   │  PostgreSQL 16   │     │  Qdrant 向量库   │     │ Doubao（火山）│
   │  - 业务表 / RBAC │     │  - patient_     │     │ - 文本/视觉   │
   │  - 审计 / 字典    │     │    knowledge    │     │ - Embedding  │
   └─────────────────┘     └─────────────────┘     └──────────────┘
```

### 2.2 部署形态

| 组件 | 端口 | 容器 |
|---|---|---|
| PostgreSQL | 5432 | `shequyiyuan-postgres-1` |
| Qdrant（gRPC/REST） | 6334 / 6333 | `qdrant/qdrant:v1.12.4` |
| Spring Boot | 8089 | `mvn spring-boot:run` |
| Next.js | 3000 | `npm run dev` |
| 火山方舟 Doubao | 远程 | HTTPS：`ark.cn-beijing.volces.com` |

---

## 3. 后端架构

### 3.1 技术栈

| 类别 | 技术 | 用途 |
|---|---|---|
| 运行时 | Java 21 + Spring Boot 3.3.5 | Web + IoC 容器 |
| Web | Spring MVC + Spring WebFlux | REST + SSE 流式响应 |
| 安全 | Spring Security + JJWT 0.12 | JWT 鉴权 |
| 持久化 | Spring Data JPA（Hibernate） | 业务表 CRUD |
| 数据库 | PostgreSQL 16 + Flyway 10 | 版本化迁移（V1–V45） |
| 向量库 | Qdrant Java SDK 1.14（gRPC） | 患者私域向量检索 |
| AI 接入 | Spring AI 1.0（仅 BOM 占位） + 自研 `AiCallTemplate` | 直接调用 OpenAI 兼容 API |
| 限流 | Bucket4j 8.10 | 每用户 QPM + 全局 token 预算 |
| 监控 | Spring Actuator + Micrometer | 自定义 AI 指标 |
| 测试 | JUnit 5 + Spring Test + Testcontainers | 集成测试拉真实 PG 容器 |

### 3.2 模块结构

每个业务模块统一目录约定：

```
com.hospital.<module>/
├── controller/   REST 端点（@RestController, 权限注解）
├── service/      Service 接口 + DefaultXxxService 实现
├── repository/   JPA Repository
├── domain/       JPA Entity + 状态枚举
└── dto/          Request / Response DTO
```

业务模块（均已 JPA 化、状态 = Done）：

| 模块 | 包名 | 主要表 |
|---|---|---|
| 认证 | `auth` | `app_user`、`app_role`、`app_user_role`、`app_permission`、`app_role_permission` |
| 患者 | `patient` | `patient_profile` |
| 医生 | `doctor` / `yisheng` | `doctor_profile` |
| 家庭医生 | `familydoctor` / `jiatingyisheng` | `family_doctor_profile` |
| 前台 | `reception` | `reception_profile` |
| 科室 | `department` | `department` |
| 药品 | `medication` / `yaopin` | `medication`、`medication_inventory_log` |
| 就诊 | `visit` / `jiuzhen` | `visit_record` |
| 病历 | `medicalrecord` / `bingli` | `medical_record` |
| 慢病随访 | `followup`（重构新增，承接 legacy 健康档案） | `patient_followup` |
| 家医签约 | `familydoctor.contract`（重构新增） | `family_doctor_contract` |
| 字典 | `dictionary` | `dictionary_item`、`dictionary_operation_log` |
| 系统配置 | `systemconfig` | `system_config` |
| 文件 / 照片 | `file` / `photo` | `file_metadata`、`photo` |
| 审计 | `audit` | 通过 `AuditService` 落到字典审计日志 |
| 可观测性 | `observability` | TraceId + 指标 Filter |
| AI 子系统 | `ai.{client,vision,embedding,ingestion,rag,consult,qdrant,ratelimit,audit,common,admin,config}` | `ai_audit_log`、`ai_extraction_history`、`ai_consult_session`、`ai_consult_message`、`ai_dead_letter` |

### 3.3 跨切面机制

- **`SecurityConfig`**：基于角色的 URL 拦截 + 自定义 `JwtAuthenticationFilter`，所有 `/api/v1/*` 默认要求 Bearer Token；`/api/v1/auth/**`、`/api/v1/health` 开放。
- **`ApiExceptionHandler`（@ControllerAdvice）**：
  - `NotFoundException` → 404
  - `IllegalArgumentException` → 409 Conflict（业务冲突）
  - `MethodArgumentNotValidException` → 400 Bad Request
  - `AiRateLimitException` → 429 Too Many Requests
  - `AiConsentRequiredException` → 412 Precondition Failed
- **`TraceIdFilter`**：为每个请求生成 `X-Trace-Id`，写入 MDC，AI 调用一并透传。
- **`ApiMetricsFilter` + `AiMetricsRecorder`**：暴露 `hospital_api_*`、`hospital_ai_calls_total / latency_seconds / tokens_total` 指标。
- **`AuditService`**：字典等敏感模块写入操作日志。

### 3.4 数据库迁移规范（Flyway V1–V45）

- 脚本路径：`src/main/resources/db/migration/`，文件命名 `V{n}__{description}.sql`。
- **三层规范**（L1 基础实体 / L2 RBAC 结构 / L3 数据补丁），变更必须新增版本，禁止修改历史脚本。
- 命名重构遵循两阶段策略：先用 JPA `@Column` 映射旧名 → 后续新版本脚本重命名。
- V1–V36：基础表与 RBAC；V37–V45：AI 模块（含 pgvector 安装 → 后改 Qdrant，V44 留为空 NOOP，V45 删除遗留向量表）。

---

## 4. 前端架构

### 4.1 技术栈

| 类别 | 技术 |
|---|---|
| 框架 | Next.js 15.3（App Router）+ React 19 |
| 语言 | TypeScript 5.8（strict） |
| 样式 | Tailwind CSS 3.4 + PostCSS |
| 状态/数据 | 原生 `fetch` + React `use*` Hooks（未引入额外状态库） |
| 鉴权 | localStorage 持久 Token + Route Guards |

### 4.2 目录结构

```
web/
├── app/                       # App Router 页面
│   ├── login/  register/      # 登录、注册
│   ├── dashboard/             # 控制台
│   ├── patients/  doctors/    # 业务管理页
│   ├── family-doctors/
│   ├── receptions/            # 前台管理
│   ├── departments/
│   ├── medications/
│   ├── visits/
│   ├── medical-records/
│   ├── health-records/
│   ├── dictionaries/
│   ├── configs/
│   └── ai-consult/            # 社区 AI 问诊
├── components/
│   ├── ui/                    # 通用原语（button, input, data-table, file-upload）
│   ├── layout/                # auth-layout, app-shell, sidebar
│   └── business/
│       ├── entity-management-page.tsx   # CRUD 通用页（核心抽象）
│       ├── forms/entity-form-configs.ts # 各模块的页面配置
│       ├── patient-ai-ask-panel.tsx     # 患者私域 AI 问询面板
│       ├── ai-suggestion-panel.tsx      # 病历 OCR 识别结果面板
│       └── ai-consent-modal.tsx         # AI 授权弹窗
└── lib/
    ├── api.ts                 # HTTP 客户端 + SSE
    ├── api-contract.ts        # 端点 + DTO 类型（单一类型源）
    ├── auth.ts                # 登录态
    ├── permissions.ts         # 前端 RBAC（隐藏无权限菜单/按钮）
    └── token-storage.ts       # Token 持久化
```

### 4.3 关键抽象：EntityManagementPage

`components/business/entity-management-page.tsx` 是一套配置驱动的 CRUD 通用页，多数业务模块（药品、医生、家庭医生、家医签约、慢病随访、患者、前台、科室、就诊、病历）都是它的薄包装层，只需提供一个 `EntityPageConfig` 即可获得：

- 列表 + 服务端分页 + 顶部搜索（`searchFields`）
- 新建 / 编辑 / 删除 / 启用-停用 / 重置密码（`rowActions`）
- 详情弹窗：按 `labelMap` 顺序展示中文字段，自动处理性别词典、时间格式化、头像、附件、处方数组、`enabled` 徽章
- 表单字段类型：`text/password/textarea/number/photo/select`
- 权限驱动渲染：列、按钮、行操作均通过 `permission` 与当前用户权限码比对

### 4.4 类型契约

`lib/api-contract.ts` 是前后端类型/路由的 **单一来源**：

- `API_ROUTES`：集中维护所有端点常量
- 各模块的 `Request` / `Response` 类型
- 与后端 DTO 保持一致；任何字段变更先改本文件再传染到组件

---

## 5. 主要功能模块

### 5.1 认证与授权

- **注册 / 登录**：`/api/v1/auth/register`、`/login`；登录返回 JWT（HS256，TTL 8h）。
- **JWT 解析**：`JwtAuthenticationFilter` 解析 `Authorization: Bearer …`，注入 `Authentication` + 权限集。
- **RBAC 三件套**：
  - `app_role`（ADMIN / DOCTOR / FAMILY_DOCTOR / PATIENT / RECEPTION / USER）
  - `app_permission`（细粒度权限码，如 `patients:write`、`ai:vision`、`medications:inventory`）
  - `app_role_permission`（多对多）
- **统一账号服务 `UserAccountService`**：
  - `createUser(username, password, role)` 为医生 / 患者 / 前台 / 家庭医生统一创建账号
  - `setEnabled` 启用 / 停用
  - `resetPassword` 管理员重置密码（需 `<entity>:reset-password` 权限）

### 5.2 患者管理

- 患者档案：姓名、头像、性别（词典）、手机、身份证、邮箱、AI 授权时间戳 `ai_consent_at`。
- 列表 / 新建 / 编辑 / 启用-停用 / 重置密码。
- 患者详情页 `/patients/[id]`：左侧基础信息，右侧 **AI 智能问询面板**（基于该患者私域知识）。

### 5.3 医生 / 家庭医生 / 前台

- 三类账号均沿用 `app_user + profile` 模式。
- 医生额外字段：工号 `uuid_number`（YS001 …）、身份证号。
- 家庭医生不带工号 / 身份证号。
- 前台：基础联系方式与启用状态。

### 5.4 科室

- 科室名称、简介、负责人、电话、科室类型（词典）。
- 删除时校验是否被就诊记录引用，避免脏数据。

### 5.5 药品管理

- 药品台账：编号、名称、价格、库存、主要药效、副作用、详情。
- 编号自动生成：`YP + yyyyMMddHHmmss + 3位` （后端在创建时生成，前端无需填写）。
- 库存调整：`PATCH /api/v1/medications/{id}/inventory`，参数 `{ delta, reason }`；行锁 `FOR UPDATE` 保证并发安全；负库存抛 409。
- 库存流水落 `medication_inventory_log`。

### 5.6 就诊记录

- 就诊号自动生成：`JZ + ...`。
- 字段：患者、科室（词典）、费用、日期、挂号备注 / 详情。
- 列表反规范化拼接患者姓名、头像、联系方式。

### 5.7 病历

- 病历号自动生成：`BL + ...`。
- 字段：患者、就诊、主诉、体征、诊断、`prescription_items`（JSONB）、`attachments`（JSONB）、`ai_extracted`（OCR 结果 JSONB）。
- **AI Vision OCR 识别**：上传病历照片 → 自动抽取主诉 / 体征 / 诊断 / 处方建议（详见 §6.1）。
- 病历保存时若 patient 已授权 RAG，则触发异步嵌入入库（详见 §6.2）。

### 5.8 患者档案与慢病随访

> Legacy 的「健康档案」（自由文本）在重构中被结构化拆解：身份与病史信息进入患者档案（过敏史、既往病史、紧急联系人等），周期性健康数据进入慢病随访模块。

- **患者档案扩展**：出生日期、住址、过敏史、既往病史、紧急联系人；患者 360° 视图（`/patients/[id]`）聚合展示并以警示条突出过敏史。
- **慢病随访** (`patient_followup`)：结构化记录血压/血糖/身高体重/心率，BMI 服务端计算；360° 视图内置趋势折线与超标预警（血压 ≥140/90、血糖 ≥7、BMI ≥28）。
- **家庭医生签约** (`family_doctor_contract`)：患者 ↔ 家庭医生服务契约（服务包/签约日期/到期），数据库部分唯一索引保证一名患者仅一份生效签约。

### 5.9 字典 / 系统配置

- 数据字典：树形 type/code/label，支持启用-停用，操作落 `dictionary_operation_log`。
- 系统配置：扁平 key-value，供运行时读取。

### 5.10 文件与照片

- `file_metadata` 通用文件元数据（按 `business_type` 关联）。
- `photo` 表存 BYTEA + 元数据，主键为 UUID（便于安全引用，避免遍历）。

---

## 6. AI 智能化子系统

### 6.1 病历 AI 识别（Vision OCR）

- 接口：`POST /api/v1/ai/vision/parse-medical-record`，需 `ai:vision` 权限。
- 模型：`doubao-seed-2.0-lite`（多模态，单张图片 base64 上送）。
- 流程：上传图片 → `AiCallTemplate.chat` 强制 `temperature=1` → JSON Schema 抽取（主诉 / 现病史 / 体征 / 诊断 / 处方建议） → 写入 `ai_extraction_history` → 前端 `ai-suggestion-panel.tsx` 展示并允许「一键填回病历表单」。
- 失败入 `ai_dead_letter` 死信，便于审计与重放。

### 6.2 患者私域 RAG（一对一问询）

- 入口：`POST /api/v1/ai/patient/{patientId}/ask`，需 `ai:patient-rag` 权限；同时要求 `patient_profile.ai_consent_at IS NOT NULL`，否则返回 **412 + AI_CONSENT_REQUIRED**，前端弹 `AiConsentModal` 让患者授权。
- 授权接口：`POST /api/v1/ai/patient/{patientId}/consent`。
- 嵌入模型：`doubao-embedding-vision-250615`（2048 维，agentPlan 白名单）。
- 向量库：**Qdrant**（gRPC 6334），collection `patient_knowledge`，启动自动建库；启用 Cosine 距离。
- **强制隐私围栏**：`PatientKnowledgeStore.search()` 内部强制 `match(patient_id, ?)` MUST 过滤；不提供任何旁路重载，保证 A 患者绝不可能命中 B 患者的向量。
- 入库源：病历 / 就诊记录两类业务字段；改动后异步生成 embedding，失败入 `ai_dead_letter`。
- 回管：`POST /api/v1/ai/admin/backfill` 一键回填历史数据。
- 引用渲染：LLM 回答里以 `[#N]` 内联标注引用；前端 `patient-ai-ask-panel.tsx` 用 `text.split(/(\[#\d+\])/g)` 解析为可点击按钮，点击侧滑显示原文片段 + 跳回原始记录的深链接。

### 6.3 社区 AI 问诊（多轮 SSE 聊天）

- 接口：
  - `POST /api/v1/ai/consult/sessions` 创建会话
  - `GET /sessions` / `PATCH` / `DELETE` 会话管理
  - `POST /sessions/{id}/messages` **SSE 流式响应**
- 模型：`doubao-seed-2.0-lite`。
- 流式实现：`WebClient + JdkClientHttpConnector` 在 `AiCallTemplate.chatStream()` 内执行；将 OpenAI 协议的 `data:` 行透传给浏览器。
- 守护规则：`resources/guardrail/refused_keywords.txt` 中匹配到危急 / 违规关键词时返回兜底拒答。
- 数据：`ai_consult_session`、`ai_consult_message` 持久化每条对话，便于回看。

### 6.4 AI 统一基础设施

- **`AiCallTemplate`**：所有上游调用单点入口，强制 `temperature=1`（火山方舟约束），统一超时 60s、重试、错误转 `AiException`。
- **`AiAuditInterceptor`**：unary 调用统一审计；流式调用在 `AiCallTemplate` 内部审计。落 `ai_audit_log`（feature / model / tokens / 延迟 / 状态 / trace_id）。
- **限流 `AiRateLimiter`**：基于 Bucket4j，每用户 QPM = 10，全局日 token 预算 = 100000，超限抛 429。
- **配置**：`application.yml` + `application-local.yml`（gitignore，仅放 API Key 与 agentPlan 专用 base-url）。

---

## 7. 可观测性、安全与质量保障

### 7.1 可观测性

- **TraceId**：`TraceIdFilter` 注入 `X-Trace-Id`，贯穿日志 + AI 调用。
- **指标**：
  - `hospital_api_requests_total / latency_seconds` 通用 HTTP 指标
  - `hospital_ai_calls_total{feature,model,outcome}` AI 调用计数
  - `hospital_ai_latency_seconds` AI 延迟
  - `hospital_ai_tokens_total{direction}` token 用量
- **健康检查**：`/actuator/health`、`/actuator/info`。
- **审计**：字典写操作 → `dictionary_operation_log`；AI 调用 → `ai_audit_log`。

### 7.2 安全与隐私

- JWT 签名密钥配置在 `security.jwt.secret`（≥ 32 字节，生产环境必须替换占位符）。
- BCrypt 密码哈希（cost = 10）。
- 患者向量检索强制 patient_id 隔离（前述）。
- 患者 AI 授权门控（412）。
- 文件 / 密钥 gitignore：`key/`, `*.key`, `.env*`, `**/HOSPITAL_API_KEY`, `**/application-local.yml`。
- 数据库密码、JWT、AI Key 全部来自环境变量或本地覆盖文件。

### 7.3 测试

- 单元测试：核心算法（`MedicalRecordExtractor`、`ConsultGuardrail`、`ConsultContextBuilder`）。
- 集成测试：所有模块均有 `XxxControllerIntegrationTests`，使用 **Testcontainers** 拉起真实 PostgreSQL 容器，验证 Flyway 迁移 + 完整 HTTP 链路 + Spring Security 鉴权。
- AI 模块单独有 `AiCallTemplateStreamTest`、`AiConsultServiceTest` 等。

---

## 8. 与 Legacy 系统的对比

| 维度 | Legacy（旧） | Rewrite（新） |
|---|---|---|
| 后端 | Spring Boot 2 + MyBatis + MySQL | Spring Boot 3.3 + Spring Data JPA + PostgreSQL 16 |
| 前端 | Vue 2 + ElementUI | Next.js 15 + React 19 + Tailwind + TS |
| 鉴权 | Session + 自定义拦截器 | JWT + Spring Security + 细粒度 RBAC |
| 数据库迁移 | 手工 SQL | Flyway 版本化（V1–V45） |
| 各角色账号 | 各表独立 username/password | 统一 `app_user` + profile + role |
| 错误处理 | 控制器内零散返回 | `@ControllerAdvice` 集中映射 HTTP 语义 |
| 测试 | 极少 | TestContainers 全模块集成测试 |
| 可观测性 | 仅日志 | TraceId + Micrometer 指标 + 审计表 |
| AI 能力 | 无 | Vision OCR / 患者 RAG / SSE 问诊 |
| 向量检索 | 无 | Qdrant + 强制租户隔离 |

---

## 9. 主要功能点清单（一句话总览）

为便于答辩 PPT 列点，下面给出可直接复用的「功能点」一句话清单：

1. **基础账户与权限**：JWT 鉴权 + RBAC 细粒度权限，五类角色统一接入。
2. **患者档案管理**：CRUD + 启用-停用 + 管理员重置密码。
3. **医生 / 家庭医生 / 前台管理**：与患者同体系，工号自动管理。
4. **科室管理**：CRUD + 引用约束（禁止删除已被就诊引用的科室）。
5. **药品管理**：自动编号 + 价格 / 库存维护 + 库存增减流水（行锁防并发）。
6. **就诊管理**：自动就诊号，跨表展示患者与科室信息。
7. **病历管理**：自动病历号 + JSONB 处方与附件 + AI OCR 一键识别。
8. **患者档案与慢病管理**：患者 360° 视图、结构化随访指标（BMI 自动计算 + 超标预警 + 趋势图）、家庭医生签约（一患者一份生效契约的数据库级约束）。
9. **数据字典 / 系统配置**：可视化维护 + 操作审计。
10. **文件 / 照片管理**：通用文件表 + 二进制照片表，按业务 ID 关联。
11. **AI 病历识别**：火山 Doubao 视觉模型自动抽取结构化字段，结果可回填表单。
12. **患者私域 RAG 问询**：医生针对单个患者发起自然语言询问，回答带可点击引用，强隐私围栏 + 授权门控。
13. **社区 AI 问诊**：患者可与 AI 多轮对话，SSE 流式渲染，关键词守护规则兜底拒答。
14. **AI 调用审计与限流**：统一审计落库 + 每用户 QPM + 全局日 token 预算 + 死信队列。
15. **可观测性**：Trace ID 贯穿、Micrometer 指标、Actuator 健康检查。
16. **数据库版本化**：Flyway V1–V45，结构 / 数据 / 命名三层分离规范。
17. **测试体系**：Testcontainers 全模块集成测试，AI 模块单测覆盖关键算法。
18. **现代化前端**：Next.js App Router + 配置驱动的 EntityManagementPage 通用页，组件级 RBAC 渲染。

---

## 10. 论文写作建议章节映射

| 论文章节 | 推荐取材小节 |
|---|---|
| 绪论 / 研究背景 | §1、§8（Legacy 对比） |
| 需求分析 | §5（业务模块） + §9（功能清单） |
| 系统总体设计 | §2（总体架构） + §3.1 / §4.1（技术栈选型） |
| 详细设计与实现 | §3.2 / §3.3（后端模块与跨切面）、§4.2 / §4.3（前端抽象） |
| 关键技术 | §3.4（Flyway 三层规范）、§5.1（RBAC）、§6（AI 三大功能） |
| 系统测试 | §7.3 + §7.1 |
| 安全设计 | §7.2 |
| 总结与展望 | §8 对比表 + §9 功能清单 |
