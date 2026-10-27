# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY boot/pom.xml boot/pom.xml
COPY lab/pom.xml lab/pom.xml
COPY session/pom.xml session/pom.xml
COPY knowledge/pom.xml knowledge/pom.xml
COPY retrieval/pom.xml retrieval/pom.xml
COPY diagnosis/pom.xml diagnosis/pom.xml
COPY safety/pom.xml safety/pom.xml
COPY evidence/pom.xml evidence/pom.xml
COPY report/pom.xml report/pom.xml
COPY evaluation/pom.xml evaluation/pom.xml
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl && addgroup -S app && adduser -S -G app -h /app -u 1000 app
COPY --from=builder --chown=app:app /build/boot/target/cloudlab-copilot-boot-*.jar app.jar
USER app
EXPOSE 8080
LABEL org.opencontainers.image.title="cloudlab-copilot-backend" \
      org.opencontainers.image.source="https://github.com/Aric-byte/ICT" \
      org.opencontainers.image.description="CloudLab Copilot backend service" \
      org.opencontainers.image.version="1.0.0-SNAPSHOT" \
      org.opencontainers.image.created="2026-10-22"
ENTRYPOINT ["java", "-jar", "app.jar"]