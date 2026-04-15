# Modelo de Domínio - Daily Balance Service

Este documento descreve o modelo de domínio do **Daily Balance Service**, detalhando as principais entidades, *value objects*, regras de negócio e tabelas de suporte para a consolidação de saldos diários.

---

## 1. Entidades Principais

### `DailyBalance`
Representa o saldo consolidado de um dia específico. É o agregado principal responsável por manter a integridade financeira diária.

| Atributo | Tipo | Descrição |
| :--- | :--- | :--- |
| `id` | `UUID` | Identificador único do saldo diário. |
| `date` | `LocalDate` | Data de referência do saldo. |
| `openingBalance` | `Money` | Saldo inicial (vindo do fechamento do dia anterior). |
| `totalCredits` | `Money` | Soma de todos os créditos processados no dia. |
| `totalDebits` | `Money` | Soma de todos os débitos processados no dia (valor negativo). |
| `closingBalance` | `Money` | Saldo final (`openingBalance + totalCredits + totalDebits`). |
| `status` | `BalanceStatus` | Estado do dia (`OPEN` ou `CLOSED`). |
| `balancedAt` | `LocalDateTime` | Data/hora da última atualização. |

#### **Regras de Negócio**
- **Cálculo de Saldo:** O saldo de fechamento é sempre recalculado a cada nova transação para garantir precisão.
- **Continuidade:** O saldo de abertura de hoje deve ser obrigatoriamente o saldo de fechamento de ontem.
- **Unicidade:** O sistema garante que exista apenas um registro de `DailyBalance` por data.
- **Fechamento:** Um saldo com status `CLOSED` não aceita novas transações. O fechamento pode ser revertido via `reopen()`.
- **Idempotência:** Eventos já processados são ignorados via tabela `processed_events` (eventId como chave única).

#### **Comportamentos**
- `addCredit(amount)`: Incrementa `totalCredits` e recalcula `closingBalance`.
- `addDebit(amount)`: Incrementa `totalDebits` (preservando o sinal negativo) e recalcula `closingBalance`.
- `close()`: Altera o status para `CLOSED`. Lança `BalanceAlreadyClosedException` se já fechado.
- `reopen()`: Altera o status para `OPEN`. Lança `BalanceAlreadyOpenException` se já aberto.

---

## 2. Value Objects

- **`Money`**: Encapsula o valor numérico (`BigDecimal`) e a moeda (`Currency`).
- **`BalanceStatus` (Enum)**:
  - `OPEN`: Aceita novos eventos de transação.
  - `CLOSED`: Saldo consolidado e bloqueado para alterações. Pode ser reaberto via `reopen()`.

---

## 3. Serviços e Repositórios

### `DailyBalanceService` (Application Service)
Responsável pela orquestração do domínio:
- Busca ou criação de saldos sob demanda.
- Aplicação de créditos e débitos vindos de eventos.
- Fechamento e reabertura de períodos (publicando eventos `period-events`).
- Consulta de lançamentos auditados por data.

### `DailyBalanceRepository`
- `findByDate(LocalDate)`: Localiza o saldo de um dia específico.
- `findMostRecentClosedBefore(LocalDate)`: Recupera o último saldo fechado para inicializar o saldo de abertura de um novo dia.
- `save(DailyBalance)`: Persiste o estado atual.

### `ProcessedEventRepository`
- `existsByEventId(String)`: Verifica se o evento já foi processado (idempotência).
- `save(String eventId)`: Registra o eventId como processado.

---

## 4. Tabelas do Banco de Dados

### `daily_balances`
| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | UUID (PK) | Identificador único |
| `date` | DATE (UNIQUE) | Data de referência |
| `opening_balance` | NUMERIC | Saldo de abertura |
| `total_credits` | NUMERIC | Total de créditos do dia |
| `total_debits` | NUMERIC | Total de débitos do dia |
| `closing_balance` | NUMERIC | Saldo de fechamento |
| `currency` | VARCHAR | Moeda (`BRL`) |
| `status` | VARCHAR | `OPEN` ou `CLOSED` |
| `balanced_at` | TIMESTAMP | Data/hora da última atualização |

### `daily_balance_transactions`
| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | UUID (PK) | Identificador único do registro |
| `daily_balance_id` | UUID (FK) | Referência ao saldo do dia |
| `transaction_id` | UUID | ID da transação original |
| `type` | VARCHAR | `CREDIT` ou `DEBIT` |
| `amount` | NUMERIC | Valor do lançamento |
| `currency` | VARCHAR | Moeda |
| `description` | TEXT | Descrição do lançamento |
| `transaction_date` | DATE | Data do lançamento |
| `processed_at` | TIMESTAMP | Data/hora em que foi processado |

Populada ao consumir cada evento de transação. Consultada via `GET /api/balances/{date}/transactions`.

### `processed_events`
| Coluna | Tipo | Descrição |
|---|---|---|
| `event_id` | VARCHAR (PK) | ID único do evento Pub/Sub |
| `processed_at` | TIMESTAMP | Data/hora do processamento |

Garante que mensagens entregues mais de uma vez pelo Pub/Sub (at-least-once delivery) não causem duplicidade no saldo.

---

## 5. Fluxo de Eventos

### Consumidos (tópico `transaction-events`, subscription `dailybalance-transaction-subscription`)

**Ao receber `TransactionCreatedEvent`:**
1. Verifica se `eventId` já existe em `processed_events` (se sim, descarta).
2. Identifica a data da transação.
3. Recupera ou cria o `DailyBalance` para aquela data.
4. Aplica `addCredit()` ou `addDebit()` conforme o tipo.
5. Registra o lançamento em `daily_balance_transactions`.
6. Salva o estado atualizado.
7. Registra o `eventId` em `processed_events`.

**Ao receber `TransactionReversedEvent`:**
1. Verifica idempotência via `processed_events`.
2. Aplica o valor invertido (o estorno já chega com o sinal correto da transação compensatória).

### Publicados (tópico `period-events`)

- **`PeriodClosedEvent`**: Data do período fechado → Transaction Service insere em `closed_periods`.
- **`PeriodReopenedEvent`**: Data do período reaberto → Transaction Service remove de `closed_periods`.

---

## 6. Exceções de Domínio

| Exceção | HTTP | Quando é lançada |
|---|---|---|
| `BalanceNotFoundException` | 404 | Saldo não encontrado para a data |
| `BalanceAlreadyClosedException` | 422 | Tentativa de fechar um saldo já fechado |
| `BalanceAlreadyOpenException` | 422 | Tentativa de reabrir um saldo já aberto |

---

## 7. Diagrama de Classes Simplificado

```text
+----------------+       +----------------+       +-----------------+
|  DailyBalance  |       |     Money      |       |  BalanceStatus  |
+----------------+       +----------------+       +-----------------+
| - id: UUID     |       | - amount: Big  |       | OPEN            |
| - date: Date   |       | - currency: Cur|       | CLOSED          |
| - openingBal   |<>---->|                |       +-----------------+
| - totalCredits |       +----------------+
| - totalDebits  |
| - closingBal   |       +-------------------------------+
| - balancedAt   |       |  DailyBalanceTransaction      |
| - status       |       +-------------------------------+
+----------------+       | - id: UUID                    |
| + addCredit()  |       | - dailyBalanceId: UUID (FK)   |
| + addDebit()   |<----->| - transactionId: UUID         |
| + recalculate()|       | - type, amount, description   |
| + close()      |       | - processedAt: Timestamp      |
| + reopen()     |       +-------------------------------+
+----------------+

+-------------------+
|  ProcessedEvent   |
+-------------------+
| - eventId: String |
| - processedAt     |
+-------------------+
```

---

## Conclusão

O modelo de domínio do Daily Balance Service foi projetado para ser eficiente em leitura e resiliente a falhas de mensageria. A idempotência via `processed_events` protege contra a semântica at-least-once do Pub/Sub. O mecanismo de `daily_balance_transactions` permite auditoria completa dos lançamentos por dia sem necessidade de consulta ao Transaction Service. O `reopen()` garante que períodos fechados por engano possam ser corrigidos sem perda de dados.
