# 问题定位手册

## 1. 从用户报错收集信息
- 让前端用户提供：报错时间、页面路径、错误提示中的“追踪ID(Trace ID)”。
- 前端会把后端返回的 `traceId` 拼接在错误提示中。

## 2. 用 Trace ID 关联后端日志
- 在应用日志中检索 `traceId=<id>`。
- 重点关注：`ApiExceptionHandler` 的异常日志和 `AUDIT` 审计日志。

## 3. 确认请求链路与接口
- 根据日志中的 URI、HTTP 方法、状态码，定位具体接口。
- 若是认证失败，优先检查 `/api/v1/auth/login`；若是权限问题，检查 403 与角色配置。

## 4. 结合审计日志排查关键操作
- 登录：`action=LOGIN`
- 注册/权限变更：`action=REGISTER` 或后续扩展动作
- 文件/关键数据变更：按 `action` 与 `target` 检索

## 5. 结合指标判断是否系统性故障
- 指标：
  - `hospital_api_requests_total`
  - `hospital_api_request_duration_ms`
  - `hospital_api_errors_total`
- 建议告警阈值：
  - P95 延迟 > 500ms（持续 5 分钟）
  - 错误率 > 5%（持续 3 分钟）

## 6. 快速定位闭环
1) 用户提供 traceId。  
2) 日志检索 traceId。  
3) 锁定接口 + 入参校验/鉴权/业务异常。  
4) 审计日志确认操作人和对象。  
5) 指标确认影响范围后修复与回归。
