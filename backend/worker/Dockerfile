## Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common /app/common
COPY api /app/api
COPY worker /app/worker
RUN mvn clean package -DskipTests -pl worker -am

## Stage 2: Run
FROM registry.access.redhat.com/ubi8/openjdk-17:1.20
ENV LANGUAGE='en_US:en'
COPY --from=builder /app/worker/target/quarkus-app/lib/ /deployments/lib/
COPY --from=builder /app/worker/target/quarkus-app/*.jar /deployments/
COPY --from=builder /app/worker/target/quarkus-app/app/ /deployments/app/
COPY --from=builder /app/worker/target/quarkus-app/quarkus/ /deployments/quarkus/

USER 1001
ENTRYPOINT ["java", "-jar", "/deployments/quarkus-run.jar"]