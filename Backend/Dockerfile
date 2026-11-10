FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package



FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/improvement-app-backend-0.0.1-SNAPSHOT.jar /app/improvement-app-backend-0.0.1-SNAPSHOT.jar

HEALTHCHECK --interval=30s --timeout=3s CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
ENTRYPOINT exec java $JAVA_OPTS -jar improvement-app-backend-0.0.1-SNAPSHOT.jar

