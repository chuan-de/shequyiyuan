# hospital-backend-rewrite

Spring Boot 3.x backend rewrite baseline using package prefix `com.hospital`.

## Current scope
- Java 21 + Spring Boot 3.3.x baseline
- PostgreSQL datasource configuration
- Flyway migration baseline
- Basic security filter chain
- Health check API at `/api/v1/health`

## Run
```bash
mvn spring-boot:run
```

## Next milestones
1. Build authentication module with JWT.
2. Introduce domain modules (user/clinic/pharmacy).
3. Migrate legacy schema into normalized PostgreSQL schema.
4. Replace legacy token/session auth.
