# Rewrite 联调核查记录（2026-05-20）

## 1) 后端启动目录
- 已按 `README.md` 确认后端唯一启动目录为：`server/backend-rewrite/`。
- 推荐启动命令：`cd server/backend-rewrite && mvn spring-boot:run`。

## 2) 前端联调 API 地址
- `web/.env.example` 默认值为：`NEXT_PUBLIC_API_BASE_URL=http://localhost:8080`。
- `web/lib/api.ts` 在未配置环境变量时也默认回退到 `http://localhost:8080`。

## 3) 网关/反向代理检查
- 在仓库内未发现 Nginx/网关配置将 `/api/v1/*` 转发到 legacy 的规则。
- 当前前端通过 `NEXT_PUBLIC_API_BASE_URL + /api/v1/*` 直接请求后端。

## 4) 健康检查与 4 个模块列表接口联调验证
尝试在 `server/backend-rewrite/` 启动服务后再验证：
- `GET /api/v1/health`
- `GET /api/v1/dictionaries`
- `GET /api/v1/medications`
- `GET /api/v1/family-doctors`
- `GET /api/v1/visits`

本次环境受限：
- `docker compose` 不可用（`docker: command not found`），无法按 README 拉起本地 Postgres。
- Maven 拉取 Spring Boot parent 依赖失败（`repo.maven.apache.org` 返回 403），导致 rewrite 服务未能启动。

结论：
- 代码与配置层面已对齐 rewrite 入口与 API base URL。
- 运行态接口验证需在可访问 Maven Central 且具备数据库依赖的环境中重试。
