# shequyiyuan workspace

This repository is now organized as a two-app workspace:

- `server/`: backend code (legacy code and Spring Boot 3 rewrite)
- `web/`: Next.js frontend scaffold

## Layout

- `server/legacy/`: original Spring Boot + Vue2 bundled project moved from root
- `server/backend-rewrite/`: Spring Boot 3 rewrite project (`com.hospital`)
- `web/`: independent Next.js app for frontend rewrite

## 本地启动

1. 启动 PostgreSQL：

```bash
docker compose up -d postgres
```

2. 启动后端（Spring Boot）：

```bash
cd server/backend-rewrite && mvn spring-boot:run
```

3. 启动前端（Next.js）：

```bash
cd web && npm install && npm run dev
```

### 前端环境变量示例

在 `web/.env.local` 中配置：

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

### 接口验证命令

1. 健康检查：

```bash
curl http://localhost:8080/api/v1/health
```

2. 注册接口：

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Password123!",
    "name": "Test User"
  }'
```

3. 登录接口：

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Password123!"
  }'
```

4. 获取当前用户 `/api/v1/auth/me`（将 `<JWT_TOKEN>` 替换为登录接口返回的 token）：

```bash
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

### 排错说明

- **403 / CORS 问题**：检查后端 CORS 白名单是否包含前端地址（如 `http://localhost:3000`），并确认前端 `NEXT_PUBLIC_API_BASE_URL` 指向正确后端地址。
- **Postgres 未启动**：执行 `docker compose ps` 确认 `postgres` 服务状态，必要时重新执行 `docker compose up -d postgres`。
- **Flyway migration 失败**：查看后端启动日志与数据库连接配置，确认数据库可连接且迁移脚本版本连续、未重复执行。
- **JWT secret 过短**：检查后端 JWT 配置，确保 secret 长度满足签名算法要求（建议至少 32 字节）。

## 团队前端规范（补充）

- `web/components/ui/` 与 `web/components/layout/` 组件文件统一使用 **小写 kebab-case** 文件名。
- **导入路径大小写必须与文件名完全一致**（包括别名路径 `@/components/...`）。
- 禁止在仓库中并存仅大小写不同的重复组件文件。
