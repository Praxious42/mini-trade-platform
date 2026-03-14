# Multi-stage Dockerfile for authorisation-server
FROM maven:3.9.4-amazoncorretto-17 AS builder
WORKDIR /workspace
COPY . .
RUN mvn -q -DskipTests -pl authorisation-server -am package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /workspace/authorisation-server/target/*-SNAPSHOT.jar ./authorisation-server.jar
EXPOSE 8084
ENTRYPOINT ["java","-jar","/app/authorisation-server.jar"]

