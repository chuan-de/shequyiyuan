# 多阶段构建：maven 编译 → 仅携带 JRE 的运行镜像。
# 构建上下文 = server/backend-rewrite/（见根目录 docker-compose.prod.yml）。
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# 先单独拷 pom 拉依赖，源码改动时可命中 Docker 层缓存
COPY pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
