# Daily Balance Service

Microserviço responsável pela consolidação e gerenciamento dos saldos diários no sistema **Carrefour Cashflow**. Consome eventos de transações do Google Cloud Pub/Sub, calcula saldos diários, gerencia fechamentos e reabertura de períodos contábeis e disponibiliza auditoria de lançamentos por data.

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
| Artefato        | `dailybalance-service`       |
| Versão          | `1.0.0-SNAPSHOT`             |
| Porta           | `8081`                       |
| Context path    | `/dailybalance-service`      |
| Perfil padrão   | `dev`                        |

### Responsabilidades

- Consumir eventos de transações (`transaction-created`, `transaction-reversed`) do Cloud Pub/Sub
- Calcular e atualizar saldos diários em tempo real
- Gerenciar fechamento e reabertura de períodos contábeis
- Auditar lançamentos processados por data (`daily_balance_transactions`)
- Garantir idempotência no processamento de eventos duplicados (`processed_events`)

---

## Stack

| Tecnologia              | Versão        |
|-------------------------|---------------|
| Java                    | 21            |
| Spring Boot             | 3.2.4         |
| Spring JDBC             | (BOM)         |
| Spring Cache + Caffeine | (BOM)         |
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
- Transaction Service em execução (produtor dos eventos Pub/Sub)

---

## Configuração Local

### 1. Iniciar dependências via Docker

```bash
docker-compose -f ../infrastructure/docker/docker-compose.yml up -d
```

Isso sobe:
- **PostgreSQL** na porta `5433` — banco `dailybalance_db`
- **Pub/Sub Emulator** na porta `8085`

### 2. Variáveis de ambiente (dev)

O perfil `dev` já aponta para as dependências locais via `application-dev.yml`. Nenhuma variável adicional é necessária para rodar localmente.

| Variável                | Dev (padrão)                                       |
|-------------------------|----------------------------------------------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/dailybalance_db` |
| `PUBSUB_EMULATOR_HOST`  | `localhost:8085`                                   |
| `GCP_PROJECT_ID`        | `local-project`                                    |

### 3. Credenciais de banco

```
username: dailybalance_user
password: dailybalance_password
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
java -jar target/dailybalance-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

O serviço estará disponível em: `http://localhost:8081/dailybalance-service`

---

## API

Documentação completa: [`docs/api/dailybalance-service-api.md`](../docs/api/dailybalance-service-api.md)

### Endpoints

| Método | Endpoint                                   | Descrição                       |
|--------|--------------------------------------------|---------------------------------|
| GET    | `/api/dailybalances/{date}`                | Obter saldo por data            |
| GET    | `/api/dailybalances`                       | Listar saldos (com filtros)     |
| POST   | `/api/dailybalances/{date}/close`          | Fechar período                  |
| POST   | `/api/dailybalances/{date}/reopen`         | Reabrir período                 |
| POST   | `/api/dailybalances/{date}/recalculate`    | Recalcular saldo                |
| GET    | `/api/dailybalances/{date}/transactions`   | Listar lançamentos auditados    |

### Exemplo rápido

```bash
# Consultar saldo do dia
curl -H "X-API-Key: cashflow-local-key" \
  http://localhost:8081/dailybalance-service/api/dailybalances/2026-04-14

# Fechar período
curl -X POST http://localhost:8081/dailybalance-service/api/dailybalances/2026-04-14/close \
  -H "X-API-Key: cashflow-local-key" \
  -H "Content-Type: application/json" \
  -d '{"closedBy": "admin"}'
```

---

## Banco de Dados

### Schema (`schema.sql`)

```sql
CREATE TABLE IF NOT EXISTS daily_balances (
    id              VARCHAR(36)    PRIMARY KEY,   -- UUID
    date            DATE           NOT NULL UNIQUE,
    opening_balance DECIMAL(19,4)  NOT NULL,
    total_credits   DECIMAL(19,4)  NOT NULL,
    total_debits    DECIMAL(19,4)  NOT NULL,
    closing_balance DECIMAL(19,4)  NOT NULL,
    status          VARCHAR(20)    NOT NULL,       -- OPEN | CLOSED
    closed_at       TIMESTAMP,
    created_at      TIMESTAMP      NOT NULL,
    updated_at      TIMESTAMP      NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_dailybalance_date   ON daily_balances (date);
CREATE INDEX IF NOT EXISTS idx_dailybalance_status ON daily_balances (status);
```

> Invariante financeira: `closing_balance = opening_balance + total_credits + total_debits`

### Conexão (produção — Cloud SQL)

Em produção, as credenciais são recuperadas do **Secret Manager**. O serviço se conecta ao Cloud SQL via Cloud SQL Auth Proxy ou socket Unix no Cloud Run.

---

## Mensageria

O serviço **consome** eventos do tópico `transaction-events` no **Google Cloud Pub/Sub**.

| Assinatura                               | Tópico               | Eventos consumidos                              |
|------------------------------------------|----------------------|-------------------------------------------------|
| `dailybalance-transaction-subscription`  | `transaction-events` | `transaction-created`, `transaction-reversed`   |

### Comportamento ao receber eventos

| Evento                   | Ação                                                      |
|--------------------------|-----------------------------------------------------------|
| `transaction-created`    | Cria ou atualiza saldo do dia; recalcula closing balance  |
| `transaction-reversed`   | Aplica estorno no saldo; recalcula closing balance        |

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

| Endpoint              | URL                                                        |
|-----------------------|------------------------------------------------------------|
| Health                | `GET /dailybalance-service/actuator/health`                |
| Info                  | `GET /dailybalance-service/actuator/info`                  |
| Metrics               | `GET /dailybalance-service/actuator/metrics`               |
| Prometheus            | `GET /dailybalance-service/actuator/prometheus`            |

Em produção, as métricas são coletadas pelo **Cloud Monitoring**.

### Métricas principais

| Métrica                            | Descrição                               |
|------------------------------------|-----------------------------------------|
| `dailybalance.closed.count`        | Total de saldos fechados                |
| `dailybalance.reopened.count`      | Total de saldos reabertos               |
| `dailybalance.recalculation.count` | Total de recálculos realizados          |
| `dailybalance.processing.time`     | Tempo médio de processamento            |
| `cache.gets` (`dailyBalances`)     | Hits e misses do cache Caffeine         |
| `http.server.requests.duration`    | Latência por endpoint (p50/p90/p95/p99) |

### Logs

| Arquivo                             | Retenção | Conteúdo                             |
|-------------------------------------|----------|--------------------------------------|
| `logs/dailybalance-service.log`     | 30 dias  | Log geral da aplicação               |
| `logs/dailybalances.log`            | 90 dias  | Audit log de operações de saldo      |

Campos MDC propagados em todos os logs: `traceId`, `spanId`, `balanceId`.

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
dailybalance-service/
├── src/
│   ├── main/
│   │   ├── java/com/carrefourbank/dailybalance/
│   │   │   ├── DailybalanceServiceApplication.java
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── DailyBalance.java      # Entidade imutável
│   │   │   │   │   └── BalanceStatus.java     # Enum: OPEN, CLOSED
│   │   │   │   ├── exception/
│   │   │   │   │   ├── BalanceAlreadyClosedException.java
│   │   │   │   │   └── BalanceAlreadyOpenException.java
│   │   │   │   └── port/
│   │   │   │       └── DailyBalanceRepository.java  # Interface de repositório
│   │   │   ├── application/
│   │   │   │   ├── dto/                       # DailyBalanceDTO, DailyBalancePageResponse, etc.
│   │   │   │   ├── mapper/
│   │   │   │   │   └── DailyBalanceMapper.java
│   │   │   │   ├── port/
│   │   │   │   │   └── DailyBalanceService.java    # Interface de serviço
│   │   │   │   └── service/
│   │   │   │       └── DailyBalanceServiceImpl.java
│   │   │   └── infrastructure/
│   │   │       ├── adapter/
│   │   │       │   ├── persistence/
│   │   │       │   │   ├── JdbcDailyBalanceRepository.java
│   │   │       │   │   ├── JdbcDailyBalanceTransactionRepository.java
│   │   │       │   │   └── JdbcProcessedEventRepository.java
│   │   │       │   └── pubsub/
│   │   │       │       ├── TransactionEventConsumer.java
│   │   │       │       └── event/              # TransactionEventEnvelope, *EventData records
│   │   │       ├── config/
│   │   │       │   ├── BannerConfig.java
│   │   │       │   └── CacheConfig.java        # @EnableCaching — Caffeine in-process
│   │   │       ├── logging/
│   │   │       │   └── DailyBalanceLogger.java
│   │   │       └── web/
│   │   │           ├── DailyBalanceController.java
│   │   │           ├── GlobalExceptionHandler.java
│   │   │           └── ErrorResponse.java
│   │   └── resources/
│   │       ├── application.yaml          # Configuração base
│   │       ├── application-dev.yml       # Overrides para desenvolvimento local
│   │       ├── schema.sql                # DDL do banco de dados
│   │       ├── logback-spring.xml        # Configuração de logs
│   │       └── banner.txt                # Banner de inicialização
│   └── test/
│       ├── java/com/carrefourbank/dailybalance/
│       │   ├── domain/model/
│       │   │   └── DailyBalanceTest.java
│       │   ├── application/service/
│       │   │   ├── DailyBalanceServiceImplTest.java
│       │   │   └── DailyBalanceCacheTest.java   # @SpringBootTest — valida @Cacheable/@CacheEvict
│       │   ├── infrastructure/adapter/persistence/
│       │   │   ├── JdbcDailyBalanceRepositoryTest.java
│       │   │   └── JdbcDailyBalanceTransactionRepositoryTest.java
│       │   └── infrastructure/web/
│       │       └── DailyBalanceControllerTest.java
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

---

## Dependência com o Transaction Service

Este serviço **não se comunica diretamente** com o Transaction Service via HTTP. A integração é exclusivamente assíncrona:

```
Transaction Service → Pub/Sub (transaction-events) → Daily Balance Service
```

Para desenvolvimento local, o Transaction Service deve estar em execução e publicando eventos no emulador Pub/Sub (`localhost:8085`).
