# Visão Geral da Arquitetura - Carrefour Cashflow System

## Introdução

O Carrefour Cashflow System é uma plataforma de gestão financeira projetada para gerenciar transações e consolidar saldos diários para o Carrefour Bank. Este documento fornece uma visão geral da arquitetura do sistema, incluindo seus componentes principais, padrões arquiteturais e decisões tecnológicas.

## Objetivos do Sistema

O sistema foi projetado para atender os seguintes objetivos:

- Registrar e gerenciar transações financeiras (créditos e débitos)
- Consolidar saldos diários com base nas transações
- Permitir fechamento e reconciliação de períodos contábeis
- Fornecer relatórios e análises financeiras
- Integrar com sistemas corporativos existentes

## Visão Geral da Arquitetura

O Carrefour Cashflow adota uma **arquitetura de microsserviços**, implementada seguindo os princípios da **Arquitetura Hexagonal** (Ports and Adapters). O sistema é hospedado na **Google Cloud Platform**, utilizando serviços gerenciados para maximizar a escalabilidade e minimizar o overhead operacional.

### Diagrama de Contexto

O sistema interage com diferentes atores e sistemas externos:

[Diagrama de Contexto C4](c4/context.md)

### Principais Componentes

O sistema é composto por dois microsserviços principais:

1. **Transaction Service**: Gerencia o ciclo de vida das transações financeiras
    - Criação e consulta de transações
    - Estorno de transações
    - Validação de regras de negócio para transações

2. **Daily Balance Service**: Gerencia a consolidação e fechamento de saldos diários
    - Cálculo e atualização de saldos baseados em transações
    - Fechamento e reabertura de períodos contábeis
    - Geração de relatórios consolidados

### Infraestrutura na Google Cloud Platform

O sistema utiliza os seguintes serviços do Google Cloud:

- **Cloud Run**: Hospedagem serverless para os microsserviços
- **Cloud SQL (PostgreSQL)**: Banco de dados relacional gerenciado
- **Cloud API Gateway**: Gerenciamento de APIs e roteamento de requisições
- **Cloud Pub/Sub**: Comunicação assíncrona entre serviços
- **Identity Platform**: Autenticação e autorização
- **Cloud Monitoring e Logging**: Observabilidade
- **Secret Manager**: Gerenciamento seguro de credenciais
- **Cloud Storage**: Armazenamento de relatórios e documentos

## Padrões Arquiteturais

### Arquitetura Hexagonal (Ports and Adapters)

Cada microsserviço implementa o padrão de Arquitetura Hexagonal:

- **Core/Domain**: Centro do serviço, contendo as regras de negócio
- **Portas (interfaces)**: Definem como o domínio interage com o mundo externo
- **Adaptadores**: Implementações específicas de tecnologia para as portas

Esta abordagem permite:
- Isolamento da lógica de negócio
- Testabilidade melhorada
- Flexibilidade para trocar componentes de infraestrutura

### Event-Driven Architecture

Os serviços se comunicam através de eventos em dois sentidos:

| Direção | Tópico | Producer | Consumer | Efeito |
|---|---|---|---|---|
| Transação → Saldo | `transaction-events` | transaction-service | dailybalance-service | Atualiza saldo do dia |
| Fechamento → Bloqueio | `period-events` | dailybalance-service | transaction-service | Bloqueia novos lançamentos na data |

Isso permite:
- Desacoplamento total entre serviços (sem chamadas síncronas)
- Resiliência: falha em um serviço não interrompe o outro
- Processamento assíncrono com idempotência garantida

### Padrões Complementares

- **Domain-Driven Design (DDD)**: Para modelagem rica de domínio
- **CQRS (Command Query Responsibility Segregation)**: Separação de operações de leitura e escrita
- **Repository Pattern**: Para abstração de acesso a dados

## Decisões Tecnológicas Principais

### Backend

- **Linguagem**: Java 21
- **Framework**: Spring Boot 3.2.4
- **Persistência**: JDBC direto com PostgreSQL (sem JPA/Hibernate)
- **Cache**: Caffeine (in-process) no Daily Balance Service — `@Cacheable`/`@CacheEvict` via Spring Cache abstraction
- **Segurança**: Spring Security — API Key via header `X-API-Key` (ver [ADR-006](decisions/006-security.md))
- **Tracing**: Micrometer Tracing + Brave — `traceId`/`spanId` automáticos no MDC
- **API**: REST com OpenAPI — Swagger UI disponível em runtime (`/swagger-ui.html` em cada serviço)
- **Contêinerização**: Docker

### Infraestrutura

- **Cloud Provider**: Google Cloud Platform (ver [ADR-005](decisions/005-cloud-provider-selection.md))
- **Deployment**: Cloud Run (serverless)
- **Banco de Dados**: Cloud SQL para PostgreSQL
- **CI/CD**: Cloud Build com GitOps

## Considerações de Qualidade

### Escalabilidade

- Serviços stateless para escala horizontal
- Cloud Run escala automaticamente com a demanda
- Banco de dados pode escalar verticalmente conforme necessário

### Segurança

- **API Key via `X-API-Key`**: todos os endpoints `/api/**` exigem o header. Implementado como `OncePerRequestFilter` (Spring Security). Ver [ADR-006](decisions/006-security.md).
- Endpoints públicos: `/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**`
- Chave configurada via `APP_API_KEY` (Secret Manager em produção)
- Encriptação em trânsito (HTTPS/TLS) e em repouso (Cloud SQL)
- Segmentação de rede com VPC

### Observabilidade

- **Logging estruturado com MDC**: formato `[appName, traceId, spanId, domainId]`
- **Micrometer Tracing (Brave)**: `traceId` e `spanId` gerados automaticamente por requisição HTTP — sem exportador externo
- **Campos de domínio**: `transactionId` populado pelo `TransactionLogger`; `balanceId` pelo `DailyBalanceLogger`
- **Métricas e Prometheus**: Spring Actuator em `/actuator/metrics` e `/actuator/prometheus`
- **Resilience4j**: Circuit Breaker e Retry nos publishers de eventos Pub/Sub
- Em produção: Cloud Monitoring + Cloud Logging recebem os traces via OTLP

### Resiliência

- Padrões de circuit breaker e retry
- Comunicação assíncrona para operações não críticas
- Múltiplas zonas de disponibilidade

## Limitações e Riscos

- Lock-in parcial com serviços específicos do Google Cloud
- Possível latência em comunicações assíncronas
- Complexidade de debugging em ambiente distribuído

## Próximos Passos e Evolução

Evoluções mapeadas com contexto técnico, esforço e dependências estão detalhadas em **[docs/roadmap.md](../roadmap.md)**.

Resumo dos principais itens:

| Evolução | Descrição |
|---|---|
| Cloud Build (CI/CD) | Deploys automáticos, rastreáveis e sem intervenção manual |
| Relatórios financeiros | Exportação de consolidado por período diretamente pela API |
| Flyway | Evolução do banco sem downtime e com histórico auditável |
| Cloud Monitoring + Trace | Detecção proativa de problemas e rastreabilidade financeira |
| IA no desenvolvimento | Maior velocidade de entrega com menor risco de regressão |

## Documentação Relacionada

- [Diagrama de Contêineres (C4)](c4/containers.md)
- [Diagrama de Componentes (C4)](c4/components.md)
- [Eventos de Domínio](../domain/domain-events.md)
- [Registros de Decisões Arquiteturais (ADRs)](decisions/) — incluindo [ADR-007: Arquitetura de Transição](decisions/007-transition-architecture.md) (Strangler Fig para migração de legado)
- [Matriz de Rastreabilidade](../traceability.md)
- [Estimativa de Custo GCP](cost-estimation.md)
- [Roadmap de Evoluções](../roadmap.md)
- **API (runtime)**: Swagger UI disponível em `http://localhost:8080/transaction-service/swagger-ui.html` e `http://localhost:8081/dailybalance-service/swagger-ui.html` com os serviços rodando