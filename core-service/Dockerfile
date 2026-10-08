# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/
COPY core-service/pom.xml core-service/
COPY gateway/pom.xml gateway/
COPY .mvn .mvn
COPY mvnw mvnw

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B -pl common,core-service

COPY common/src common/src
COPY core-service/src core-service/src

RUN ./mvnw clean package -DskipTests -pl common,core-service -am

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /workspace/core-service/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
