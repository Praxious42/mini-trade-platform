# Multi-stage Dockerfile for order-service
# Builds the Spring Boot fat JAR with Maven and produces a small runtime image

FROM maven:3.9.4-amazoncorretto-17 AS builder
WORKDIR /workspace
# Copy the entire repo so Maven can resolve parent POMs and shared modules
COPY . .
# Build only the order-service module (and required modules) to produce its JAR
RUN mvn -q -DskipTests -pl order-service -am package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Copy the built jar from the builder stage
COPY --from=builder /workspace/order-service/target/*-SNAPSHOT.jar ./order-service.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/order-service.jar"]
