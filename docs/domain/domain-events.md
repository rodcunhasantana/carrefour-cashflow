# Eventos de Domínio - Carrefour Cashflow

Este documento descreve os eventos de domínio usados para comunicação assíncrona entre os serviços do sistema Carrefour Cashflow.

## Visão Geral

O sistema utiliza uma arquitetura baseada em eventos para comunicação entre o Transaction Service e o Daily Balance Service. Os eventos são publicados no Google Cloud Pub/Sub e consumidos pelos serviços interessados.

## Estrutura Geral dos Eventos

Todos os eventos seguem uma estrutura comum:

```json
{
  "eventId": "5f9c5b9b-1b1a-4b1a-9b1a-1b1a9b1a1b1a",
  "eventType": "transaction-created",
  "timestamp": "2026-04-01T14:30:15.123Z",
  "producer": "transaction-service",
  "data": {
    // Conteúdo específico do evento
  }
}
```
Onde:

- **eventId**: UUID único do evento
- **eventType**: Tipo do evento (transaction-created, transaction-reversed, etc.)
- **timestamp**: Data e hora de geração do evento
- **producer**: Serviço que gerou o evento
- **data**: Payload específico do tipo de evento

## Eventos Específicos

### period-closed

Publicado pelo Daily Balance Service quando um período é fechado via `POST /api/dailybalances/{date}/close`.

**Tópico**: `period-events`
**Subscription consumida por**: `transaction-period-subscription` (Transaction Service)

**Payload**:

```json
{
  "eventId": "9f1a2b3c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
  "eventType": "period-closed",
  "timestamp": "2026-04-14T23:59:00.000Z",
  "producer": "dailybalance-service",
  "data": {
    "balanceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "date": "2026-04-14"
  }
}
```

**Efeito**: O Transaction Service persiste a data fechada na tabela `closed_periods`. A partir desse momento, qualquer tentativa de criar um lançamento para essa data retorna HTTP 422 (`PeriodClosedException`).

---

### transaction-created

Publicado quando uma nova transação é criada no Transaction Service.

**Tópico**: `transaction-events`

**Payload**:

```json
{
  "eventId": "5f9c5b9b-1b1a-4b1a-9b1a-1b1a9b1a1b1a",
  "eventType": "transaction-created",
  "timestamp": "2026-04-01T14:30:15.123Z",
  "producer": "transaction-service",
  "data": {
    "transactionId": "0b45e7db-7fb1-4fb0-a4f1-8120af6bb9e7",
    "type": "CREDIT",
    "amount": 150.75,
    "currency": "BRL",
    "date": "2026-04-01",
    "description": "Depósito em dinheiro"
  }
}
```
**Consumido por**: Daily Balance Service

**Efeito**: O Daily Balance Service atualiza o saldo diário correspondente à data da transação:

- Se for crédito: incrementa o total de créditos
- Se for débito: incrementa o total de débitos
- Recalcula o saldo de fechamento

### transaction-reversed

Publicado quando uma transação é estornada no Transaction Service.

**Tópico**: `transaction-events`

**Payload**:

```json
{
  "eventId": "6a7b8c9d-0e1f-2a3b-4c5d-6e7f8a9b0c1d",
  "eventType": "transaction-reversed",
  "timestamp": "2026-04-02T09:15:30.789Z",
  "producer": "transaction-service",
  "data": {
    "originalTransactionId": "0b45e7db-7fb1-4fb0-a4f1-8120af6bb9e7",
    "reversalTransactionId": "c2d3e4f5-6g78-9h10-i11j-12k13l14m15n",
    "reason": "Lançamento incorreto",
    "date": "2026-04-02",
    "amount": 150.75,
    "currency": "BRL",
    "type": "CREDIT"
  }
}
```
**Consumido por**: Daily Balance Service

**Efeito**: O Daily Balance Service processa a transação de estorno como uma nova transação normal, atualizando o saldo do dia em que o estorno foi registrado.

## Garantias de Entrega e Idempotência

### Garantia de Entrega

- O Google Cloud Pub/Sub armazena as mensagens até que sejam processadas com sucesso
- O Transaction Service implementa retry em caso de falha na publicação
- O Daily Balance Service confirma o recebimento (ack) apenas após processamento completo

### Idempotência

Implementada no Daily Balance Service via tabela `processed_events`:

- Antes de processar qualquer evento, o consumer tenta inserir o `eventId` na tabela `processed_events (event_id PK, processed_at)`
- Se a inserção falhar por chave duplicada (`DuplicateKeyException`), o evento já foi processado → mensagem é descartada silenciosamente e confirmada (`ack`)
- Se a inserção suceder → o saldo é atualizado e a mensagem é confirmada
- Em caso de erro no processamento → `ack` não é enviado, o Pub/Sub reentrega a mensagem

Essa abordagem garante que o mesmo `eventId` nunca acumule saldo duas vezes, mesmo sob reentrega de mensagens (at-least-once delivery).

## Tópicos e Subscriptions

| Tópico | Subscription | Producer | Consumer |
|---|---|---|---|
| `transaction-events` | `dailybalance-transaction-subscription` | transaction-service | dailybalance-service |
| `period-events` | `transaction-period-subscription` | dailybalance-service | transaction-service |

> **Nota sobre `period-reopened`:** O evento `period-reopened` **não está implementado** no Daily Balance Service. Ao reabrir um período via `POST /api/dailybalances/{date}/reopen`, o saldo é atualizado para `OPEN` no banco, mas nenhum evento é publicado no tópico `period-events`. O consumer `PeriodEventConsumer` no Transaction Service está preparado para processar esse evento, mas enquanto ele não for emitido, a reabertura de período não remove o registro de `closed_periods` automaticamente. Esta lacuna está mapeada no [roadmap](../../roadmap.md).

Ambos os tópicos e subscriptions são criados automaticamente pelo container `pubsub-setup` no `docker-compose.yml` ao subir o ambiente local.

## Ordenação de Eventos

A ordenação exata dos eventos não é garantida pelo Pub/Sub, portanto:

- O Daily Balance Service acumula créditos e débitos conforme os eventos chegam
- O `closingBalance` é recalculado a cada evento: `openingBalance + totalCredits + totalDebits`
- Timestamps são utilizados para correlação e auditoria

## Evolução de Esquema

Para permitir a evolução dos esquemas de eventos:

- Novos campos são sempre adicionados como opcionais
- Campos existentes nunca têm seu significado alterado
- Consumidores são projetados para ignorar campos desconhecidos

## Auditoria: rastreabilidade transação → saldo

Cada evento processado com sucesso gera um registro imutável na tabela `daily_balance_transactions` do `dailybalance-service`:

| Campo | Descrição |
|---|---|
| `id` | UUID do registro de auditoria |
| `balance_id` | UUID do saldo diário ao qual o lançamento foi aplicado |
| `transaction_id` | ID da transação original no `transaction-service` |
| `event_id` | `eventId` do envelope Pub/Sub (garante unicidade) |
| `transaction_type` | `CREDIT` ou `DEBIT` |
| `amount` | Valor aplicado |
| `currency` | Moeda (BRL) |
| `applied_at` | Timestamp de aplicação |

**Endpoint de consulta:**
```
GET /api/dailybalances/{date}/transactions
```
Retorna todos os lançamentos que compõem o saldo de uma data, ordenados cronologicamente por `applied_at`.

## Monitoramento

O fluxo de eventos é monitorado através de:

- Métricas de publicação e consumo no Pub/Sub
- Logs estruturados em cada serviço com correlação via `eventId`
- Alertas para filas de mensagens não processadas ou erros recorrentes