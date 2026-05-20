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
| 药品（medication） | yaopin | Partial | 代码：`server/backend-rewrite/src/main/java/com/hospital/yaopin/controller/MedicationController.java`；契约：`docs/openapi-migrated-modules.yaml`（`/api/v1/medications*`）；页面：`web/app/medications/page.tsx`；测试：待补充（缺少集成测试证据） | 2026-05-20 | 待指定 |
| 家庭医生（familyDoctor） | jiatingyisheng | Partial | 代码：`server/backend-rewrite/src/main/java/com/hospital/jiatingyisheng/controller/FamilyDoctorController.java`；契约：`docs/openapi-migrated-modules.yaml`（`/api/v1/family-doctors*`）；页面：`web/app/family-doctors/page.tsx`；测试：待补充（缺少集成测试证据） | 2026-05-20 | 待指定 |
| 配置（config） | peizhi/config | Partial | 代码：`server/backend-rewrite/src/main/java/com/hospital/configmodule/controller/SystemConfigController.java`；契约：`docs/openapi-migrated-modules.yaml`（`/api/v1/configs*`）；页面：`web/app/configs/page.tsx`；测试：待补充（缺少集成测试证据） | 2026-05-20 | 待指定 |
| 就诊（visits） | jiuzhen | Partial | 代码：`server/backend-rewrite/src/main/java/com/hospital/jiuzhen/controller/VisitController.java`；契约：`docs/openapi-migrated-modules.yaml`（`/api/v1/visits`）；页面：`web/app/visits/page.tsx`；测试：待补充（缺少集成测试证据） | 2026-05-20 | 待指定 |
| 病例（medicalRecords） | bingli | Partial | 代码：待补充（rewrite 控制器未落地）；契约：`docs/openapi-migrated-modules.yaml`（`/api/v1/medical-records`）；页面：`web/app/medical-records/page.tsx`；测试：待补充 | 2026-05-20 | 待指定 |
| 健康档案（healthRecords） | jiuankangdangan | Partial | 代码：待补充（rewrite 控制器未落地）；契约：`docs/openapi-migrated-modules.yaml`（`/api/v1/health-records`）；页面：`web/app/health-records/page.tsx`；测试：待补充 | 2026-05-20 | 待指定 |
| 医生（doctors） | yisheng | Partial | 代码：`server/backend-rewrite/src/main/java/com/hospital/yisheng/controller/DoctorController.java`；契约：`docs/openapi-migrated-modules.yaml`（`/api/v1/doctors`）；页面：`web/app/doctors/page.tsx`；测试：待补充（缺少集成测试证据） | 2026-05-20 | 待指定 |

## 迁移顺序调整记录

- **配置权限命名迁移顺序调整（2026-05-20）**：将 `V4__align_configs_permission_naming.sql` 中依赖 `app_permission` / `app_role_permission` 的 SQL 迁移到 `V13__align_configs_permission_naming.sql`，并将 V4 改为安全空操作注释。
- **调整原因**：空库初始化时，V4 执行阶段早于 `V8__permissions.sql`（权限表创建），会出现依赖未满足导致迁移失败。

