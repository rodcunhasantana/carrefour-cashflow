# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copia o pom.xml raiz e os módulos necessários para resolver dependências primeiro
COPY pom.xml ./
COPY common/pom.xml common/
COPY transaction-service/pom.xml transaction-service/

# Baixa dependências antes de copiar o código-fonte (cache de camadas Docker)
RUN mvn dependency:go-offline -pl common,transaction-service -am -q

# Copia os fontes
COPY common/src common/src
COPY transaction-service/src transaction-service/src

# Empacota pulando os testes (já rodados em CI)
RUN mvn clean package -pl common,transaction-service -am -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Usuário não-root por segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /build/transaction-service/target/transaction-service-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
