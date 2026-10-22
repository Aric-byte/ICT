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
RUN mvn dependency:go-offline -B || true
COPY . .
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=builder /build/boot/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]