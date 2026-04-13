# Padrões Arquiteturais - Carrefour Cashflow

Este documento descreve os principais padrões arquiteturais aplicados no sistema Carrefour Cashflow, explicando como cada padrão é implementado e os benefícios obtidos.

## Clean Architecture / Arquitetura Hexagonal

### Descrição
A Clean Architecture (também conhecida como Arquitetura Hexagonal ou Ports and Adapters) organiza o código em camadas concêntricas, com regras de dependência apontando para dentro. As camadas externas podem depender das internas, mas não o contrário.

### Implementação no Carrefour Cashflow
Cada serviço implementa as seguintes camadas:

1. **Domain Layer (Core)**: Entidades, regras de negócio, interfaces de repositórios
    - `com.carrefourbank.transaction.domain`
    - `com.carrefourbank.dailybalance.domain`

2. **Application Layer**: Casos de uso, orquestração de fluxos
    - `com.carrefourbank.transaction.application`
    - `com.carrefourbank.dailybalance.application`

3. **Infrastructure Layer**: Implementações técnicas (APIs, banco de dados, mensageria)
    - `com.carrefourbank.transaction.infrastructure`
    - `com.carrefourbank.dailybalance.infrastructure`

### Benefícios
- **Testabilidade**: O domínio e a aplicação podem ser testados sem infraestrutura
- **Flexibilidade**: Adaptadores podem ser substituídos sem afetar o core
- **Clareza**: Separação clara de responsabilidades
- **Proteção do domínio**: Regras de negócio isoladas de detalhes técnicos

## Domain-Driven Design (DDD)

### Descrição
DDD é uma abordagem de desenvolvimento que conecta a implementação a um modelo evolutivo, priorizando o domínio do problema e usando uma linguagem ubíqua.

### Implementação no Carrefour Cashflow
1. **Modelo Rico de Domínio**:
    - Entidades encapsulam comportamento (ex: `Transaction.validate()`)
    - Value Objects para conceitos sem identidade (ex: `Money`)
    - Serviços de Domínio para operações que não pertencem naturalmente a uma entidade

2. **Bounded Contexts**:
    - `Transaction Service`: Contexto de lançamentos financeiros
    - `Daily Balance Service`: Contexto de saldos consolidados

3. **Linguagem Ubíqua**:
    - Termos financeiros consistentes em código e documentação
    - Ex: Transaction, Credit, Debit, Balance, Reversal

### Benefícios
- **Alinhamento com o negócio**: Código reflete terminologia e regras do domínio
- **Expressividade**: Intenções claras no código
- **Evolução suave**: Mudanças de negócio mapeiam diretamente para o código
- **Compartimentalização**: Contextos bem definidos evitam vazamento de conceitos

## Event-Driven Architecture

### Descrição
Arquitetura onde a comunicação entre componentes ocorre primariamente através de eventos, permitindo desacoplamento e comunicação assíncrona.

### Implementação no Carrefour Cashflow
1. **Eventos de Domínio**:
    - `TransactionCreatedEvent`
    - `TransactionReversedEvent`

2. **Publicação e Consumo**:
    - `PubSubTransactionEventPublisher`: Publica eventos no Google Cloud Pub/Sub
    - `TransactionEventConsumer`: Consome eventos para atualizar saldos

3. **Tópicos e Assinaturas**:
    - Tópico `transaction-events` para todos os eventos relacionados a transações
    - Assinatura do Daily Balance Service para processar eventos

### Benefícios
- **Desacoplamento temporal**: Serviços não precisam estar disponíveis simultaneamente
- **Escalabilidade**: Picos de carga são absorvidos pela infraestrutura de mensageria
- **Resiliência**: Falhas temporárias não comprometem a integridade do sistema
- **Extensibilidade**: Novos consumidores podem ser adicionados sem afetar produtores

## Repository Pattern

### Descrição
Encapsula a lógica de persistência e recuperação de objetos, proporcionando uma interface orientada a coleções.

### Implementação no Carrefour Cashflow
1. **Interfaces de Repositório**:
    - `TransactionRepository`: Interface no pacote de domínio
    - `DailyBalanceRepository`: Interface no pacote de domínio

2. **Implementações**:
    - `JdbcTransactionRepository`: Implementação JDBC do repositório
    - `JdbcDailyBalanceRepository`: Implementação JDBC do repositório

### Benefícios
- **Abstração de Persistência**: Domínio desacoplado de detalhes de persistência
- **Testabilidade**: Facilidade para mockar repositórios em testes
- **Encapsulamento de Queries**: Complexidade de consultas isolada em um lugar
- **Substituição Transparente**: Possibilidade de mudar implementações sem afetar clientes

## Command Query Responsibility Segregation (CQRS)

### Descrição
Padrão que separa operações de leitura (Queries) das operações de escrita (Commands), permitindo otimizações específicas para cada tipo.

### Implementação no Carrefour Cashflow
Implementamos uma versão simplificada do CQRS:

1. **Commands**:
    - `TransactionCreateCommand`: Para criar transações
    - `TransactionReversalCommand`: Para estornar transações

2. **Queries**:
    - Métodos de consulta específicos nos repositórios
    - DTOs de resposta otimizados para leitura

### Benefícios
- **Separação de Responsabilidades**: Código de leitura e escrita com propósitos claros
- **Performance**: Possibilidade de otimizar leituras e escritas independentemente
- **Escalabilidade**: Base para separação de modelos de leitura e escrita se necessário no futuro

## Circuit Breaker

### Descrição
Padrão que monitora falhas em chamadas a serviços externos e evita cascata de falhas interrompendo temporariamente chamadas quando um limiar é atingido.

### Implementação no Carrefour Cashflow
1. **Resilience4j**:
    - Configurado para chamadas externas (Pub/Sub)
    - Políticas de retry, fallback e circuit breaker configuradas

2. **Configuração**:
    - Limiares de falha configuráveis
    - Estados de half-open para recuperação gradual

### Benefícios
- **Falha Rápida**: Evita timeouts longos quando serviço está indisponível
- **Auto-recuperação**: Tenta restabelecer conexão automaticamente após período de espera
- **Proteção de Recursos**: Evita sobrecarga de serviços já comprometidos
- **Monitorabilidade**: Métricas sobre estado de circuit breakers

## Observer Pattern (via Events)

### Descrição
Padrão onde objetos (observers) se registram para serem notificados quando ocorrem mudanças em outro objeto (subject).

### Implementação no Carrefour Cashflow
Implementado via eventos e assinatura:

1. **Subjects**:
    - Transaction Service publica eventos quando transações são criadas/estornadas

2. **Observers**:
    - Daily Balance Service se inscreve para receber eventos de transação

### Benefícios
- **Desacoplamento**: O Transaction Service não conhece seus observers
- **Extensibilidade**: Novos observers podem ser adicionados sem modificar o subject
- **Separação de Responsabilidades**: Cada observer foca em sua própria lógica

## DTO (Data Transfer Object)

### Descrição
Objetos simples usados para transferir dados entre camadas, especialmente na comunicação com clientes.

### Implementação no Carrefour Cashflow
1. **DTOs de API**:
    - `TransactionDTO`: Para transferir dados de transações pela API
    - `DailyBalanceDTO`: Para transferir dados de saldos pela API

2. **Mapeadores**:
    - `TransactionMapper`: Converte entre entidades e DTOs
    - `DailyBalanceMapper`: Converte entre entidades e DTOs

### Benefícios
- **Desacoplamento**: API desacoplada de mudanças no modelo de domínio
- **Controle de Exposição**: Apenas dados necessários são expostos
- **Validação Específica**: Regras de validação para entrada de API separadas de regras de domínio
- **Evolução Independente**: API pode evoluir sem impactar o domínio e vice-versa

## Conclusion

Estes padrões arquiteturais trabalham em conjunto para criar um sistema modular, testável, escalável e orientado ao domínio. A aplicação consistente destes padrões em todos os serviços proporciona uniformidade e previsibilidade, facilitando a manutenção e evolução do sistema.