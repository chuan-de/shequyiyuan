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
| 健康档案（healthRecords） | jiuankangdangan | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/jiuankangdangan/controller/JiuankangdanganController.java`；DB：`V21__create_jiuankangdangan_record_table.sql`；JPA：`JiuankangdanganRepository extends JpaRepository`；契约：`docs/openapi-migrated-modules.yaml`（`/api/v1/health-records`）；页面：`web/app/health-records/page.tsx`；测试：`JiuankangdanganControllerIntegrationTests` | 2026-05-21 | 待指定 |
| 医生（doctors） | yisheng | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/yisheng/controller/DoctorController.java`；DB：`V17__create_doctor_profile_table.sql`；JPA：`DoctorRepository extends JpaRepository`；契约：`docs/openapi-migrated-modules.yaml`（`/api/v1/doctors`）；页面：`web/app/doctors/page.tsx`；测试：`DoctorControllerIntegrationTests` | 2026-05-21 | 待指定 |
| 患者（patient） | yonghu | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/patient/controller/PatientController.java`；DB：`V22__add_patient_reception_roles_permissions.sql`、`V23__create_patient_profile_table.sql`；JPA：`PatientProfileRepository extends JpaRepository`；共享：`UserAccountService`（由 AuthService 实现）；页面：`web/app/patients/page.tsx`；测试：`PatientControllerIntegrationTests` | 2026-05-23 | 待指定 |
| 前台（reception） | qiantai | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/reception/controller/ReceptionController.java`；DB：`V22__add_patient_reception_roles_permissions.sql`、`V24__create_reception_profile_table.sql`；JPA：`ReceptionProfileRepository extends JpaRepository`；共享：`UserAccountService`（由 AuthService 实现）；页面：`web/app/receptions/page.tsx`；测试：`ReceptionControllerIntegrationTests` | 2026-05-23 | 待指定 |
| AI 基础设施（ai-core） | — | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/ai/{client,config,audit,ratelimit,common}/*`；DB：V37（权限）、V38（`ai_audit_log`）；指标：`hospital_ai_calls_total/latency_seconds/tokens_total`；用户文档：`docs/ai-features.md` | 2026-05-24 | 待指定 |
| AI 病历识别（ai-vision） | — | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/ai/vision/*`；DB：V39；接口：`POST /api/v1/ai/vision/parse-medical-record`；页面：病历表单 AI 识别按钮 + `web/components/business/ai-suggestion-panel.tsx`；测试：`MedicalRecordExtractorTest`、`AiVisionControllerIntegrationTests` | 2026-05-24 | 待指定 |
| AI 患者 RAG（ai-patient-rag） | — | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/ai/{embedding,ingestion,rag,admin}/*`；DB：V40/V41/V42（pgvector 扩展、`patient_knowledge_chunk` + HNSW、`ai_consent_at` + `ai_dead_letter`）；接口：`POST /api/v1/ai/patient/{patientId}/ask`、`/consent`、`POST /api/v1/ai/admin/backfill`；页面：`web/app/patients/[id]/page.tsx` + `patient-ai-ask-panel.tsx` + `ai-consent-modal.tsx`；模型：`doubao-embedding-text-240715`（1024 维）+ `doubao-seed-2.0-lite` | 2026-05-24 | 待指定 |
| AI 社区问诊（ai-consult） | — | Done | 代码：`server/backend-rewrite/src/main/java/com/hospital/ai/consult/*`；DB：V43；接口：`POST/GET/PATCH/DELETE /api/v1/ai/consult/sessions`、`POST .../messages`（SSE）；页面：`web/app/ai-consult/page.tsx`；guardrail：`resources/guardrail/refused_keywords.txt`；测试：`ConsultGuardrailTest`、`ConsultContextBuilderTest`、`AiConsultServiceTest`、`AiCallTemplateStreamTest` | 2026-05-24 | 待指定 |

## 迁移顺序调整记录

- **7 个业务模块 JPA 化（2026-05-21）**：medications、family-doctors、doctors、visits、system-config、medical-records、health-records 从 `InMemoryXxxRepository` 升级为 JPA + PostgreSQL 持久化。Flyway V15–V21 建表，entity 从 record 转为 `@Entity` class，新增 `NotFoundException`（→ 404）与 `IllegalArgumentException`（→ 409）分离处理。新增 7 个集成测试（TestContainers）。

- **配置权限命名迁移顺序调整（2026-05-20）**：将 `V4__align_configs_permission_naming.sql` 中依赖 `app_permission` / `app_role_permission` 的 SQL 迁移到 `V13__align_configs_permission_naming.sql`，并将 V4 改为安全空操作注释。
- **调整原因**：空库初始化时，V4 执行阶段早于 `V8__permissions.sql`（权限表创建），会出现依赖未满足导致迁移失败。

