# Permission Matrix

## 统一命名规范

- 格式：`<resource>:<action>`
- `resource` 使用 **kebab-case 复数**（例如 `medications`、`family-doctors`）。
- `action` 当前使用：`read`、`write`、`status`（以及少量历史权限如 `delete`、`auth:me:read`）。

## 模块权限对照

| 模块 | API 路径前缀 | 权限前缀（统一后） | 权限集合 |
|---|---|---|---|
| 药品管理 | `/api/v1/medications` | `medications` | `medications:read` / `medications:write` / `medications:status` |
| 家庭医生签约 | `/api/v1/family-doctors` | `family-doctors` | `family-doctors:read` / `family-doctors:write` / `family-doctors:status` |
| 就诊记录 | `/api/v1/visits` | `visits` | `visits:read` / `visits:write` / `visits:status` |
| 系统配置 | `/api/v1/configs` | `configs` | `configs:read` / `configs:write` / `configs:status` |
| 字典管理 | `/api/v1/dictionaries` | `dictionary`（历史保留） | `dictionary:read` / `dictionary:write` / `dictionary:delete` |

## 本次对齐范围

- 前端：`web/app/dashboard/page.tsx` 与 `EntityManagementPage` 使用的权限前缀与后端一致。
- 后端：`MedicationController`、`FamilyDoctorController`、`VisitController`、`SystemConfigController` 的 `@PreAuthorize` 已统一到 kebab-case 复数。
- 数据库：`V4__rbac_permissions.sql` 新增并分配上述四个模块权限，避免登录后权限集缺失导致前后端校验不一致。

## 漂移防护建议

- 新增业务模块时，必须同时更新：
  1. 后端 `@PreAuthorize`；
  2. `app_permission` 种子（及角色映射）；
  3. 前端页面权限常量；
  4. 本文档矩阵。
