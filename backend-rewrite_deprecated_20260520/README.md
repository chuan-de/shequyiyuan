> [!WARNING]
> 该目录是历史副本（archive snapshot），仅用于对照，**禁止作为运行入口**。
>
> 当前唯一可运行后端目录：`server/backend-rewrite/`。

# ⚠️ DEPRECATED DIRECTORY

> `backend-rewrite/` 为历史副本目录，**不是当前迭代主线后端目录**。
>
> 当前唯一后端主线请使用：`server/backend-rewrite/`。

---

# hospital-backend-rewrite

Spring Boot 3.x backend rewrite baseline using package prefix `com.hospital`.

## Current scope
- Java 21 + Spring Boot 3.3.x baseline
- PostgreSQL datasource configuration
- Flyway migration baseline
- JWT authentication bootstrap (register/login + request filter)
- Health check API at `/api/v1/health`

## Run
```bash
mvn spring-boot:run
```
