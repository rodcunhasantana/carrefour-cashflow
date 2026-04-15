# Carrefour Cash Flow System

Sistema de controle de fluxo de caixa para gerenciamento de lançamentos financeiros (débitos e créditos) e geração de saldos consolidados diários.

## Visão Geral

Este sistema é composto por dois microserviços principais:

- **Transaction Service**: Gerencia os lançamentos financeiros (débitos e créditos)
- **Daily Balance Service**: Gera e consulta saldos consolidados diários

## Tecnologias

- Java 21 / Spring Boot 3.2.4
- JDBC puro com PostgreSQL (sem JPA/Hibernate)
- Google Cloud Pub/Sub (mensageria assíncrona — emulador local via Docker)
- Resilience4j — Circuit Breaker e Retry nos publishers de eventos
- SpringDoc OpenAPI 2.3.0 — Swagger UI em runtime
- Docker / Docker Compose (ambiente local completo)
- Google Cloud Run, Cloud SQL, Secret Manager (produção)

## Estrutura do Projeto

- `/transaction-service` - Microserviço de lançamentos financeiros
- `/dailybalance-service` - Microserviço de saldo consolidado diário
- `/common` - Componentes compartilhados
- `/infrastructure` - Configurações de infraestrutura
- `/docs` - Documentação do projeto

## Começando

### Pré-requisitos

- Java 21+
- Docker Desktop
- Maven
- Google Cloud CLI (`gcloud`)

### Executando localmente

O ambiente local utiliza emuladores GCP para simular os serviços de nuvem.

```bash
# Clone o repositório
git clone https://github.com/seu-usuario/carrefour-cashflow.git
cd carrefour-cashflow

# Iniciar dependências locais (PostgreSQL + emulador Pub/Sub)
docker-compose -f infrastructure/docker/docker-compose.yml up -d

# Compilar e executar os serviços (perfil dev)
cd transaction-service

# Bash / Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# PowerShell (Windows) — aspas obrigatórias nos argumentos -D
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"

# Em outro terminal
cd dailybalance-service

# Bash / Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# PowerShell (Windows)
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"

### Documentação da API (Swagger UI)

Com os serviços rodando:

| Serviço | Swagger UI | OpenAPI JSON |
|---|---|---|
| transaction-service | http://localhost:8080/transaction-service/swagger-ui.html | http://localhost:8080/transaction-service/v3/api-docs |
| dailybalance-service | http://localhost:8081/dailybalance-service/swagger-ui.html | http://localhost:8081/dailybalance-service/v3/api-docs |

### Executando os testes

```bash
# Todos os módulos
mvn clean install

# Apenas um serviço
mvn test -pl transaction-service
mvn test -pl dailybalance-service
```
