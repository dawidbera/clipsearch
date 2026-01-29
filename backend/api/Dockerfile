## Stage 1: Build
FROM registry.access.redhat.com/ubi8/openjdk-17:1.20 AS build
USER root
COPY . /code/
WORKDIR /code
# Use the wrapper from the project root if available, or assume mvn is in path, or use a bundled maven. 
# Since we are in a clean container, we install maven or use the wrapper.
# Simpler approach for UBI: Install maven or rely on successful build context.
# Let's assume we copy the parent pom and build from root context usually, but here context is module specific? 
# To support monorepo build properly in docker without complex context, let's use a standard maven image.

FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY common /app/common
COPY api /app/api
COPY worker /app/worker
# Build everything to ensure dependencies are met
RUN mvn clean package -DskipTests -pl api -am

## Stage 2: Run
FROM registry.access.redhat.com/ubi8/openjdk-17:1.20
ENV LANGUAGE='en_US:en'
# Copy from builder
COPY --from=builder /app/api/target/quarkus-app/lib/ /deployments/lib/
COPY --from=builder /app/api/target/quarkus-app/*.jar /deployments/
COPY --from=builder /app/api/target/quarkus-app/app/ /deployments/app/
COPY --from=builder /app/api/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080
USER 1001
ENTRYPOINT ["java", "-jar", "/deployments/quarkus-run.jar"]