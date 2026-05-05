# 迁移状态总表（唯一状态源）

> 本文件是迁移完成度唯一状态源（Single Source of Truth）。
> `docs/route-migration-final-report.md`、`docs/p1-iteration-module-plan.md`、`docs/api-contract-governance.md` 中的完成度信息必须引用本文件，不得重复维护独立口径。

## 维护要求
- 每次合并“迁移相关 PR”时，必须同步更新本文件中的模块状态、证据链接、最后更新时间、负责人。
- 模块状态仅允许以下枚举：`Not Started` / `Partial` / `Done` / `Diverged`。

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
| 药品（medication） | yaopin | Not Started | 代码：`server/legacy/src/main/java/com/controller/YaopinController.java`（legacy 现状）；契约：`docs/openapi-migrated-modules.yaml`（待补充）；页面：`server/legacy/src/main/resources/admin/admin/src/views/modules/yaopin/list.vue`；测试：待补充 | 2026-05-05 | 待指定 |
| 家庭医生（familyDoctor） | jiatingyisheng | Not Started | 代码：`server/legacy/src/main/java/com/controller/JiatingyishengController.java`（legacy 现状）；契约：`docs/openapi-migrated-modules.yaml`（待补充）；页面：`server/legacy/src/main/resources/admin/admin/src/views/modules/jiatingyisheng/list.vue`；测试：待补充 | 2026-05-05 | 待指定 |
| 配置（config） | peizhi/config | Not Started | 代码：`server/legacy/src/main/java/com/controller/ConfigController.java`（legacy 现状）；契约：`docs/openapi-migrated-modules.yaml`（待补充）；页面：`server/legacy/src/main/resources/admin/admin/src/views/modules/config/list.vue`；测试：待补充 | 2026-05-05 | 待指定 |

