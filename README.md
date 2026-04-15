# Carrefour Cash Flow System

Sistema de controle de fluxo de caixa para gerenciamento de lançamentos financeiros (débitos e créditos) e geração de saldos consolidados diários.

## Visão Geral

Dois microserviços independentes com database-per-service, comunicação assíncrona via Pub/Sub e arquitetura hexagonal:

- **Transaction Service** (porta 8080): Registra créditos/débitos, permite estorno, bloqueia lançamentos em períodos fechados
- **Daily Balance Service** (porta 8081): Consolida saldo diário via eventos, fecha/reabre períodos, auditoria de lançamentos por dia

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem / Framework | Java 21 / Spring Boot 3.2.4 |
| Persistência | JDBC puro + PostgreSQL (sem JPA/Hibernate) |
| Cache | Caffeine in-process (Daily Balance Service) — `@Cacheable`/`@CacheEvict` |
| Mensageria | Google Cloud Pub/Sub (emulador local via Docker) |
| Segurança | Spring Security — API Key via header `X-API-Key` |
| Tracing | Micrometer Tracing + Brave (`traceId`/`spanId` automáticos no MDC) |
| Resiliência | Resilience4j — Circuit Breaker e Retry |
| Documentação API | SpringDoc OpenAPI 2.3.0 — Swagger UI em runtime |
| Infraestrutura local | Docker Compose (compatível com Docker e Podman) |
| Produção | Cloud Run, Cloud SQL, Secret Manager, Cloud Pub/Sub |

## Estrutura do Projeto

```
carrefour-cashflow/
├── common/                  # Tipos e exceções compartilhadas (Money, ValidationException...)
├── transaction-service/     # Microserviço de lançamentos financeiros
├── dailybalance-service/    # Microserviço de saldo consolidado diário
├── infrastructure/
│   └── docker/
│       └── docker-compose.yml
└── docs/                    # ADRs, diagramas C4, modelo de domínio
```

## Serviços no Docker Compose

| Container | Imagem | Porta | Descrição |
|---|---|---|---|
| `transaction-db` | postgres:15-alpine | 5432 | Banco de dados do transaction-service |
| `dailybalance-db` | postgres:15-alpine | 5433 | Banco de dados do dailybalance-service |
| `pubsub-emulator` | google-cloud-cli:emulators | 8085 | Emulador do Google Cloud Pub/Sub |
| `pubsub-setup` | curlimages/curl | — | Cria tópicos e subscriptions (roda uma vez e sai) |
| `transaction-service` | build local | 8080 | Microserviço de lançamentos |
| `dailybalance-service` | build local | 8081 | Microserviço de saldo consolidado |

### Tópicos e Subscriptions criados automaticamente

| Tópico | Subscription | DLQ |
|---|---|---|
| `transaction-events` | `dailybalance-transaction-subscription` | `transaction-events-dlq` |
| `period-events` | `transaction-period-subscription` | `period-events-dlq` |

## Começando

### Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker Desktop **ou** Podman Desktop

### Executar com Docker

```bash
cd infrastructure/docker
docker-compose up --build -d

# Acompanhar status
docker-compose ps
```

### Executar com Podman (Windows)

```powershell
cd infrastructure/docker
podman compose up --build -d

# Acompanhar status
podman compose ps

# Ver logs de um serviço
podman logs transaction-service
podman logs dailybalance-service
```

> Os serviços ficam prontos quando o status for `Up` e o healthcheck do actuator responder `UP`.

### Executar localmente (sem containers para os serviços)

Sobe apenas a infraestrutura (bancos + Pub/Sub) e roda os serviços direto pelo Maven:

```bash
# Docker
cd infrastructure/docker
docker-compose up -d transaction-db dailybalance-db pubsub-emulator pubsub-setup

# Podman
podman compose up -d transaction-db dailybalance-db pubsub-emulator pubsub-setup
```

Em terminais separados:

```bash
# Terminal 1
cd transaction-service
mvn spring-boot:run

# Terminal 2
cd dailybalance-service
mvn spring-boot:run
```

Neste modo a chave API padrão é `cashflow-dev-key`.

## Autenticação

Todos os endpoints `/api/**` exigem o header `X-API-Key`:

| Ambiente | Chave |
|---|---|
| Local sem Docker | `cashflow-dev-key` |
| Docker / Podman Compose | `cashflow-local-key` |
| Produção (Cloud Run) | Variável `APP_API_KEY` via Secret Manager |

Endpoints públicos (sem autenticação): `/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**`

**curl:**
```bash
curl -H "X-API-Key: cashflow-local-key" \
  http://localhost:8080/transaction-service/api/transactions
```

**PowerShell:**
```powershell
# GET
Invoke-RestMethod "http://localhost:8080/transaction-service/api/transactions" `
  -Headers @{ "X-API-Key" = "cashflow-local-key" }

# POST — usar UTF-8 explícito para evitar erro com caracteres especiais
$body = [System.Text.Encoding]::UTF8.GetBytes((@{
    type = "CREDIT"; amount = 500.00; currency = "BRL"
    date = "2025-01-20"; description = "Receita"
} | ConvertTo-Json))
Invoke-RestMethod "http://localhost:8080/transaction-service/api/transactions" `
  -Method POST -Headers @{ "X-API-Key" = "cashflow-local-key" } `
  -ContentType "application/json; charset=utf-8" -Body $body
```

## Documentação da API (Swagger UI)

Com os serviços rodando, acesse no browser:

| Serviço | Swagger UI | OpenAPI JSON |
|---|---|---|
| transaction-service | http://localhost:8080/transaction-service/swagger-ui.html | http://localhost:8080/transaction-service/v3/api-docs |
| dailybalance-service | http://localhost:8081/dailybalance-service/swagger-ui.html | http://localhost:8081/dailybalance-service/v3/api-docs |

Para autenticar no Swagger UI: clique em **Authorize** (cadeado) e informe a chave API.

## Observabilidade

### Logs estruturados

Formato: `[appName, traceId, spanId, domainId]`

```
[transaction-service,69dfb2043,3d8cef2a,1edba28a-f9ad-...]  INFO  Transaction created: CREDIT R$ 500
[dailybalance-service,69dfb2043,3d8cef2b,d4e5f678-...]       INFO  Balance updated: 2025-01-20
```

- `traceId` / `spanId`: gerados automaticamente pelo Micrometer Tracing (Brave) em cada requisição HTTP
- `transactionId` / `balanceId`: campo de domínio populado pelos Loggers específicos de cada serviço

### Health e métricas

```bash
# Health (sem autenticação)
curl http://localhost:8080/transaction-service/actuator/health
curl http://localhost:8081/dailybalance-service/actuator/health

# Métricas (sem autenticação)
curl http://localhost:8080/transaction-service/actuator/metrics
```

## Executando os testes

```bash
# Todos os módulos (121 testes)
mvn clean install

# Por serviço
mvn test -pl transaction-service
mvn test -pl dailybalance-service
```

**Cobertura da pirâmide de testes:**
- Unitários de domínio — JUnit puro, sem Spring, sem mocks de framework
- Unitários de serviço — Mockito
- Integração JDBC — H2 in-memory com schema idêntico ao PostgreSQL
- Web — MockMvc standalone (sem Spring Security aplicado)
- Cache — `@SpringBootTest` + `@MockBean` + Caffeine ativo (Daily Balance Service)
- Smoke test de contexto — `@SpringBootTest` verifica inicialização do contexto completo

## Documentação Arquitetural

| Documento | Descrição |
|---|---|
| [ADR-001](docs/architecture/decisions/001-microservices.md) | Adoção de microsserviços |
| [ADR-002](docs/architecture/decisions/002-async-communication.md) | Comunicação assíncrona via Pub/Sub |
| [ADR-003](docs/architecture/decisions/003-database-strategy.md) | Database-per-service com PostgreSQL |
| [ADR-004](docs/architecture/decisions/004-testing-strategy.md) | Estratégia de testes |
| [ADR-005](docs/architecture/decisions/005-cloud-provider-selection.md) | GCP como cloud provider |
| [ADR-006](docs/architecture/decisions/006-security.md) | Autenticação por API Key |
| [Visão Geral da Arquitetura](docs/architecture/overview.md) | Padrões, stack e decisões |
| [C4 — Contêineres](docs/architecture/c4/containers.md) | Diagrama de contêineres |
| [C4 — Componentes](docs/architecture/c4/components.md) | Componentes internos de cada serviço |
| [Eventos de Domínio](docs/domain/domain-events.md) | Contratos de eventos Pub/Sub |
| [Matriz de Rastreabilidade](docs/traceability.md) | FR/NFR → ADR → implementação → testes |
| [Estimativa de Custo GCP](docs/architecture/cost-estimation.md) | Custos por componente em produção |
| [Roadmap de Evoluções](docs/roadmap.md) | CI/CD, Flyway, TestContainers, relatórios, IA e mais |
| [Resultados de Performance (k6)](docs/performance/results.md) | Baseline local — 20 VUs, todos os thresholds aprovados |
