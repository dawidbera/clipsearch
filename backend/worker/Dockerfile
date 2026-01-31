FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
ENV MAVEN_OPTS="-Xmx6g -XX:+UseG1GC"
COPY pom.xml .
COPY common /app/common
COPY api /app/api
COPY worker /app/worker
RUN mvn clean package -DskipTests -pl worker -am

FROM registry.access.redhat.com/ubi9/ubi-minimal:9.3
RUN microdnf install -y java-17-openjdk-headless && microdnf clean all

WORKDIR /work/
COPY --from=build /app/worker/target/quarkus-app/lib/ /work/lib/
COPY --from=build /app/worker/target/quarkus-app/*.jar /work/
COPY --from=build /app/worker/target/quarkus-app/app/ /work/app/
COPY --from=build /app/worker/target/quarkus-app/quarkus/ /work/quarkus/

EXPOSE 8080
USER 1001

CMD ["java", "-jar", "quarkus-run.jar"]