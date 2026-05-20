# P1 执行任务看板（2026-05-20）

> 目标：在当前“页面壳 + 部分接口”的基础上，优先打通可联调链路，随后补齐模块闭环。

## 一、当前基线

- 已可联调模块（后端 rewrite 控制器已存在）：
  - `medications`：`/api/v1/medications*`
  - `family-doctors`：`/api/v1/family-doctors*`
  - `configs`：`/api/v1/configs*`
- 待补齐 rewrite 控制器模块（前端页面和契约已预留）：
  - `visits`
  - `medical-records`
  - `health-records`
  - `doctors`

## 二、任务分组与优先级

### A 组：后端接口补齐（P0）

1. `visits` 模块：
   - 新增 controller/service/repository/domain/dto
   - 落地接口：`GET /{list}`、`GET /{id}`、`POST`、`PUT /{id}`、`PATCH /{id}/status`
2. `doctors` 模块：
   - 同 `visits` 的接口组合与鉴权模型
3. `medical-records` 与 `health-records`：
   - 基于统一 CRUD 模板补齐
4. 统一事项：
   - 权限位统一为 `xxx:read`、`xxx:write`、`xxx:status`
   - 异常码对齐 `NOT_FOUND` / `VALIDATION_ERROR` / `FORBIDDEN`

**完成定义（DoD）**
- OpenAPI 有对应 path 与状态码说明。
- 后端有至少 1 个集成测试覆盖“列表 + 新增 + 状态变更”。

### B 组：前端联调收口（P0）

1. 对 `medications`、`family-doctors`、`configs` 完成端到端联调：
   - 列表查询
   - 详情打开
   - 新增保存
   - 编辑保存
   - 状态变更
2. 故障处理：
   - 401 自动跳登录
   - 403 权限文案准确
   - 5xx 使用统一错误提示

**完成定义（DoD）**
- 每个页面输出“联调记录表”：请求、返回、失败原因、修复结论。

### C 组：文档与质量治理（P1）

1. 每次模块推进后同步更新：
   - `docs/migration-status.md`
   - `docs/openapi-migrated-modules.yaml`
2. 增补“证据链”：
   - 代码路径
   - 契约路径
   - 页面路径
   - 测试用例名

## 三、建议排期（5 天）

- Day 1：补 `visits`、`doctors` 控制器与 DTO；前端联调三个已实现模块
- Day 2：补 `medical-records`、`health-records`；修复联调阻塞项
- Day 3：补集成测试；修正 OpenAPI 与鉴权差异
- Day 4：全链路回归（前端 + 后端）
- Day 5：冻结发布候选版本，更新迁移报告与风险清单

## 四、风险与应对

- 风险：依赖仓库网络异常导致 Maven 无法拉取依赖（403）。
  - 应对：先执行不依赖远端的静态检查与代码审查，联调环境使用缓存镜像仓库。
- 风险：前端权限前缀与后端 authority 命名不一致。
  - 应对：建立权限命名映射表并在 PR 模板中增加检查项。
- 风险：状态表更新滞后导致排期误判。
  - 应对：把“更新 `migration-status.md`”设为迁移 PR 的必选 checklist。
