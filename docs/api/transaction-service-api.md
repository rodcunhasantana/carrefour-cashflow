# API do Transaction Service

## Visão Geral

O Transaction Service é responsável pelo gerenciamento completo do ciclo de vida das transações financeiras no sistema Carrefour Cashflow. Esta API permite criar, consultar, listar e estornar transações, fornecendo uma interface RESTful completa para manipulação de registros financeiros.

## Base URL

```
https://api.carrefourbank.com.br/cashflow/transactions
```

Para ambiente de desenvolvimento:

```
https://dev-api.carrefourbank.com.br/cashflow/transactions
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

### Criar Transação

Cria uma nova transação financeira no sistema.

**Endpoint:**

```
POST /api/transactions
```

**Requisição:**

```json
{
  "type": "CREDIT",
  "amount": "100.00",
  "date": "2026-04-10",
  "description": "Recebimento de taxa de serviço"
}
```

**Campos da Requisição:**

| Campo       | Tipo   | Obrigatório | Descrição                                                                  |
|-------------|--------|-------------|----------------------------------------------------------------------------|
| type        | string | Sim         | Tipo da transação. Valores: `CREDIT` (entrada) ou `DEBIT` (saída)          |
| amount      | string | Sim         | Valor da transação. Deve ser positivo para `CREDIT` e negativo para `DEBIT`|
| date        | string | Sim         | Data da transação no formato `YYYY-MM-DD`                                  |
| description | string | Sim         | Descrição da transação (3-100 caracteres)                                  |

**Resposta (201 Created):**

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "type": "CREDIT",
  "amount": "100.00",
  "currency": "BRL",
  "date": "2026-04-10",
  "description": "Recebimento de taxa de serviço",
  "createdAt": "2026-04-10T14:30:45.123Z",
  "status": "COMPLETED"
}
```

**Possíveis Códigos de Resposta:**

| Código | Descrição                                                         |
|--------|-------------------------------------------------------------------|
| 201    | Created - Transação criada com sucesso                            |
| 400    | Bad Request - Validação falhou                                    |
| 401    | Unauthorized - Token inválido ou expirado                         |
| 403    | Forbidden - Sem permissão para criar transações                   |
| 422    | Unprocessable Entity - Dados válidos, mas violação de regra de negócio |
| 500    | Internal Server Error - Erro interno no servidor                  |

---

### Obter Transação por ID

Recupera os detalhes de uma transação específica pelo seu identificador.

**Endpoint:**

```
GET /api/transactions/{id}
```

**Parâmetros de Path:**

| Parâmetro | Tipo          | Descrição                        |
|-----------|---------------|----------------------------------|
| id        | string (UUID) | Identificador único da transação |

**Resposta (200 OK):**

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "type": "CREDIT",
  "amount": "100.00",
  "currency": "BRL",
  "date": "2026-04-10",
  "description": "Recebimento de taxa de serviço",
  "createdAt": "2026-04-10T14:30:45.123Z",
  "status": "COMPLETED"
}
```

**Possíveis Códigos de Resposta:**

| Código | Descrição                                             |
|--------|-------------------------------------------------------|
| 200    | OK - Transação encontrada                             |
| 401    | Unauthorized - Token inválido ou expirado             |
| 403    | Forbidden - Sem permissão para acessar essa transação |
| 404    | Not Found - Transação não encontrada                  |
| 500    | Internal Server Error - Erro interno no servidor      |

---

### Listar Transações

Lista transações com suporte a filtros por período e tipo.

**Endpoint:**

```
GET /api/transactions
```

**Parâmetros de Query:**

| Parâmetro  | Tipo    | Obrigatório | Descrição                                       |
|------------|---------|-------------|-------------------------------------------------|
| startDate  | string  | Não         | Data inicial do período (formato `YYYY-MM-DD`)  |
| endDate    | string  | Não         | Data final do período (formato `YYYY-MM-DD`)    |
| type       | string  | Não         | Filtro por tipo (`CREDIT` ou `DEBIT`)           |
| page       | integer | Não         | Número da página (padrão: 0)                    |
| size       | integer | Não         | Tamanho da página (padrão: 20, máximo: 100)     |

**Resposta (200 OK):**

```json
{
  "content": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "type": "CREDIT",
      "amount": "100.00",
      "currency": "BRL",
      "date": "2026-04-10",
      "description": "Recebimento de taxa de serviço",
      "createdAt": "2026-04-10T14:30:45.123Z",
      "status": "COMPLETED"
    },
    {
      "id": "223e4567-e89b-12d3-a456-426614174001",
      "type": "DEBIT",
      "amount": "-50.00",
      "currency": "BRL",
      "date": "2026-04-09",
      "description": "Pagamento de fornecedor",
      "createdAt": "2026-04-09T10:15:30.456Z",
      "status": "COMPLETED"
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

| Código | Descrição                                             |
|--------|-------------------------------------------------------|
| 200    | OK - Transações listadas                              |
| 400    | Bad Request - Parâmetros inválidos                    |
| 401    | Unauthorized - Token inválido ou expirado             |
| 403    | Forbidden - Sem permissão para listar transações      |
| 500    | Internal Server Error - Erro interno no servidor      |

---

### Estornar Transação

Cria uma transação de estorno para uma transação existente.

**Endpoint:**

```
POST /api/transactions/{id}/reverse
```

**Parâmetros de Path:**

| Parâmetro | Tipo          | Descrição                                        |
|-----------|---------------|--------------------------------------------------|
| id        | string (UUID) | Identificador único da transação a ser estornada |

**Requisição:**

```json
{
  "reason": "Lançamento incorreto"
}
```

**Campos da Requisição:**

| Campo  | Tipo   | Obrigatório | Descrição                           |
|--------|--------|-------------|-------------------------------------|
| reason | string | Sim         | Motivo do estorno (3-100 caracteres)|

**Resposta (200 OK):**

```json
{
  "id": "323e4567-e89b-12d3-a456-426614174002",
  "type": "CREDIT",
  "amount": "-100.00",
  "currency": "BRL",
  "date": "2026-04-11",
  "description": "Reversal: Recebimento de taxa de serviço - Reason: Lançamento incorreto",
  "createdAt": "2026-04-11T09:45:22.789Z",
  "status": "COMPLETED"
}
```

**Possíveis Códigos de Resposta:**

| Código | Descrição                                                       |
|--------|-----------------------------------------------------------------|
| 200    | OK - Transação estornada com sucesso                            |
| 400    | Bad Request - Validação falhou                                  |
| 401    | Unauthorized - Token inválido ou expirado                       |
| 403    | Forbidden - Sem permissão para estornar transações              |
| 404    | Not Found - Transação original não encontrada                   |
| 409    | Conflict - A transação já foi estornada                         |
| 422    | Unprocessable Entity - Não é possível estornar (período fechado, etc.) |
| 500    | Internal Server Error - Erro interno no servidor                |

---

### Gerar Relatório de Transações

Gera um relatório de transações e armazena no Google Cloud Storage.

**Endpoint:**

```
POST /api/transactions/reports
```

**Requisição:**

```json
{
  "reportType": "TRANSACTION_SUMMARY",
  "startDate": "2026-04-01",
  "endDate": "2026-04-30",
  "format": "PDF"
}
```

**Campos da Requisição:**

| Campo      | Tipo   | Obrigatório | Descrição                                                                     |
|------------|--------|-------------|-------------------------------------------------------------------------------|
| reportType | string | Sim         | Tipo de relatório. Valores: `TRANSACTION_SUMMARY`, `TRANSACTION_DETAIL`       |
| startDate  | string | Sim         | Data inicial do período (formato `YYYY-MM-DD`)                                |
| endDate    | string | Sim         | Data final do período (formato `YYYY-MM-DD`)                                  |
| format     | string | Não         | Formato do relatório. Valores: `PDF`, `CSV`, `XLSX`. Padrão: `PDF`            |

**Resposta (202 Accepted):**

```json
{
  "reportId": "423e4567-e89b-12d3-a456-426614174003",
  "status": "PROCESSING",
  "reportType": "TRANSACTION_SUMMARY",
  "startDate": "2026-04-01",
  "endDate": "2026-04-30",
  "format": "PDF",
  "estimatedCompletionTime": "2026-04-11T10:00:00.000Z",
  "statusCheckUrl": "/api/transactions/reports/423e4567-e89b-12d3-a456-426614174003"
}
```

**Possíveis Códigos de Resposta:**

| Código | Descrição                                             |
|--------|-------------------------------------------------------|
| 202    | Accepted - Relatório está sendo gerado                |
| 400    | Bad Request - Validação falhou                        |
| 401    | Unauthorized - Token inválido ou expirado             |
| 403    | Forbidden - Sem permissão para gerar relatórios       |
| 500    | Internal Server Error - Erro interno no servidor      |

---

## Modelo de Dados

### TransactionDTO

| Propriedade | Tipo          | Descrição                                              |
|-------------|---------------|--------------------------------------------------------|
| id          | string (UUID) | Identificador único da transação                       |
| type        | string        | Tipo da transação: `CREDIT` ou `DEBIT`                 |
| amount      | string        | Valor da transação (formato decimal com 2 casas)       |
| currency    | string        | Moeda da transação (ISO 4217, atualmente apenas `BRL`) |
| date        | string        | Data da transação (formato `YYYY-MM-DD`)               |
| description | string        | Descrição textual da transação                         |
| createdAt   | string        | Timestamp de criação (formato ISO 8601)                |
| status      | string        | Status da transação: `PENDING`, `COMPLETED`, `FAILED`  |

---

## Regras de Negócio

### Tipos de Transação

- **CREDIT**: Representa entrada de recursos (valores positivos)
- **DEBIT**: Representa saída de recursos (valores negativos)

### Validação de Valores

- Transações `CREDIT` devem ter valores positivos
- Transações `DEBIT` devem ter valores negativos
- Valores são limitados a 2 casas decimais

### Estorno

- Transações estornadas mantêm o mesmo tipo da original, mas com valor invertido
- A data do estorno é a data atual, não a data da transação original
- Descrição inclui referência à transação original e motivo do estorno
- Uma transação só pode ser estornada uma vez

### Imutabilidade

- Transações já criadas não podem ser modificadas
- Correções devem ser feitas via estorno e nova transação

### Períodos Contábeis

- Transações não podem ser criadas em períodos contábeis fechados
- Transações não podem ser estornadas se o período estiver fechado

---

## Erros e Tratamento de Exceções

### Estrutura de Resposta de Erro

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Credit transactions must have positive amount",
  "timestamp": "2026-04-11T14:22:34.123Z"
}
```

### Códigos de Erro Comuns

| Código             | Descrição                                        |
|--------------------|--------------------------------------------------|
| VALIDATION_ERROR   | Erro de validação nos dados de entrada           |
| RESOURCE_NOT_FOUND | Recurso solicitado não encontrado                |
| PERIOD_CLOSED      | Operação não permitida em período fechado        |
| ALREADY_REVERSED   | Transação já foi estornada                       |
| ACCESS_DENIED      | Permissão insuficiente para a operação           |
| INTERNAL_ERROR     | Erro interno no processamento                    |

---

## Eventos Publicados

O Transaction Service publica eventos para outros serviços através do Google Cloud Pub/Sub.

### Evento de Criação de Transação

```json
{
  "eventType": "TRANSACTION_CREATED",
  "transactionId": "123e4567-e89b-12d3-a456-426614174000",
  "timestamp": "2026-04-10T14:30:45.123Z",
  "data": {
    "type": "CREDIT",
    "amount": "100.00",
    "currency": "BRL",
    "date": "2026-04-10",
    "description": "Recebimento de taxa de serviço"
  }
}
```

### Evento de Estorno de Transação

```json
{
  "eventType": "TRANSACTION_REVERSED",
  "transactionId": "323e4567-e89b-12d3-a456-426614174002",
  "originalTransactionId": "123e4567-e89b-12d3-a456-426614174000",
  "timestamp": "2026-04-11T09:45:22.789Z",
  "data": {
    "type": "CREDIT",
    "amount": "-100.00",
    "currency": "BRL",
    "date": "2026-04-11",
    "reason": "Lançamento incorreto"
  }
}
```

---

## Monitoramento e Métricas

O serviço expõe métricas através do Cloud Monitoring. As principais métricas incluem:

- `transaction.created.count` - Total de transações criadas
- `transaction.reversed.count` - Total de transações estornadas
- `transaction.processing.time` - Tempo médio de processamento de transações
- `http.server.requests.duration` - Duração das requisições por endpoint

---

## Adaptações para Google Cloud

### Armazenamento de Relatórios

Os relatórios gerados são armazenados no Cloud Storage e disponibilizados para download através de URLs assinadas com prazo de validade.

### Autenticação e Autorização

A autenticação é realizada através do Google Cloud Identity Platform, com tokens JWT validados pelo Cloud API Gateway e pelo serviço.
