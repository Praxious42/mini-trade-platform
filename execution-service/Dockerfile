# Multi-stage Dockerfile for execution-service
# Builds the Spring Boot fat JAR with Maven and produces a small runtime image

FROM maven:3.9.4-amazoncorretto-17 AS builder
WORKDIR /workspace
# Copy the entire repo so Maven can resolve parent POMs and shared modules
COPY . .
# Build only the execution-service module (and required modules)
RUN mvn -q -DskipTests -pl execution-service -am package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Copy the built jar from the builder stage
COPY --from=builder /workspace/execution-service/target/*-SNAPSHOT.jar ./execution-service.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/execution-service.jar"]

