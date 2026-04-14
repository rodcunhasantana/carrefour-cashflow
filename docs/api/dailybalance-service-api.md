# API do Daily Balance Service

## Visão Geral

O Daily Balance Service é responsável pela consolidação e gerenciamento dos saldos diários no sistema Carrefour Cashflow. Esta API permite criar, consultar, fechar e reabrir períodos de saldo diário, fornecendo uma interface RESTful completa para o gerenciamento de saldos financeiros consolidados.

## Base URL

```
https://api.carrefourbank.com.br/cashflow/dailybalances
```

Para ambiente de desenvolvimento:

```
https://dev-api.carrefourbank.com.br/cashflow/dailybalances
```

## Autenticação

Todas as requisições devem incluir um token JWT válido no cabeçalho `Authorization` usando o esquema Bearer:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Os tokens são obtidos através do Google Cloud Identity Platform.

## Formatos

- Todas as requisições e respostas utilizam formato JSON
- Datas seguem o padrão ISO 8601 (`YYYY-MM-DD`)
- Timestamps seguem o padrão ISO 8601 com timezone (`YYYY-MM-DDThh:mm:ss.sssZ`)
- Valores monetários são representados como strings decimais com 2 casas decimais
- IDs são representados como UUIDs em formato string

---

## Endpoints

### Obter Saldo Diário por Data

Recupera o saldo diário de uma data específica.

**Endpoint:**

```
GET /api/dailybalances/{date}
```

**Parâmetros de Path:**

| Parâmetro | Tipo   | Descrição                             |
|-----------|--------|---------------------------------------|
| date      | string | Data do saldo no formato `YYYY-MM-DD` |

**Resposta (200 OK):**

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "date": "2026-04-10",
  "openingBalance": "1000.00",
  "totalCredits": "500.00",
  "totalDebits": "-300.00",
  "closingBalance": "1200.00",
  "status": "OPEN",
  "closedAt": null,
  "createdAt": "2026-04-10T08:00:00.000Z",
  "updatedAt": "2026-04-10T15:30:22.456Z"
}
```

**Possíveis Códigos de Resposta:**

| Código | Descrição                                                        |
|--------|------------------------------------------------------------------|
| 200    | OK - Saldo diário encontrado                                     |
| 401    | Unauthorized - Token inválido ou expirado                        |
| 403    | Forbidden - Sem permissão para acessar o recurso                 |
| 404    | Not Found - Saldo diário não encontrado para a data especificada |
| 500    | Internal Server Error - Erro interno no servidor                 |

---

### Listar Saldos Diários

Lista saldos diários com suporte a filtros por período e status.

**Endpoint:**

```
GET /api/dailybalances
```

**Parâmetros de Query:**

| Parâmetro  | Tipo    | Obrigatório | Descrição                                      |
|------------|---------|-------------|------------------------------------------------|
| startDate  | string  | Não         | Data inicial do período (formato `YYYY-MM-DD`) |
| endDate    | string  | Não         | Data final do período (formato `YYYY-MM-DD`)   |
| status     | string  | Não         | Filtro por status (`OPEN`, `CLOSED`)           |
| page       | integer | Não         | Número da página (padrão: 0)                   |
| size       | integer | Não         | Tamanho da página (padrão: 20, máximo: 100)    |

**Resposta (200 OK):**

```json
{
  "content": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "date": "2026-04-10",
      "openingBalance": "1000.00",
      "totalCredits": "500.00",
      "totalDebits": "-300.00",
      "closingBalance": "1200.00",
      "status": "OPEN",
      "closedAt": null,
      "createdAt": "2026-04-10T08:00:00.000Z",
      "updatedAt": "2026-04-10T15:30:22.456Z"
    },
    {
      "id": "223e4567-e89b-12d3-a456-426614174001",
      "date": "2026-04-09",
      "openingBalance": "800.00",
      "totalCredits": "450.00",
      "totalDebits": "-250.00",
      "closingBalance": "1000.00",
      "status": "CLOSED",
      "closedAt": "2026-04-09T23:59:59.999Z",
      "createdAt": "2026-04-09T08:00:00.000Z",
      "updatedAt": "2026-04-09T23:59:59.999Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 2,
    "totalPages": 1
  }
}
```

**Possíveis Códigos de Resposta:**

| Código | Descrição                                            |
|--------|------------------------------------------------------|
| 200    | OK - Saldos diários listados                         |
| 400    | Bad Request - Parâmetros inválidos                   |
| 401    | Unauthorized - Token inválido ou expirado            |
| 403    | Forbidden - Sem permissão para listar saldos diários |
| 500    | Internal Server Error - Erro interno no servidor     |

---

### Fechar Saldo Diário

Fecha o saldo diário de uma data específica, impedindo novas alterações.

**Endpoint:**

```
POST /api/dailybalances/{date}/close
```

**Parâmetros de Path:**

| Parâmetro | Tipo   | Descrição                                           |
|-----------|--------|-----------------------------------------------------|
| date      | string | Data do saldo a ser fechado no formato `YYYY-MM-DD` |

**Requisição:**

```json
{
  "notes": "Fechamento do dia 10/04/2026 concluído sem pendências"
}
```

**Campos da Requisição:**

| Campo | Tipo   | Obrigatório | Descrição                               |
|-------|--------|-------------|-----------------------------------------|
| notes | string | Não         | Observações sobre o fechamento do saldo |

**Resposta (200 OK):**

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "date": "2026-04-10",
  "openingBalance": "1000.00",
  "totalCredits": "500.00",
  "totalDebits": "-300.00",
  "closingBalance": "1200.00",
  "status": "CLOSED",
  "closedAt": "2026-04-11T10:15:30.123Z",
  "createdAt": "2026-04-10T08:00:00.000Z",
  "updatedAt": "2026-04-11T10:15:30.123Z",
  "notes": "Fechamento do dia 10/04/2026 concluído sem pendências"
}
```

**Possíveis Códigos de Resposta:**

| Código | Descrição                                                       |
|--------|-----------------------------------------------------------------|
| 200    | OK - Saldo diário fechado com sucesso                           |
| 400    | Bad Request - Validação falhou                                  |
| 401    | Unauthorized - Token inválido ou expirado                       |
| 403    | Forbidden - Sem permissão para fechar saldos                    |
| 404    | Not Found - Saldo diário não encontrado                         |
| 409    | Conflict - Saldo já está fechado                                |
| 422    | Unprocessable Entity - Não é possível fechar (pendências, etc.) |
| 500    | Internal Server Error - Erro interno no servidor                |

---

### Reabrir Saldo Diário

Reabre um saldo diário fechado anteriormente, permitindo atualizações.

**Endpoint:**

```
POST /api/dailybalances/{date}/reopen
```

**Parâmetros de Path:**

| Parâmetro | Tipo   | Descrição                                            |
|-----------|--------|------------------------------------------------------|
| date      | string | Data do saldo a ser reaberto no formato `YYYY-MM-DD` |

**Requisição:**

```json
{
  "reason": "Correção de lançamentos pendentes",
  "approvedBy": "João Silva"
}
```

**Campos da Requisição:**

| Campo      | Tipo   | Obrigatório | Descrição                                       |
|------------|--------|-------------|-------------------------------------------------|
| reason     | string | Sim         | Motivo da reabertura do saldo                   |
| approvedBy | string | Sim         | Nome do responsável que autorizou a reabertura  |

**Resposta (200 OK):**

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "date": "2026-04-10",
  "openingBalance": "1000.00",
  "totalCredits": "500.00",
  "totalDebits": "-300.00",
  "closingBalance": "1200.00",
  "status": "OPEN",
  "closedAt": null,
  "createdAt": "2026-04-10T08:00:00.000Z",
  "updatedAt": "2026-04-11T14:25:10.456Z",
  "reopenHistory": [
    {
      "reopenedAt": "2026-04-11T14:25:10.456Z",
      "reason": "Correção de lançamentos pendentes",
      "approvedBy": "João Silva"
    }
  ]
}
```

**Possíveis Códigos de Resposta:**

| Código | Descrição                                                                  |
|--------|----------------------------------------------------------------------------|
| 200    | OK - Saldo diário reaberto com sucesso                                     |
| 400    | Bad Request - Validação falhou                                             |
| 401    | Unauthorized - Token inválido ou expirado                                  |
| 403    | Forbidden - Sem permissão para reabrir saldos                              |
| 404    | Not Found - Saldo diário não encontrado                                    |
| 409    | Conflict - Saldo já está aberto                                            |
| 422    | Unprocessable Entity - Não é possível reabrir (período muito antigo, etc.) |
| 500    | Internal Server Error - Erro interno no servidor                           |

---

### Recalcular Saldo Diário

Força o recálculo do saldo diário a partir das transações existentes.

**Endpoint:**

```
POST /api/dailybalances/{date}/recalculate
```

**Parâmetros de Path:**

| Parâmetro | Tipo   | Descrição                                               |
|-----------|--------|---------------------------------------------------------|
| date      | string | Data do saldo a ser recalculado no formato `YYYY-MM-DD` |

**Resposta (200 OK):**

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "date": "2026-04-10",
  "openingBalance": "1000.00",
  "totalCredits": "550.00",
  "totalDebits": "-320.00",
  "closingBalance": "1230.00",
  "status": "OPEN",
  "closedAt": null,
  "createdAt": "2026-04-10T08:00:00.000Z",
  "updatedAt": "2026-04-11T16:45:22.789Z",
  "recalculationDetails": {
    "previousCredits": "500.00",
    "previousDebits": "-300.00",
    "previousClosingBalance": "1200.00",
    "transactionsProcessed": 25
  }
}
```

**Possíveis Códigos de Resposta:**

| Código | Descrição                                                  |
|--------|------------------------------------------------------------|
| 200    | OK - Saldo diário recalculado com sucesso                  |
| 401    | Unauthorized - Token inválido ou expirado                  |
| 403    | Forbidden - Sem permissão para recalcular saldos           |
| 404    | Not Found - Saldo diário não encontrado                    |
| 409    | Conflict - Não é possível recalcular saldo fechado         |
| 500    | Internal Server Error - Erro interno no servidor           |

---

### Exportar Saldos para ERP

Exporta dados de saldos diários fechados para o ERP corporativo.

**Endpoint:**

```
POST /api/dailybalances/export
```

**Requisição:**

```json
{
  "startDate": "2026-04-01",
  "endDate": "2026-04-30",
  "destination": "ERP"
}
```

**Campos da Requisição:**

| Campo       | Tipo   | Obrigatório | Descrição                                                          |
|-------------|--------|-------------|--------------------------------------------------------------------|
| startDate   | string | Sim         | Data inicial do período (formato `YYYY-MM-DD`)                     |
| endDate     | string | Sim         | Data final do período (formato `YYYY-MM-DD`)                       |
| destination | string | Não         | Destino da exportação. Valores: `ERP`, `STORAGE`. Padrão: `ERP`   |

**Resposta (202 Accepted):**

```json
{
  "exportId": "523e4567-e89b-12d3-a456-426614174004",
  "status": "PROCESSING",
  "startDate": "2026-04-01",
  "endDate": "2026-04-30",
  "destination": "ERP",
  "estimatedCompletionTime": "2026-04-11T10:05:00.000Z",
  "statusCheckUrl": "/api/dailybalances/exports/523e4567-e89b-12d3-a456-426614174004"
}
```

**Possíveis Códigos de Resposta:**

| Código | Descrição                                                |
|--------|----------------------------------------------------------|
| 202    | Accepted - Exportação iniciada                           |
| 400    | Bad Request - Validação falhou                           |
| 401    | Unauthorized - Token inválido ou expirado                |
| 403    | Forbidden - Sem permissão para exportar dados            |
| 422    | Unprocessable Entity - Período contém dias não fechados  |
| 500    | Internal Server Error - Erro interno no servidor         |

---

### Obter Histórico de Mudanças

Recupera o histórico de alterações de um saldo diário específico.

**Endpoint:**

```
GET /api/dailybalances/{date}/history
```

**Parâmetros de Path:**

| Parâmetro | Tipo   | Descrição                             |
|-----------|--------|---------------------------------------|
| date      | string | Data do saldo no formato `YYYY-MM-DD` |

**Resposta (200 OK):**

```json
{
  "dailyBalance": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "date": "2026-04-10"
  },
  "changes": [
    {
      "timestamp": "2026-04-10T08:00:00.000Z",
      "type": "CREATED",
      "user": "system",
      "details": "Initial creation with opening balance 1000.00"
    },
    {
      "timestamp": "2026-04-10T14:30:22.456Z",
      "type": "UPDATED",
      "user": "system",
      "details": "Updated by transaction processing: +150.00 credit"
    },
    {
      "timestamp": "2026-04-11T10:15:30.123Z",
      "type": "CLOSED",
      "user": "maria.silva",
      "details": "Closed with final balance 1200.00"
    },
    {
      "timestamp": "2026-04-11T14:25:10.456Z",
      "type": "REOPENED",
      "user": "joao.supervisor",
      "details": "Reopened for correction: Correção de lançamentos pendentes"
    },
    {
      "timestamp": "2026-04-11T16:45:22.789Z",
      "type": "RECALCULATED",
      "user": "maria.silva",
      "details": "Recalculated balance: +30.00 difference"
    }
  ]
}
```

**Possíveis Códigos de Resposta:**

| Código | Descrição                                          |
|--------|----------------------------------------------------|
| 200    | OK - Histórico recuperado com sucesso              |
| 401    | Unauthorized - Token inválido ou expirado          |
| 403    | Forbidden - Sem permissão para acessar o histórico |
| 404    | Not Found - Saldo diário não encontrado            |
| 500    | Internal Server Error - Erro interno no servidor   |

---

## Modelo de Dados

### DailyBalanceDTO

| Propriedade    | Tipo          | Descrição                                                 |
|----------------|---------------|-----------------------------------------------------------|
| id             | string (UUID) | Identificador único do saldo diário                       |
| date           | string        | Data do saldo (formato `YYYY-MM-DD`)                      |
| openingBalance | string        | Saldo de abertura (formato decimal com 2 casas)           |
| totalCredits   | string        | Total de créditos do dia (formato decimal com 2 casas)    |
| totalDebits    | string        | Total de débitos do dia (formato decimal com 2 casas)     |
| closingBalance | string        | Saldo de fechamento (formato decimal com 2 casas)         |
| status         | string        | Status atual: `OPEN`, `CLOSED`                            |
| closedAt       | string        | Data/hora de fechamento, se fechado (formato ISO 8601)    |
| createdAt      | string        | Timestamp de criação (formato ISO 8601)                   |
| updatedAt      | string        | Timestamp da última atualização (formato ISO 8601)        |

---

## Regras de Negócio

### Criação de Saldo Diário

- Um saldo diário é criado automaticamente quando a primeira transação do dia é processada
- O saldo de abertura é igual ao saldo de fechamento do dia anterior
- Se não houver dia anterior, o saldo de abertura é zero

### Fechamento de Período

- Apenas saldos diários no status `OPEN` podem ser fechados
- O fechamento impede o processamento de novas transações para o dia
- O saldo do dia seguinte usa o saldo de fechamento como saldo de abertura

### Reabertura de Saldo

- A reabertura requer uma justificativa e autorização específica
- Se o dia seguinte já estiver com transações, seu saldo de abertura também será recalculado
- Reabertura de dias muito antigos (>30 dias) exige aprovação de nível superior

### Recálculo de Saldo

- O recálculo só é permitido para saldos no status `OPEN`
- Inclui reprocessamento de todas as transações do dia para garantir consistência

### Consistência dos Valores

- O saldo de fechamento deve ser sempre: `abertura + créditos + débitos`
- Todos os valores monetários são armazenados com quatro casas decimais internamente
- Exibição padrão é com duas casas decimais

---

## Adaptações para Google Cloud

### Integração com Cloud Storage

Os relatórios e extratos são gerados e armazenados no Cloud Storage:

```
gs://carrefour-cashflow-reports/daily-balance/YYYY-MM-DD/report-name.pdf
```

Estes relatórios podem ser acessados via URLs assinadas com prazo de expiração.

### Processamento de Eventos

O serviço consome eventos de transações do Cloud Pub/Sub:

- **Tópico**: `transaction-events`
- **Assinatura**: `dailybalance-transaction-subscription`
- **Formato de mensagem**: JSON (conforme especificado na documentação do Transaction Service)

### Exportação para ERP

O serviço utiliza credenciais armazenadas no Secret Manager para autenticar com o ERP corporativo.

---

## Monitoramento

O serviço expõe métricas através do Cloud Monitoring:

- `dailybalance.closed.count` - Total de saldos diários fechados
- `dailybalance.reopened.count` - Total de saldos diários reabertos
- `dailybalance.recalculation.count` - Total de recálculos realizados
- `dailybalance.export.count` - Total de exportações realizadas
- `dailybalance.processing.time` - Tempo médio de processamento de operações
