# 权限矩阵（验收基线）

## 统一权限模型
- 角色（Role）：`ADMIN`、`USER`
- 资源（Resource）：`dictionary`、`auth`
- 动作（Action）：`read`、`write`、`delete`
- 权限点（Permission）：
  - `dictionary:read`
  - `dictionary:write`
  - `dictionary:delete`
  - `auth:me:read`

## 角色-权限矩阵

| 角色 | dictionary:read(看) | dictionary:write(改) | dictionary:delete(删) | auth:me:read |
|---|---|---|---|---|
| ADMIN | ✅ | ✅ | ✅ | ✅ |
| USER | ✅ | ❌ | ❌ | ✅ |

## legacy 菜单/接口映射（样例）

| legacy 模块 | 菜单 | 接口模式 | 权限点 | 角色 |
|---|---|---|---|---|
| dictionary | 字典管理 | /dictionary/** | dictionary:read | USER |
| dictionary | 字典管理 | /dictionary/** | dictionary:write | ADMIN |
| dictionary | 字典管理 | /dictionary/** | dictionary:delete | ADMIN |
| users | 用户管理 | /users/** | dictionary:read | ADMIN |
| yisheng | 医生管理 | /yisheng/** | dictionary:read | ADMIN |
| yaopin | 药品管理 | /yaopin/** | dictionary:read | ADMIN |
| jiuzhen | 就诊管理 | /jiuzhen/** | dictionary:read | ADMIN |
