# Legacy Controller 模块基线清单

基线来源：`server/legacy/src/main/java/com/controller/*Controller.java`。

| 模块名 | legacy 路由前缀 | 核心 CRUD | 状态流转 | 权限点（legacy / rewrite） |
|---|---|---|---|---|
| yaopin（药品） | `/yaopin` | `/page` 列表、`/info/{id}` 详情、`/save` 新增、`/update` 编辑、`/delete` 删除、`/batchInsert` 批量导入 | legacy 未显式状态机；rewrite 统一为 `ENABLED -> DISABLED` 双向流转 | legacy 通过 session `role` 做数据隔离；rewrite 映射 `medication:read` / `medication:write` / `medication:status` |
| jiatingyisheng（家庭医生） | `/jiatingyisheng` | `/page` 列表、`/info/{id}` 详情、`/save` 新增、`/update` 编辑、`/delete` 删除、`/batchInsert` 批量导入 | legacy 未显式状态机；rewrite 定义 `PENDING -> ACTIVE -> SUSPENDED -> TERMINATED`（受约束迁移） | legacy 通过 session `role` 做数据隔离；rewrite 映射 `familyDoctor:read` / `familyDoctor:write` / `familyDoctor:status` |
| config（配置） | `/config` | `/page`/`/list` 列表、`/info/{id}`/`/detail/{id}` 详情、`/save` 新增、`/update` 编辑、`/delete` 删除 | legacy 未显式状态机；rewrite 统一 `ENABLED <-> DISABLED` | legacy 读接口部分 `@IgnoreAuth`；rewrite 映射 `config:read` / `config:write` / `config:status` |
| yonghu（用户） | `/yonghu` | page/info/save/update/delete（同模板） | 多为启停/逻辑删除 | 建议迁移为 `user:read/write/status` |
| doctor（医生） | `/doctor` | page/info/save/update/delete（同模板） | 多为启停/逻辑删除 | 建议迁移为 `doctor:read/write/status` |
| jiuankangdangan（健康档案） | `/jiuankangdangan` | page/info/save/update/delete（同模板） | 档案状态通常草稿/归档 | 建议迁移为 `healthRecord:read/write/status` |
| jiuzhen（就诊） | `/jiuzhen` | page/info/save/update/delete（同模板） | 就诊流程通常预约/接诊/完成 | 建议迁移为 `visit:read/write/status` |
| bingli（病历） | `/bingli` | page/info/save/update/delete（同模板） | 病历状态通常草稿/签署/归档 | 建议迁移为 `medicalRecord:read/write/status` |
| dictionary（字典） | `/dictionary` | 以字典项管理为主 | 字典项启停 | 已迁移，继续沿用 dictionary 权限域 |
| users（后台用户） | `/users` | 账号管理 CRUD | 账号启停 | 建议迁移为 `adminUser:read/write/status` |
| common/file/qiantai | `/common` `/file` `/qiantai` | 工具/上传/前台聚合接口 | 按业务定义 | 按子资源拆分权限 |

## P1 模块迁移闭环映射

- yaopin：已建立 v1 控制器 + service + dto + domain + repository 层；支持列表/详情/新增/编辑/状态变更、权限、审计钩子、统一异常入口。
- jiatingyisheng：同上，状态机按签约语义收敛。
- config：同上，包含版本号字段用于配置项变更审计。
