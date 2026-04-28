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

## API quick start
- `POST /api/v1/auth/register`
  - body: `{ "username": "alice", "password": "password123" }`
- `POST /api/v1/auth/login`
  - body: `{ "username": "alice", "password": "password123" }`
  - returns bearer token
- `GET /api/v1/health`

## Configuration
Set a secure JWT secret before production use:

```yaml
security:
  jwt:
    secret: replace-with-a-32-byte-minimum-secret-key
    access-token-ttl: 1h
```

## Next milestones
1. Replace JDBC auth queries with dedicated repository/domain services.
2. Introduce RBAC permission checks by business module.
3. Migrate legacy schema into normalized PostgreSQL schema.
4. Add integration tests for auth and migration scripts.
