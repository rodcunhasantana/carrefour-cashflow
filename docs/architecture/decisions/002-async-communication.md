# ADR 002: Comunicação Assíncrona entre Serviços

## Status
✅ **Aceito**

---

## Contexto
Com a adoção de uma arquitetura de microsserviços (ver [ADR-001](./ADR-001.md)), precisamos definir como os serviços se comunicarão entre si. A comunicação entre o **Transaction Service** e o **Daily Balance Service** é particularmente importante, pois o Daily Balance Service precisa reagir a transações criadas ou estornadas em tempo real.

---

## Decisão
Adotaremos um padrão de **comunicação assíncrona baseada em eventos** utilizando o **Google Cloud Pub/Sub** como broker de mensagens.

### Fluxo de Comunicação:
1. **Transaction Service:** Publica eventos sempre que transações são criadas ou estornadas.
2. **Daily Balance Service:** Assina (subscreve) esses eventos e processa as atualizações dos saldos diários de forma assíncrona.

### Eventos Principais:
* `transaction-created`: Publicado quando uma nova transação é criada com sucesso.
* `transaction-reversed`: Publicado quando uma transação é estornada ou cancelada.

---

## Alternativas Consideradas

### 1. Comunicação Síncrona via API REST
* **Vantagens:** Mais simples de implementar e depurar inicialmente.
* **Desvantagens:** Forte acoplamento temporal, risco de cascata de falhas (se um cair, o outro para) e pior performance percebida pelo usuário final.

### 2. Banco de Dados Compartilhado
* **Vantagens:** Não requer infraestrutura adicional de mensageria.
* **Desvantagens:** Forte acoplamento em nível de esquema, considerado um anti-padrão em microsserviços.

### 3. Outros Message Brokers (Kafka, RabbitMQ)
* **Vantagens:** Características específicas de retenção (Kafka) ou roteamento complexo (RabbitMQ).
* **Desvantagens:** Maior complexidade operacional e overhead de configuração para o escopo atual do projeto.

---

## Consequências

### ✅ Positivas
* **Desacoplamento temporal:** Os serviços não precisam estar online ao mesmo tempo.
* **Resiliência:** Falhas temporárias em um serviço não impedem que o outro continue aceitando requisições.
* **Persistência:** Mensagens ficam guardadas no broker até que o processamento seja confirmado.
* **Replay:** Possibilidade de reprocessar eventos históricos em caso de necessidade de correção de dados.

### ❌ Negativas
* **Consistência eventual:** O saldo pode demorar alguns milissegundos/segundos para refletir a transação.
* **Complexidade:** Exige tratamento de mensagens duplicadas e monitoramento de filas.

---

## Implicações
* **Idempotência:** ✅ Implementada. O Daily Balance Service persiste o `eventId` na tabela `processed_events` antes de aplicar cada evento. Reentregas com o mesmo `eventId` são descartadas via `DuplicateKeyException`.
* **Fechamento de Período:** ✅ Implementado. O Daily Balance Service publica evento `period-closed` no tópico `period-events`. O Transaction Service consome esse evento e persiste a data em `closed_periods`, passando a rejeitar novos lançamentos para datas fechadas com HTTP 422.
* **Ordenação:** O sistema não depende de ordem — cada evento é aplicado de forma acumulativa ao saldo do dia correspondente.
* **Monitoramento:** Alertas para latência de mensagens e filas com acúmulo são recomendados. Dead Letter Queues (`transaction-events-dlq`, `period-events-dlq`) estão configuradas com `maxDeliveryAttempts: 5` tanto no ambiente local (emulador Pub/Sub via `docker-compose.yml`) quanto em produção.
* **Contrato de Dados:** Novos campos são sempre adicionados como opcionais. Consumidores ignoram campos desconhecidos.

---

## Observações
O **Google Cloud Pub/Sub** foi escolhido por ser uma solução **gerenciada**, o que reduz a carga operacional da equipe, permitindo o foco total nas regras de negócio.

> **Ambiente Local:** Para desenvolvimento e testes locais, utilizaremos o **emulador do Pub/Sub** via Docker para garantir paridade com o ambiente de produção.