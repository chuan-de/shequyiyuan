# 生产部署指南

单机 Docker Compose 部署。四个容器：PostgreSQL（pgvector）、Qdrant、后端（Spring Boot）、前端（Next.js standalone）。

## 前置条件

- Docker + Docker Compose v2
- 服务器开放 3000（前端）与 8080（后端 API）端口，或前置 Nginx 反代

## 部署步骤

```bash
# 1. 准备环境变量
cp .env.prod.example .env.prod
# 编辑 .env.prod，至少填：
#   HOSPITAL_DB_PASSWORD     数据库口令
#   HOSPITAL_JWT_SECRET      openssl rand -base64 48 生成
#   NEXT_PUBLIC_API_BASE_URL 浏览器访问后端的对外地址，如 http://服务器IP:8080

# 2. 构建并启动
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build

# 3. 验证
curl http://localhost:8080/actuator/health   # {"status":"UP"}
# 浏览器打开 http://服务器IP:3000
```

数据库 schema 由 Flyway 在后端启动时自动迁移（V1–V54），无需手工建表。

## 关键设计

- **机密 fail-fast**：`application-prod.yml` 中数据库口令和 JWT 密钥**没有默认值**，环境变量缺失时后端直接启动失败，杜绝占位符密钥上线。
- **`NEXT_PUBLIC_API_BASE_URL` 是构建期内联**的（Next.js 机制），改地址必须 `--build` 重建 web 镜像，仅改 `.env.prod` 后 `restart` 不生效。
- **数据库不暴露宿主端口**，仅 compose 内网可达；需要远程管理时临时加端口映射或走 SSH 隧道。
- **登录限流是单实例内存实现**（用户名+IP 连错 5 次锁 15 分钟，`security.login-throttle.*` 可调）。多实例部署需改用共享存储（如 Redis），目前规模单实例足够。
- AI 模块默认关闭；要开启需在 `.env.prod` 设 `HOSPITAL_AI_ENABLED=true` 并提供 `HOSPITAL_AI_API_KEY`（火山方舟）。

## 升级发布

```bash
git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

Flyway 自动执行新增迁移；镜像重建有层缓存，通常只重编译变更部分。

## 备份

数据都在两个命名卷里：

```bash
# PostgreSQL 逻辑备份
docker compose -f docker-compose.prod.yml exec postgres pg_dump -U hospital hospital > backup_$(date +%F).sql

# Qdrant（AI 向量，可重建，丢失不致命）：备份 hospital_qdrant_prod 卷即可
```

恢复：`docker compose exec -T postgres psql -U hospital hospital < backup.sql`。

## 建议的反向代理（可选）

生产建议前置 Nginx/Caddy 做 TLS 终结，把 `https://example.com` 转发到 web:3000、`https://api.example.com` 转发到 backend:8080，并保留 `X-Forwarded-For`（登录限流按来源 IP 计数依赖它）。
