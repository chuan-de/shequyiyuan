# 迁移状态总表（唯一状态源）

> 本文件是迁移完成度唯一状态源（Single Source of Truth）。
> `docs/route-migration-final-report.md`、`docs/p1-iteration-module-plan.md`、`docs/api-contract-governance.md` 中的完成度信息必须引用本文件，不得重复维护独立口径。

## 维护要求
- 每次合并“迁移相关 PR”时，必须同步更新本文件中的模块状态、证据链接、最后更新时间、负责人。
- 模块状态仅允许以下枚举：`Not Started` / `Partial` / `Done` / `Diverged`。
- 权限/RBAC 相关迁移必须遵循“结构先行、数据后置”的分层规则：结构创建脚本发布后，只能通过新增版本脚本做数据补丁或命名调整，禁止回改已发布版本脚本。

## 数据库迁移分层规则（权限/RBAC）

### L1：基础实体结构
- 定义：用户、角色等基础实体与核心主键/唯一键（例如 `app_role`）。
- 要求：仅做结构创建或约束补充，不混入业务数据清洗逻辑。

### L2：RBAC 结构
- 定义：权限主体结构与关联关系（例如 `app_permission`、`app_role_permission`、`app_legacy_permission_mapping`）。
- 要求：先完成表结构，再执行任何依赖该结构的数据脚本。

### L3：RBAC 数据调整
- 定义：命名对齐、历史映射补丁、冗余数据清理等（例如权限 code 对齐、legacy 映射补种）。
- 要求：
  - 必须在 L2 结构已存在后执行。
  - 必须使用新版本增量脚本发布（如 `Vxx__*.sql`），禁止修改已发布历史版本脚本内容。

## 权限/RBAC 迁移依赖清单

| 脚本 | 分层 | 依赖关系（执行前置） | 说明 |
|---|---|---|---|
| `V8__permissions.sql` | L2（含初始数据种子） | 依赖 `app_role` 已存在 | 创建 `app_permission`、`app_role_permission` 并写入初始权限/角色权限。 |
| `V9__rbac_permissions.sql` | L2（含初始数据种子） | 依赖 `app_role` 已存在 | 创建增强版 RBAC 结构（含 `resource_code`/`action_code` 及 legacy 映射表）并写入初始数据。 |
| `V11__align_module_permission_naming.sql` | L3 | 依赖 `app_permission`、`app_role_permission` 已存在（来自 V8/V9） | 对旧权限命名做对齐迁移，并清理旧命名数据。 |
| `V12__seed_legacy_permission_mapping.sql` | L3 | 依赖 `app_legacy_permission_mapping`、`app_permission`、`app_role` 已存在（来自 V9） | 补种 legacy 权限映射数据。 |

## 状态字段说明
- **状态**：
  - `Not Started`：尚未开始迁移开发。
  - `Partial`：已完成部分能力，但未闭环。
  - `Done`：功能、契约、页面、测试证据齐全并通过验收。
  - `Diverged`：与 legacy 行为存在已确认偏差（可接受或待收敛）。
- **证据链接**：每个模块至少维护以下四类证据：
  - 代码路径
  - 接口契约
  - 页面路径
  - 测试用例标识

## 模块状态

| 模块 | Legacy 名称 | 状态 | 证据链接（代码路径 / 接口契约 / 页面路径 / 测试用例标识） | 最后更新时间 | 负责人 |
|---|---|---|---|---|---|
| 认证（auth） | yonghu | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/auth/controller/AuthController.java`；契约：`docs/openapi-migrated-modules.yaml`（`/api/auth/*`）；页面：`web/app/login/page.tsx`、`web/app/register/page.tsx`；测试：`AuthControllerIntegrationTests`、`JwtServiceTests` | 2026-05-05 | 待指定 |
| 健康检查（health） | jiankang | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/common/HealthController.java`；契约：`docs/openapi-migrated-modules.yaml`（`/api/health`）；页面：`web/app/dashboard/page.tsx`（健康检查消费方）；测试：`HealthControllerRouteCompatibilityTests` | 2026-05-05 | 待指定 |
| 字典（dictionary） | dictionary | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/dictionary/controller/DictionaryController.java`；契约：`docs/openapi-migrated-modules.yaml`（`/api/dictionaries/*`）；页面：`web/app/dictionaries/page.tsx`；测试：`DictionaryControllerIntegrationTests` | 2026-05-05 | 待指定 |
| 药品（medication） | yaopin | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/yaopin/controller/MedicationController.java`；DB：`V15__create_medication_table.sql`；JPA：`MedicationRepository extends JpaRepository`；契约：`docs/openapi-migrated-modules.yaml`（`/api/v1/medications*`）；页面：`web/app/medications/page.tsx`；测试：`MedicationControllerIntegrationTests` | 2026-05-21 | 待指定 |
| 家庭医生（familyDoctor） | jiatingyisheng | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/jiatingyisheng/controller/FamilyDoctorController.java`；DB：`V16__create_family_doctor_contract_table.sql`；JPA：`FamilyDoctorRepository extends JpaRepository`；契约：`docs/openapi-migrated-modules.yaml`（`/api/v1/family-doctors*`）；页面：`web/app/family-doctors/page.tsx`；测试：`FamilyDoctorControllerIntegrationTests` | 2026-05-21 | 待指定 |
| 配置（config） | peizhi/config | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/configmodule/controller/SystemConfigController.java`；DB：`V19__create_system_config_table.sql`；JPA：`SystemConfigRepository extends JpaRepository`；契约：`docs/openapi-migrated-modules.yaml`（`/api/v1/configs*`）；页面：`web/app/configs/page.tsx`；测试：`SystemConfigControllerIntegrationTests` | 2026-05-21 | 待指定 |
| 就诊（visits） | jiuzhen | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/jiuzhen/controller/VisitController.java`；DB：`V18__create_visit_record_table.sql`；JPA：`VisitRepository extends JpaRepository`；契约：`docs/openapi-migrated-modules.yaml`（`/api/v1/visits`）；页面：`web/app/visits/page.tsx`；测试：`VisitControllerIntegrationTests` | 2026-05-21 | 待指定 |
| 病例（medicalRecords） | bingli | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/bingli/controller/BingliController.java`；DB：`V20__create_bingli_record_table.sql`；JPA：`BingliRepository extends JpaRepository`；契约：`docs/openapi-migrated-modules.yaml`（`/api/v1/medical-records`）；页面：`web/app/medical-records/page.tsx`；测试：`BingliControllerIntegrationTests` | 2026-05-21 | 待指定 |
| 健康档案（healthRecords） | jiuankangdangan | Diverged | **模块已于 2026-06-10 下线（V50 删表）**：自由文本档案职责由患者档案扩展字段（V47）与慢病随访（V49）结构化承接，AI RAG 摄取源同步移除 | 2026-06-10 | 待指定 |
| 医生（doctors） | yisheng | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/yisheng/controller/DoctorController.java`；DB：`V17__create_doctor_profile_table.sql`；JPA：`DoctorRepository extends JpaRepository`；契约：`docs/openapi-migrated-modules.yaml`（`/api/v1/doctors`）；页面：`web/app/doctors/page.tsx`；测试：`DoctorControllerIntegrationTests` | 2026-05-21 | 待指定 |
| 患者（patient） | yonghu | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/patient/controller/PatientController.java`；DB：`V22__add_patient_reception_roles_permissions.sql`、`V23__create_patient_profile_table.sql`、`V47__extend_patient_profile_medical_fields.sql`（出生日期/住址/过敏史/既往病史/紧急联系人）；JPA：`PatientProfileRepository extends JpaRepository`；共享：`UserAccountService`（由 AuthService 实现）；页面：`web/app/patients/page.tsx`、`web/app/patients/[id]/page.tsx`（360° 视图）；测试：`PatientControllerIntegrationTests` | 2026-06-10 | 待指定 |
| 家医签约（family-doctor-contracts） | —（新增能力） | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/familydoctor/controller/ContractController.java`；DB：`V48__create_family_doctor_contract.sql`（含一患者一份生效签约的部分唯一索引）；JPA：`FamilyDoctorContractRepository`；接口：`/api/v1/family-doctor-contracts*`；页面：`web/app/family-doctor-contracts/page.tsx`；测试：`ContractControllerIntegrationTests` | 2026-06-10 | 待指定 |
| 慢病随访（followups） | —（新增能力） | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/followup/controller/FollowupController.java`；DB：`V49__create_patient_followup.sql`（血压/血糖/身高体重/心率，BMI 服务端计算）；JPA：`PatientFollowupRepository`；接口：`/api/v1/followups*`；页面：`web/app/followups/page.tsx` + 360° 视图趋势卡；测试：`FollowupControllerIntegrationTests` | 2026-06-10 | 待指定 |
| 前台（reception） | qiantai | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/reception/controller/ReceptionController.java`；DB：`V22__add_patient_reception_roles_permissions.sql`、`V24__create_reception_profile_table.sql`；JPA：`ReceptionProfileRepository extends JpaRepository`；共享：`UserAccountService`（由 AuthService 实现）；页面：`web/app/receptions/page.tsx`；测试：`ReceptionControllerIntegrationTests` | 2026-05-23 | 待指定 |
| AI 基础设施（ai-core） | — | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/ai/{client,config,audit,ratelimit,common}/*`；DB：V37（权限）、V38（`ai_audit_log`）；指标：`hospital_ai_calls_total/latency_seconds/tokens_total`；用户文档：`docs/ai-features.md` | 2026-05-24 | 待指定 |
| AI 病历识别（ai-vision） | — | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/ai/vision/*`；DB：V39；接口：`POST /api/v1/ai/vision/parse-medical-record`；页面：病历表单 AI 识别按钮 + `web/components/business/ai-suggestion-panel.tsx`；测试：`MedicalRecordExtractorTest`、`AiVisionControllerIntegrationTests` | 2026-05-24 | 待指定 |
| AI 患者 RAG（ai-patient-rag） | — | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/ai/{embedding,ingestion,rag,admin}/*`；DB：V40/V41/V42（pgvector 扩展、`patient_knowledge_chunk` + HNSW、`ai_consent_at` + `ai_dead_letter`）；接口：`POST /api/v1/ai/patient/{patientId}/ask`、`/consent`、`POST /api/v1/ai/admin/backfill`；页面：`web/app/patients/[id]/page.tsx` + `patient-ai-ask-panel.tsx` + `ai-consent-modal.tsx`；模型：`doubao-embedding-text-240715`（1024 维）+ `doubao-seed-2.0-lite` | 2026-05-24 | 待指定 |
| AI 社区问诊（ai-consult） | — | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/ai/consult/*`；DB：V43；接口：`POST/GET/PATCH/DELETE /api/v1/ai/consult/sessions`、`POST .../messages`（SSE）；页面：`web/app/ai-consult/page.tsx`；guardrail：`resources/guardrail/refused_keywords.txt`；测试：`ConsultGuardrailTest`、`ConsultContextBuilderTest`、`AiConsultServiceTest`、`AiCallTemplateStreamTest` | 2026-05-24 | 待指定 |

## 迁移顺序调整记录

- **安全硬化 + CI + 部署（2026-06-11）**：登录失败限流 `LoginAttemptService`（用户名+IP 连错 5 次锁 15 分钟，HTTP 429 + Retry-After，登录成功清零；内存实现仅单实例有效，配置 `security.login-throttle.*`）。生产部署链路落地：`application-prod.yml`（机密无默认值 fail-fast）、后端/前端多阶段 Dockerfile、`docker-compose.prod.yml` + `.env.prod.example`，文档 `docs/deployment.md`；开发态 JWT 占位密钥改为 `${HOSPITAL_JWT_SECRET:占位}` 可覆写。前端引入 vitest（`npm run test`，覆盖 permissions / token-storage / entity-page 表单工具），`pnpm-lock.yaml` 入库。新增 `.github/workflows/ci.yml`：后端 TestContainers 全量测试 + 前端 lint/test/build。

- **代码优化清理（2026-06-10）**：V54 删除 `file_metadata` 表，整个磁盘文件存储模块 `com.hospital.file`（`/api/v1/files`）下线（前端统一走 `/api/v1/photos` 数据库存储）；移除 `/api/v1/bingli` 兼容路由及 `bingli:*` 授权回退（权限码已在 V50 期清空）；dictionary 模块改用 `common.PageResponse`（删除重复定义）。visits / followups / medical-records 三个增长型模块的列表从「全量加载 + 内存切片」改为数据库分页（SQL `LIMIT/OFFSET` + `COUNT`），响应结构不变。前端 `EntityManagementPage` 机械拆分出 `entity-page/`（types / modal / detail-value / 共用表单字段渲染器），公共类型仍从原路径导出。补齐 RBAC / 家医签约 / 慢病随访 / 科室四个模块的集成测试（TestContainers，CI 运行）。

- **模块串联（2026-06-10）**：V51 打通核心业务链 —— `doctor_profile.department_id`（医生归属科室）、`visit_record.doctor_id`（挂号选医生，前端按科室联动过滤）、`medical_record.visit_id`（病历挂接就诊，就诊列表「写病历」直达预填）；病历保存按处方明细自动扣减药品库存并写 `medication_inventory_log`（库存不足 409 回滚，删病历返还库存）。V52 给 RECEPTION 角色补发工作权限（挂号/患者读写 + 医生/科室/签约/随访只读）。新增 `GET /api/v1/dashboard/summary` 按权限裁剪的首页聚合指标。签约到期自动显示 EXPIRED（查询期换算 + 建约时惰性落库）。

- **健康档案模块下线（2026-06-10）**：V50 删除 `health_record` 表、`health-records:*` 权限、`jiuankangdangan_types` 字典及 AI 死信残留。原因：自由文本档案与患者档案扩展（V47 过敏史/既往病史）+ 慢病随访（V49 结构化指标）完全重叠。AI 患者 RAG 摄取源从三个收敛为 medical_record / visit 两个。

- **患者档案补强（2026-06-10）**：V47 扩展 `patient_profile`（出生日期/住址/过敏史/既往病史/紧急联系人）；V48 新增 `family_doctor_contract`（家医签约，部分唯一索引保证一患者仅一份生效签约）；V49 新增 `patient_followup`（慢病随访健康指标）。前端新增 `/family-doctor-contracts`、`/followups` 页面，患者详情页升级为 360° 视图（基本信息 + 签约 + 健康趋势 + 就诊/档案时间线 + AI 问询）。

- **7 个业务模块 JPA 化（2026-05-21）**：medications、family-doctors、doctors、visits、system-config、medical-records、health-records 从 `InMemoryXxxRepository` 升级为 JPA + PostgreSQL 持久化。Flyway V15–V21 建表，entity 从 record 转为 `@Entity` class，新增 `NotFoundException`（→ 404）与 `IllegalArgumentException`（→ 409）分离处理。新增 7 个集成测试（TestContainers）。

- **配置权限命名迁移顺序调整（2026-05-20）**：将 `V4__align_configs_permission_naming.sql` 中依赖 `app_permission` / `app_role_permission` 的 SQL 迁移到 `V13__align_configs_permission_naming.sql`，并将 V4 改为安全空操作注释。
- **调整原因**：空库初始化时，V4 执行阶段早于 `V8__permissions.sql`（权限表创建），会出现依赖未满足导致迁移失败。

