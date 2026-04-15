# Matriz de Rastreabilidade

Conecta cada **requisito funcional (FR) e não-funcional (NFR)** à **decisão arquitetural (ADR)** que o endereça, à **implementação** que o entrega e aos **testes** que o verificam.

---

## Legenda

| Prefixo | Significado |
|---|---|
| FR | Functional Requirement |
| NFR | Non-Functional Requirement |
| ADR | Architecture Decision Record |
| TS | transaction-service |
| DBS | dailybalance-service |

---

## Requisitos Funcionais

### FR-01 — Registrar lançamento financeiro (crédito ou débito)

| Campo | Valor |
|---|---|
| **Descrição** | O sistema deve permitir criar lançamentos de crédito e débito com valor, data e descrição. Créditos têm valor positivo; débitos, negativo. |
| **ADR** | [ADR-001](architecture/decisions/001-microservices.md) (Transaction Service como bounded context), [ADR-003](architecture/decisions/003-database-strategy.md) (PostgreSQL para ACID) |
| **Implementação** | `TS: Transaction.create()` · `TransactionServiceImpl.create()` · `POST /api/transactions` · tabela `transactions` |
| **Testes** | `TransactionTest.create_credit_with_positive_amount_succeeds` · `create_debit_with_negative_amount_succeeds` · `TransactionServiceImplTest.create_saves_and_publishes_event` · `TransactionControllerTest.POST_transactions_returns_201` · `JdbcTransactionRepositoryTest.save_and_findById_round_trips` |

---

### FR-02 — Validar regras de negócio no lançamento

| Campo | Valor |
|---|---|
| **Descrição** | Crédito com valor negativo, débito com valor positivo, descrição vazia ou tipo nulo devem ser rejeitados com HTTP 400. |
| **ADR** | [ADR-001](architecture/decisions/001-microservices.md) (domínio isolado no Transaction Service) |
| **Implementação** | `Transaction.validate()` · `ValidationException` (módulo common) · `GlobalExceptionHandler` → HTTP 400 |
| **Testes** | `TransactionTest.create_credit_with_negative_amount_throws_validation_exception` · `create_debit_with_positive_amount_throws_validation_exception` · `create_with_blank_description_throws_validation_exception` · `create_with_null_type_throws_validation_exception` · `TransactionControllerTest.POST_transactions_returns_400_on_missing_fields` |

---

### FR-03 — Estornar lançamento existente

| Campo | Valor |
|---|---|
| **Descrição** | Um lançamento pode ser estornado, gerando um registro compensatório com tipo e valor invertidos. Cada lançamento admite apenas um estorno. |
| **ADR** | [ADR-001](architecture/decisions/001-microservices.md) · [ADR-002](architecture/decisions/002-async-communication.md) (TransactionReversedEvent) |
| **Implementação** | `Transaction.createReversal()` · `TransactionServiceImpl.reverse()` · `POST /api/transactions/{id}/reverse` · `existsReversalFor()` no repositório |
| **Testes** | `TransactionTest.createReversal_inverts_amount_and_sets_reversal_fields` · `createReversal_with_blank_reason_throws_validation_exception` · `TransactionServiceImplTest.reverse_creates_reversal_and_marks_original_reversed` · `reverse_throws_already_reversed_when_reversal_exists` · `reverse_throws_not_found_when_transaction_missing` · `TransactionControllerTest.POST_reverse_returns_200` · `POST_reverse_returns_409_when_already_reversed` · `JdbcTransactionRepositoryTest.existsReversalFor_returns_false_when_no_reversal` · `existsReversalFor_returns_true_after_reversal_saved` |

---

### FR-04 — Consultar lançamentos (listagem e detalhe)

| Campo | Valor |
|---|---|
| **Descrição** | Os lançamentos podem ser consultados individualmente por ID ou em listagem paginada com filtro por tipo. |
| **ADR** | [ADR-001](architecture/decisions/001-microservices.md) |
| **Implementação** | `TransactionServiceImpl.findById()` · `findAll()` · `GET /api/transactions/{id}` · `GET /api/transactions` |
| **Testes** | `TransactionServiceImplTest.findById_returns_dto_when_found` · `findById_throws_not_found_when_missing` · `findAll_returns_page_response` · `TransactionControllerTest.GET_transaction_by_id_returns_200` · `GET_transaction_by_id_returns_404_when_not_found` · `GET_transactions_returns_200_with_page` · `JdbcTransactionRepositoryTest.findAll_without_filters_returns_all` · `findAll_filters_by_type` · `count_returns_total_matching_records` |

---

### FR-05 — Bloquear lançamento em período fechado

| Campo | Valor |
|---|---|
| **Descrição** | Se o Daily Balance Service fechar um período, o Transaction Service deve rejeitar novos lançamentos para aquela data com HTTP 422. |
| **ADR** | [ADR-002](architecture/decisions/002-async-communication.md) (evento `period-closed` → `closed_periods`) |
| **Implementação** | `ClosedPeriodRepository.existsByDate()` · `PeriodClosedException` · `PeriodEventConsumer` (consome `period-closed`) · tabela `closed_periods` |
| **Testes** | `TransactionServiceImplTest.create_throws_period_closed_when_date_is_closed` |

---

### FR-06 — Consolidar saldo diário via evento

| Campo | Valor |
|---|---|
| **Descrição** | Ao receber um evento de transação, o Daily Balance Service deve atualizar créditos e débitos do saldo do dia correspondente, criando-o se não existir. |
| **ADR** | [ADR-002](architecture/decisions/002-async-communication.md) · [ADR-001](architecture/decisions/001-microservices.md) (DBS como bounded context) |
| **Implementação** | `TransactionEventConsumer` · `DailyBalanceServiceImpl.applyTransaction()` · `DailyBalance.addCredit()` / `addDebit()` · `DailyBalanceRepository.save()` |
| **Testes** | `DailyBalanceTest.withAddedCredit_increasesTotalCreditsAndRecalculatesClosing` · `withAddedDebit_increasesTotalDebitsAndRecalculatesClosing` · `closingBalance_formula_openingPlusCreditsMinusDebits` · `DailyBalanceServiceImplTest.applyTransaction_credit_createsNewBalanceWhenNotExists` · `applyTransaction_debit_appliesOnExistingBalance` · `JdbcDailyBalanceRepositoryTest.save_and_findByDate_roundTrip` |

---

### FR-07 — Garantir idempotência no processamento de eventos

| Campo | Valor |
|---|---|
| **Descrição** | O mesmo evento entregue mais de uma vez (at-least-once delivery do Pub/Sub) não deve causar duplicidade no saldo. |
| **ADR** | [ADR-002](architecture/decisions/002-async-communication.md) (seção Idempotência) |
| **Implementação** | `ProcessedEventRepository.existsByEventId()` · tabela `processed_events` · `DuplicateKeyException` como guard |
| **Testes** | `DailyBalanceServiceImplTest.applyTransaction_duplicate_eventId_skipsProcessing` |

---

### FR-08 — Fechar período contábil

| Campo | Valor |
|---|---|
| **Descrição** | O Daily Balance Service deve permitir fechar um período (data), impedindo novos eventos de alterar o saldo e publicando evento `period-closed`. |
| **ADR** | [ADR-002](architecture/decisions/002-async-communication.md) · [ADR-003](architecture/decisions/003-database-strategy.md) |
| **Implementação** | `DailyBalance.close()` · `DailyBalanceServiceImpl.closeBalance()` · `PeriodEventPublisher` · `POST /api/balances/{date}/close` |
| **Testes** | `DailyBalanceTest.close_setsStatusClosedAndClosedAt` · `close_throwsBalanceAlreadyClosedException_whenAlreadyClosed` · `withAddedCredit_throwsBalanceAlreadyClosedException_whenClosed` · `DailyBalanceServiceImplTest.closeBalance_closesAndSaves` · `closeBalance_publishesPeriodClosedEvent` · `closeBalance_propagatesBalanceAlreadyClosedException` · `DailyBalanceControllerTest.closeBalance_returns200_withClosedStatus` · `closeBalance_returns409_whenAlreadyClosed` |

---

### FR-09 — Reabrir período contábil

| Campo | Valor |
|---|---|
| **Descrição** | Um período fechado pode ser reaberto, voltando a aceitar eventos. Publica evento `period-reopened` para que o Transaction Service remova o bloqueio. |
| **ADR** | [ADR-002](architecture/decisions/002-async-communication.md) |
| **Implementação** | `DailyBalance.reopen()` · `DailyBalanceServiceImpl.reopenBalance()` · `POST /api/balances/{date}/reopen` |
| **Testes** | `DailyBalanceTest.reopen_setsStatusOpenAndClearsClosedAt` · `reopen_throwsBalanceAlreadyOpenException_whenAlreadyOpen` |

---

### FR-10 — Consultar saldo diário

| Campo | Valor |
|---|---|
| **Descrição** | O saldo consolidado de uma data pode ser consultado individualmente ou em listagem paginada com filtro por status. |
| **ADR** | [ADR-001](architecture/decisions/001-microservices.md) · [ADR-003](architecture/decisions/003-database-strategy.md) |
| **Implementação** | `DailyBalanceServiceImpl.findByDate()` · `findAll()` · `GET /api/balances/{date}` · `GET /api/balances` |
| **Testes** | `DailyBalanceTest.create_initializesWithZeroCreditsDebitsAndOpenStatus` · `DailyBalanceServiceImplTest.findByDate_returnsDTO_whenFound` · `findByDate_throwsNotFoundException_whenNotFound` · `findAll_returnsPageResponse` · `DailyBalanceControllerTest.findByDate_returns200_withDTO` · `findByDate_returns404_whenNotFound` · `findAll_returns200_withPageResponse` · `JdbcDailyBalanceRepositoryTest.findAll_withoutFilters_returnsAll` · `findAll_filtersByStatus` · `count_returnsTotalMatchingRecords` |

---

### FR-11 — Auditar lançamentos por data

| Campo | Valor |
|---|---|
| **Descrição** | Para cada data, deve ser possível listar todos os lançamentos que foram processados naquele saldo, com tipo, valor e timestamp. |
| **ADR** | [ADR-003](architecture/decisions/003-database-strategy.md) (tabela `daily_balance_transactions`) |
| **Implementação** | `DailyBalanceTransactionRepository` · tabela `daily_balance_transactions` · `DailyBalanceServiceImpl.findTransactionsByDate()` · `GET /api/balances/{date}/transactions` |
| **Testes** | `DailyBalanceServiceImplTest.applyTransaction_savesAuditEntry` · `findTransactionsByDate_returnsAuditEntries` · `DailyBalanceControllerTest.findTransactionsByDate_returns200_withList` · `findTransactionsByDate_returns404_whenBalanceNotFound` · `JdbcDailyBalanceTransactionRepositoryTest.save_and_findByBalanceId_roundTrip` · `findByBalanceId_returnsMultipleEntriesOrderedByAppliedAt` · `findByBalanceId_returnsEmpty_forUnknownBalance` |

---

### FR-12 — Inicializar saldo de abertura com fechamento anterior

| Campo | Valor |
|---|---|
| **Descrição** | Ao criar um saldo para uma nova data, o saldo de abertura deve ser o saldo de fechamento do dia anterior mais recente disponível. |
| **ADR** | [ADR-003](architecture/decisions/003-database-strategy.md) |
| **Implementação** | `DailyBalanceRepository.findMostRecentClosedBefore()` · `DailyBalanceServiceImpl.applyTransaction()` (cria com saldo de abertura do fechamento anterior) |
| **Testes** | `JdbcDailyBalanceRepositoryTest.findMostRecentClosedBefore_returnsCorrectRecord` · `findMostRecentClosedBefore_returnsEmpty_whenNoClosed` |

---

## Requisitos Não-Funcionais

### NFR-01 — Autenticação de todos os endpoints de negócio

| Campo | Valor |
|---|---|
| **Descrição** | Todos os endpoints `/api/**` devem exigir autenticação via `X-API-Key`. Actuator, Swagger e OpenAPI docs são públicos. |
| **ADR** | [ADR-006](architecture/decisions/006-security.md) |
| **Implementação** | `ApiKeyAuthFilter extends OncePerRequestFilter` · `shouldNotFilter()` com `contains()` · `app.security.api-key` (env `APP_API_KEY` em produção via Secret Manager) |
| **Testes** | Testes de controller usam MockMvc standalone (sem filtro aplicado — comportamento documentado no ADR-006). Validação manual via Swagger UI ou curl com/sem `X-API-Key`. |

---

### NFR-02 — Rastreabilidade distribuída (traceId / spanId)

| Campo | Valor |
|---|---|
| **Descrição** | Cada requisição HTTP deve gerar `traceId` e `spanId` propagados em todos os logs, permitindo correlação entre serviços. |
| **ADR** | [ADR-001](architecture/decisions/001-microservices.md) (observabilidade distribuída como implicação) |
| **Implementação** | `micrometer-tracing-bridge-brave` em ambos os serviços · `management.tracing.sampling.probability: 1.0` · padrão Logback `[%X{traceId:-},%X{spanId:-}]` |
| **Testes** | Verificação manual nos logs: formato `[appName,traceId,spanId,domainId]` com campos preenchidos a cada requisição. |

---

### NFR-03 — Resiliência com Circuit Breaker e Retry

| Campo | Valor |
|---|---|
| **Descrição** | Chamadas a dependências externas (banco, Pub/Sub) devem ser protegidas com circuit breaker e retry automático. |
| **ADR** | [ADR-001](architecture/decisions/001-microservices.md) (isolamento de falhas) |
| **Implementação** | `resilience4j-spring-boot3` em ambos os serviços · configuração em `application.yaml` (circuitbreaker, retry) |
| **Testes** | `application-test.yml`: `maxAttempts: 1` e `registerHealthIndicator: false` para testes rápidos sem efeito colateral. |

---

### NFR-04 — Dead Letter Queue para mensagens não processadas

| Campo | Valor |
|---|---|
| **Descrição** | Mensagens não processadas após 5 tentativas devem ser encaminhadas para DLQ e registradas em log de alerta. |
| **ADR** | [ADR-002](architecture/decisions/002-async-communication.md) |
| **Implementação** | Topics `transaction-events-dlq` e `period-events-dlq` · `maxDeliveryAttempts: 5` · `DLQMonitor` em ambos os serviços |
| **Testes** | Sem testes automatizados — validação manual via emulador Pub/Sub. |

---

### NFR-05 — Disponibilidade de documentação da API em runtime

| Campo | Valor |
|---|---|
| **Descrição** | A documentação de todos os endpoints deve estar acessível via Swagger UI sem necessidade de autenticação. |
| **ADR** | [ADR-006](architecture/decisions/006-security.md) (endpoints públicos) |
| **Implementação** | `springdoc-openapi-starter-webmvc-ui 2.3.0` · `/{service}/swagger-ui.html` e `/{service}/v3/api-docs` públicos |
| **Testes** | Validação manual: acesso sem `X-API-Key` retorna Swagger UI funcional. |

---

### NFR-06 — Isolamento de banco de dados por serviço

| Campo | Valor |
|---|---|
| **Descrição** | Cada serviço deve ter seu próprio banco de dados, sem acesso direto ao banco do outro serviço. |
| **ADR** | [ADR-003](architecture/decisions/003-database-strategy.md) |
| **Implementação** | `transaction-db` (porta 5432) exclusivo do Transaction Service · `dailybalance-db` (porta 5433) exclusivo do Daily Balance Service · sem datasource compartilhado |
| **Testes** | `JdbcTransactionRepositoryTest` e `JdbcDailyBalanceRepositoryTest` usam bancos H2 separados por serviço — esquemas independentes. |

---

### NFR-07 — Precisão financeira sem erros de ponto flutuante

| Campo | Valor |
|---|---|
| **Descrição** | Todos os valores monetários devem usar `BigDecimal` (não `double` ou `float`) e `NUMERIC(19,4)` no banco. |
| **ADR** | [ADR-003](architecture/decisions/003-database-strategy.md) |
| **Implementação** | `Money` value object com `BigDecimal` · colunas `NUMERIC(19,4)` nos schemas SQL |
| **Testes** | `DailyBalanceTest.recalculate_recomputesClosingBalance` · `closingBalance_formula_openingPlusCreditsMinusDebits` |

---

## Cobertura por Camada

| Camada de Teste | Classes | Métodos | FRs cobertos |
|---|---|---|---|
| Domínio (JUnit puro) | 2 | 23 | FR-01, FR-02, FR-03, FR-06, FR-07, FR-08, FR-09, NFR-07 |
| Serviço (Mockito) | 2 | 21 | FR-01, FR-02, FR-03, FR-04, FR-05, FR-06, FR-07, FR-08, FR-09, FR-10, FR-11, FR-12 |
| Web (MockMvc) | 2 | 16 | FR-01, FR-02, FR-03, FR-04, FR-08, FR-10, FR-11 |
| Persistência JDBC (H2) | 3 | 21 | FR-01, FR-03, FR-04, FR-06, FR-10, FR-11, FR-12, NFR-06 |
| **Total** | **9** | **81** | |

> **Nota:** O total de 81 métodos considera a soma das camadas; o número real de testes únicos é 65 (alguns testes cobrem múltiplos FRs).

---

## Gaps Identificados

| Gap | FR/NFR | Impacto | Observação |
|---|---|---|---|
| Sem teste de integração do `PeriodEventConsumer` | FR-05 | Médio | Validado apenas via teste unitário de serviço com mock |
| Sem teste E2E Transaction → Pub/Sub → DBS | FR-06, FR-07 | Médio | Fluxo completo só validado manualmente no Docker |
| Sem teste automatizado de DLQ | NFR-04 | Baixo | DLQ validada manualmente via emulador |
| Sem teste de carga (k6) | NFR-03 | Médio | Limites do circuit breaker não validados sob carga |
| Sem teste do `ApiKeyAuthFilter` aplicado | NFR-01 | Baixo | Filtro validado manualmente; controllers usam MockMvc standalone sem Security |
