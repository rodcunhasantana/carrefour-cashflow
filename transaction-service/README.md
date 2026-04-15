# Transaction Service

Microserviço responsável pelo gerenciamento do ciclo de vida das transações financeiras no sistema **Carrefour Cashflow**. Registra débitos e créditos, valida regras de negócio e publica eventos no Google Cloud Pub/Sub para consumo pelo Daily Balance Service.

## Sumário

- [Visão Geral](#visão-geral)
- [Stack](#stack)
- [Pré-requisitos](#pré-requisitos)
- [Configuração Local](#configuração-local)
- [Executando o Serviço](#executando-o-serviço)
- [API](#api)
- [Banco de Dados](#banco-de-dados)
- [Mensageria](#mensageria)
- [Observabilidade](#observabilidade)
- [Testes](#testes)
- [Estrutura do Projeto](#estrutura-do-projeto)

---

## Visão Geral

| Propriedade     | Valor                        |
|-----------------|------------------------------|
| Artefato        | `transaction-service`        |
| Versão          | `1.0.0-SNAPSHOT`             |
| Porta           | `8080`                       |
| Context path    | `/transaction-service`       |
| Perfil padrão   | `dev`                        |

### Responsabilidades

- Criar e consultar transações financeiras (créditos e débitos)
- Validar regras de negócio (valores, tipos, períodos contábeis)
- Processar estornos de transações
- Publicar eventos `transaction-created` e `transaction-reversed` no Cloud Pub/Sub
- Gerar e armazenar relatórios no Cloud Storage

---

## Stack

| Tecnologia              | Versão        |
|-------------------------|---------------|
| Java                    | 21            |
| Spring Boot             | 3.2.4         |
| Spring JDBC             | (BOM)         |
| PostgreSQL / Cloud SQL  | 15            |
| Google Cloud Pub/Sub    | 1.2.8.RELEASE |
| Resilience4j            | 2.1.0         |
| Lombok                  | (BOM)         |
| H2 (testes)             | (BOM)         |

> **Nota:** JPA/Hibernate é **explicitamente excluído**. Todo acesso ao banco de dados é feito via `JdbcTemplate` ou `NamedParameterJdbcTemplate`.

---

## Pré-requisitos

- Java 21+
- Maven 3.9+ (ou use o wrapper `./mvnw`)
- Docker Desktop
- [Google Cloud CLI](https://cloud.google.com/sdk/docs/install) (`gcloud`)

---

## Configuração Local

### 1. Iniciar dependências via Docker

```bash
docker-compose -f ../infrastructure/docker/docker-compose.yml up -d
```

Isso sobe:
- **PostgreSQL** na porta `5432` — banco `transaction_db`
- **Pub/Sub Emulator** na porta `8085`

### 2. Variáveis de ambiente (dev)

O perfil `dev` já aponta para as dependências locais via `application-dev.yml`. Nenhuma variável adicional é necessária para rodar localmente.

| Variável              | Dev (padrão)                                    |
|-----------------------|-------------------------------------------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/transaction_db` |
| `PUBSUB_EMULATOR_HOST`  | `localhost:8085`                                |
| `GCP_PROJECT_ID`        | `local-project`                                 |

### 3. Credenciais de banco

```
username: transaction_user
password: transaction_password
```

---

## Executando o Serviço

```bash
# Bash / Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# PowerShell (Windows) — aspas obrigatórias nos argumentos -D
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"

# Ou compilar e executar o JAR (funciona em qualquer shell)
./mvnw clean package "-DskipTests"
java -jar target/transaction-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

O serviço estará disponível em: `http://localhost:8080/transaction-service`

---

## API

Documentação completa: [`docs/architecture/api/transaction-service-api.md`](../docs/architecture/api/transaction-service-api.md)

### Endpoints

| Método | Endpoint                           | Descrição                   |
|--------|------------------------------------|-----------------------------|
| POST   | `/api/transactions`                | Criar transação             |
| GET    | `/api/transactions/{id}`           | Obter transação por ID      |
| GET    | `/api/transactions`                | Listar transações           |
| POST   | `/api/transactions/{id}/reverse`   | Estornar transação          |
| POST   | `/api/transactions/reports`        | Gerar relatório             |

### Exemplo rápido

```bash
# Criar uma transação de crédito
curl -X POST http://localhost:8080/transaction-service/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "CREDIT",
    "amount": "100.00",
    "date": "2026-04-14",
    "description": "Recebimento de taxa de serviço"
  }'
```

---

## Banco de Dados

### Schema (`schema.sql`)

```sql
CREATE TABLE IF NOT EXISTS transactions (
    id          VARCHAR(36)    PRIMARY KEY,   -- UUID
    type        VARCHAR(10)    NOT NULL,       -- CREDIT | DEBIT
    amount      DECIMAL(19,4)  NOT NULL,
    currency    VARCHAR(3)     NOT NULL,       -- BRL, USD, EUR, GBP
    date        DATE           NOT NULL,
    description VARCHAR(255)   NOT NULL,
    created_at  TIMESTAMP      NOT NULL,
    status      VARCHAR(20)    NOT NULL        -- PENDING | COMPLETED | FAILED
);

CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions (date);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions (type);
```

### Conexão (produção — Cloud SQL)

Em produção, as credenciais são recuperadas do **Secret Manager**. O serviço se conecta ao Cloud SQL via Cloud SQL Auth Proxy ou socket Unix no Cloud Run.

---

## Mensageria

O serviço publica eventos no **Google Cloud Pub/Sub**.

| Tópico               | Evento                    | Quando                            |
|----------------------|---------------------------|-----------------------------------|
| `transaction-events` | `transaction-created`     | Após criação bem-sucedida         |
| `transaction-events` | `transaction-reversed`    | Após estorno bem-sucedido         |

### Emulador local

O perfil `dev` usa o emulador do Pub/Sub (iniciado via Docker):

```yaml
spring.cloud.gcp.pubsub:
  emulator-host: localhost:8085
  project-id: local-project
```

---

## Observabilidade

### Actuator

| Endpoint              | URL                                                       |
|-----------------------|-----------------------------------------------------------|
| Health                | `GET /transaction-service/actuator/health`                |
| Info                  | `GET /transaction-service/actuator/info`                  |
| Metrics               | `GET /transaction-service/actuator/metrics`               |
| Prometheus            | `GET /transaction-service/actuator/prometheus`            |

Em produção, as métricas são coletadas pelo **Cloud Monitoring**.

### Métricas principais

| Métrica                         | Descrição                              |
|---------------------------------|----------------------------------------|
| `transaction.created.count`     | Total de transações criadas            |
| `transaction.reversed.count`    | Total de transações estornadas         |
| `transaction.processing.time`   | Tempo médio de processamento           |
| `http.server.requests.duration` | Latência por endpoint (p50/p90/p95/p99)|

### Logs

| Arquivo                          | Retenção | Conteúdo                        |
|----------------------------------|----------|---------------------------------|
| `logs/transaction-service.log`   | 30 dias  | Log geral da aplicação          |
| `logs/transactions.log`          | 90 dias  | Audit log de transações         |

Campos MDC propagados em todos os logs: `traceId`, `spanId`, `transactionId`.

### Resilience4j

| Configuração                         | Valor       |
|--------------------------------------|-------------|
| Circuit Breaker — failure threshold  | 50%         |
| Circuit Breaker — sliding window     | 10 chamadas |
| Circuit Breaker — wait (open state)  | 5 s         |
| Retry — max tentativas               | 3           |
| Retry — backoff inicial              | 1 s (x2)    |

---

## Testes

```bash
# Executar todos os testes
./mvnw test

# Com relatório de cobertura
./mvnw verify
```

O perfil `test` utiliza **H2 em memória** no modo de compatibilidade PostgreSQL. O Pub/Sub é desabilitado (`spring.cloud.gcp.pubsub.enabled=false`). O Resilience4j é configurado sem retry para evitar ruído nos testes.

---

## Estrutura do Projeto

```
transaction-service/
├── src/
│   ├── main/
│   │   ├── java/com/carrefourbank/transaction/
│   │   │   ├── TransactionServiceApplication.java
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── Transaction.java       # Entidade imutável
│   │   │   │   │   └── TransactionStatus.java # Enum: PENDING, COMPLETED, FAILED
│   │   │   │   ├── exception/
│   │   │   │   │   ├── TransactionAlreadyReversedException.java
│   │   │   │   │   └── PeriodClosedException.java
│   │   │   │   └── port/
│   │   │   │       └── TransactionRepository.java  # Interface de repositório
│   │   │   ├── application/
│   │   │   │   ├── dto/                       # CreateTransactionRequest, TransactionDTO, etc.
│   │   │   │   ├── mapper/
│   │   │   │   │   └── TransactionMapper.java
│   │   │   │   ├── port/
│   │   │   │   │   └── TransactionService.java    # Interface de serviço
│   │   │   │   └── service/
│   │   │   │       └── TransactionServiceImpl.java
│   │   │   └── infrastructure/
│   │   │       ├── adapter/
│   │   │       │   ├── persistence/
│   │   │       │   │   └── JdbcTransactionRepository.java
│   │   │       │   └── pubsub/
│   │   │       │       ├── PubSubTransactionEventPublisher.java
│   │   │       │       ├── NoOpTransactionEventPublisher.java
│   │   │       │       └── event/              # TransactionEventEnvelope, *EventData records
│   │   │       ├── config/
│   │   │       │   └── BannerConfig.java
│   │   │       ├── logging/
│   │   │       │   └── TransactionLogger.java
│   │   │       └── web/
│   │   │           ├── TransactionController.java
│   │   │           ├── GlobalExceptionHandler.java
│   │   │           └── ErrorResponse.java
│   │   └── resources/
│   │       ├── application.yaml          # Configuração base
│   │       ├── application-dev.yml       # Overrides para desenvolvimento local
│   │       ├── schema.sql                # DDL do banco de dados
│   │       ├── logback-spring.xml        # Configuração de logs
│   │       └── banner.txt                # Banner de inicialização
│   └── test/
│       ├── java/com/carrefourbank/transaction/
│       │   ├── domain/model/
│       │   │   └── TransactionTest.java
│       │   ├── application/service/
│       │   │   └── TransactionServiceImplTest.java
│       │   ├── infrastructure/adapter/persistence/
│       │   │   └── JdbcTransactionRepositoryTest.java
│       │   └── infrastructure/web/
│       │       └── TransactionControllerTest.java
│       └── resources/
│           ├── application-test.yml      # Configuração de testes (H2)
│           ├── schema-test.sql           # DDL para testes
│           └── logback-test.xml
├── Dockerfile
├── pom.xml
└── mvnw / mvnw.cmd
```

---

## Módulo Comum

Este serviço depende do módulo `common` que fornece:

- `TransactionType` — enum `CREDIT` / `DEBIT`
- `Currency` — enum `BRL`, `USD`, `EUR`, `GBP`
- `Money` — value object imutável para valores monetários
- `BusinessException`, `NotFoundException`, `ValidationException`
