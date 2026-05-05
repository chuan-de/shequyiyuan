# 回归清单与执行门槛（合并后固定执行）

## 1) 覆盖范围（Happy Path）
每次主分支合并后，必须执行以下已迁移核心模块集成测试：

- 认证模块：注册、登录、获取当前用户、兼容拼音路由。
- 字典模块：字典项增删改查完整流程。
- 核心基础模块：健康检查英文路由与拼音兼容路由。

对应自动化用例：

- `AuthControllerIntegrationTests`
- `DictionaryControllerIntegrationTests`
- `HealthControllerRouteCompatibilityTests`

## 2) 关键异常路径（必须覆盖）
以下异常路径纳入固定门禁：

- `401 Unauthorized`：未登录访问受保护接口。
- `403 Forbidden`：非授权角色访问管理员接口。
- `404 Not Found`：访问不存在资源。
- `400 Bad Request`：参数校验失败（如空字段、非法数值）。

## 3) 执行门槛（Merge Gate）
满足以下条件才允许合并后的版本进入下一环境：

1. 指定回归清单全量执行。
2. 关键路径（Happy Path + 异常路径）通过率 **100%**。
3. 测试任务无阻塞失败（环境原因需在流水线备注并补跑）。

推荐执行命令：

```bash
./mvnw -Dtest=AuthControllerIntegrationTests,DictionaryControllerIntegrationTests,HealthControllerRouteCompatibilityTests test
```

## 4) 失败闭环要求
若任一回归用例失败，必须在当次迭代执行以下闭环动作：

1. 关联缺陷单（Bug ID）并在提交或流水线日志中标注。
2. 记录失败用例名称、影响范围、根因分析。
3. 提交修复后补充或更新自动化用例，防止回归。
4. 在下一迭代结束前验证缺陷状态为已关闭。

> 建议在 CI 中将“缺陷单号”设为失败重试前置校验项，确保问题可追踪。
