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
    "date": "2026-04-02"
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

Para garantir idempotência no processamento de eventos:

- O Daily Balance Service mantém um registro de eventos processados com base no `eventId`
- Eventos duplicados são detectados e ignorados
- Operações de atualização de saldo são projetadas para serem idempotentes

## Ordenação de Eventos

A ordenação exata dos eventos não é garantida pelo Pub/Sub, portanto:

- O Daily Balance Service processa eventos da mesma data em lote
- O recálculo de saldo é feito após o processamento de todos os eventos de um lote
- Timestamps são utilizados para resolução de conflitos quando necessário

## Evolução de Esquema

Para permitir a evolução dos esquemas de eventos:

- Novos campos são sempre adicionados como opcionais
- Campos existentes nunca têm seu significado alterado
- Consumidores são projetados para ignorar campos desconhecidos

## Monitoramento

O fluxo de eventos é monitorado através de:

- Métricas de publicação e consumo no Pub/Sub
- Logs estruturados em cada serviço com correlação via `eventId`
- Alertas para filas de mensagens não processadas ou erros recorrentes