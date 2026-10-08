# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/
COPY core-service/pom.xml core-service/
COPY gateway/pom.xml gateway/
COPY .mvn .mvn
COPY mvnw mvnw

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B -pl gateway

COPY gateway/src gateway/src

RUN ./mvnw clean package -DskipTests -pl gateway

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /workspace/gateway/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
