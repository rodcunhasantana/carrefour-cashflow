# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml ./
COPY common/pom.xml common/
COPY dailybalance-service/pom.xml dailybalance-service/
RUN mvn dependency:go-offline -pl common,dailybalance-service -am -q

COPY common/src common/src
COPY dailybalance-service/src dailybalance-service/src
RUN mvn clean package -pl common,dailybalance-service -am -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /build/dailybalance-service/target/dailybalance-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
