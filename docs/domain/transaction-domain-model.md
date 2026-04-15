# Modelo de Domínio - Transaction Service

Este documento descreve o núcleo do **Transaction Service**, detalhando as entidades, objetos de valor, regras de negócio e tabelas de suporte que garantem a integridade dos lançamentos financeiros.

---

## 1. Entidades Principais

### `Transaction`
Representa um lançamento financeiro (débito ou crédito). É o **Aggregate Root** deste contexto.

| Atributo | Tipo | Descrição |
| :--- | :--- | :--- |
| `id` | `UUID` | Identificador único universal da transação. |
| `type` | `TransactionType` | Classificação: `CREDIT` ou `DEBIT`. |
| `amount` | `Money` | Valor monetário (encapsulado em VO). |
| `date` | `LocalDate` | Data em que o lançamento ocorreu. |
| `description` | `String` | Texto explicativo do lançamento. |
| `status` | `TransactionStatus` | Estado atual (`PENDING`, `COMPLETED`, `CANCELED`, `REVERSED`). |
| `createdAt` | `LocalDateTime` | Timestamp de auditoria da criação. |

#### **Regras de Negócio de Transaction**
- **Sinalização por Tipo:** Transações de **Crédito** possuem valor positivo; transações de **Débito** possuem valor negativo.
- **Imutabilidade:** Uma vez criada, uma transação não pode ser editada. Erros são corrigidos via **Estorno (Reversal)**, que gera um novo registro compensatório com tipo e valor invertidos.
- **Bloqueio por Período Fechado:** Não é possível criar lançamentos para uma data cujo período esteja fechado. Tentativas retornam `HTTP 422 Unprocessable Entity` com `PeriodClosedException`.
- **Integridade:** A descrição é campo obrigatório e não pode ser nula ou vazia.

---

## 2. Value Objects (VOs)

### `Money`
Objeto de valor que encapsula a lógica financeira, evitando erros de arredondamento com `double` ou `float`.
- **Atributos:** `amount` (`BigDecimal`) e `currency` (Enum).
- **Comportamentos:** Soma de valores, inversão de sinal (`negate`) e validação de sinal (positivo/negativo).

### Enums
- **`TransactionType`**: `CREDIT`, `DEBIT`.
- **`TransactionStatus`**: `PENDING`, `COMPLETED`, `CANCELED`, `REVERSED`.
- **`Currency`**: Moedas suportadas (`BRL`, `USD`, `EUR`).

---

## 3. Serviços e Repositórios

### `TransactionService` (Application Service)
Orquestra a lógica de criação, consulta e estorno:
- Verifica se a data está em período fechado antes de criar o lançamento (`ClosedPeriodRepository`).
- Executa a fábrica de criação de transações.
- Coordena o processo de estorno: inverte o tipo e nega o valor da transação original.
- Publica `TransactionCreatedEvent` ou `TransactionReversedEvent` via `TransactionEventPublisher`.

### `TransactionRepository`
Interface de abstração para persistência (PostgreSQL):
- `save(Transaction)`: Persiste o estado atual.
- `findById(UUID)`: Busca por ID.
- `findAll(...)`: Recupera transações com paginação e filtro opcional por tipo.
- `count(...)`: Conta registros com filtro opcional por tipo.
- `existsReversalFor(UUID)`: Verifica se já existe um estorno para a transação informada.

### `ClosedPeriodRepository`
Interface para verificação e gestão de períodos fechados:
- `existsByDate(LocalDate)`: Retorna `true` se a data está com período fechado.
- `save(LocalDate)`: Registra uma data como fechada (chamado ao consumir evento `period-closed`).
- `deleteByDate(LocalDate)`: Remove o bloqueio (chamado ao consumir evento `period-reopened`).

---

## 4. Tabelas do Banco de Dados

### `transactions`
| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | UUID (PK) | Identificador único |
| `type` | VARCHAR | `CREDIT` ou `DEBIT` |
| `amount` | NUMERIC | Valor (positivo para crédito, negativo para débito) |
| `currency` | VARCHAR | `BRL`, `USD`, `EUR` |
| `date` | DATE | Data do lançamento |
| `description` | TEXT | Descrição do lançamento |
| `status` | VARCHAR | `PENDING`, `COMPLETED`, `CANCELED`, `REVERSED` |
| `is_reversal` | BOOLEAN | `true` se este registro é um estorno |
| `original_transaction_id` | VARCHAR(36) (FK) | ID da transação original (apenas para estornos) |
| `created_at` | TIMESTAMP | Data/hora de criação |

### `closed_periods`
| Coluna | Tipo | Descrição |
|---|---|---|
| `date` | DATE (PK) | Data com período fechado |
| `closed_at` | TIMESTAMP | Data/hora em que o período foi fechado |

Populada automaticamente ao consumir eventos `period-closed` do tópico `period-events`. Deletada ao consumir eventos `period-reopened`.

---

## 5. Eventos de Domínio

### Publicados (tópico `transaction-events`)

- **`TransactionCreatedEvent`**: ID, tipo, valor, data e status da nova transação.
- **`TransactionReversedEvent`**: ID da transação original, ID do estorno e motivo.

### Consumidos (tópico `period-events`, subscription `transaction-period-subscription`)

- **`PeriodClosedEvent`**: Data do período fechado → insere em `closed_periods`.
- **`PeriodReopenedEvent`**: Data do período reaberto → remove de `closed_periods`.

> **Nota:** O evento `PeriodReopenedEvent` ainda **não está publicado** pelo Daily Balance Service. O consumer `PeriodEventConsumer` está implementado e pronto para processar esse evento, mas enquanto ele não for publicado, a reabertura de período no Daily Balance Service não remove automaticamente o bloqueio de `closed_periods`.

---

## 6. Exceções de Domínio

| Exceção | HTTP | Quando é lançada |
|---|---|---|
| `PeriodClosedException` | 422 | Tentativa de criar lançamento em data com período fechado |
| `NotFoundException` | 404 | Transação não encontrada por ID |
| `ValidationException` | 400 | Dados inválidos na requisição |

---

## 7. Diagrama de Classes Simplificado

```text
+----------------+       +----------------+       +-----------------+
|  Transaction   |       |     Money      |       | TransactionType |
+----------------+       +----------------+       +-----------------+
| - id: UUID     |       | - amount: Big  |       | CREDIT          |
| - type: Type   |<>---->| - currency: Cur|       | DEBIT           |
| - amount: Money|       +----------------+       +-----------------+
| - date: Date   |
| - description  |       +-----------------+      +----------------+
| - createdAt    |       |TransactionStatus|      | ClosedPeriod   |
| - status       |       +-----------------+      +----------------+
| - isReversal   |       | PENDING         |      | - date: Date   |
+----------------+       | COMPLETED       |      | - closedAt     |
| + validate()   |       | CANCELED        |      +----------------+
| + createRev...()|      | REVERSED        |
+----------------+       +-----------------+
        ^
        |
+----------------------+
| Reversal Transaction |
| (type invertido,     |
|  amount negado)      |
+----------------------+
```

---

## Conclusão

O modelo de domínio do Transaction Service foi projetado para ser resiliente a falhas humanas através do uso de imutabilidade e para ser altamente auditável. O mecanismo de `closed_periods` garante consistência com o Daily Balance Service de forma desacoplada, via eventos assíncronos. A utilização de Value Objects para valores monetários e eventos de domínio para comunicação entre serviços garantem precisão financeira e escalabilidade técnica.
