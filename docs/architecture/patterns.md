# Padrões Arquiteturais - Carrefour Cashflow System

## Visão Geral

O Carrefour Cashflow System implementa padrões arquiteturais robustos para garantir clareza, manutenibilidade e escalabilidade do código. Este documento apresenta os principais padrões aplicados, com foco especial na Arquitetura Hexagonal como o fundamento arquitetural do sistema.

---

## Arquitetura Hexagonal (Ports and Adapters)

A arquitetura principal do sistema é a Arquitetura Hexagonal, também conhecida como Ports and Adapters, proposta por Alistair Cockburn. Esta abordagem organiza o software em camadas concêntricas, protegendo a lógica de negócio de detalhes externos.

### Princípios Fundamentais

**Independência de Domínio:**
- O núcleo da aplicação contém regras de negócio puras
- O domínio não tem conhecimento de infraestrutura, frameworks ou interfaces externas

**Portas e Adaptadores:**
- Portas: Interfaces que definem como o domínio se comunica com o mundo externo
- Adaptadores: Implementações concretas das portas para tecnologias específicas

**Inversão de Dependência:**
- Dependências sempre apontam para o centro (domínio)
- Módulos externos dependem do domínio, não o contrário

### Componentes Principais

**Domínio (Núcleo):**
- Contém a lógica de negócio pura
- Define modelos, entidades e regras sem dependências externas

**Portas:**
- Portas Primárias (Entrada): Definem serviços que o domínio oferece ao mundo externo
- Portas Secundárias (Saída): Definem serviços que o domínio necessita do mundo externo

**Adaptadores:**
- Adaptadores Primários (Driver): Conduzem a aplicação (Controllers REST, Consumers de Eventos)
- Adaptadores Secundários (Driven): São conduzidos pela aplicação (Repositórios, Gateways)

### Implementação no Projeto

A estrutura de pacotes do projeto reflete diretamente a Arquitetura Hexagonal:

```
com.carrefourbank.transaction
├── domain                     # Núcleo do domínio
│   ├── model                  # Entidades e objetos de valor
│   │   ├── Transaction.java
│   │   └── TransactionStatus.java
│   └── port                   # Portas secundárias (saída)
│       ├── TransactionRepository.java
│       └── TransactionEventPublisher.java
├── application                # Casos de uso / Regras de aplicação
│   ├── dto                    # DTOs para transferência de dados
│   ├── port                   # Portas primárias (entrada)
│   │   └── TransactionService.java
│   └── service                # Implementação dos casos de uso
│       └── TransactionServiceImpl.java
└── infrastructure             # Implementações externas
    ├── adapter                
    │   ├── persistence        # Adaptadores secundários (repositórios)
    │   │   └── JdbcTransactionRepository.java
    │   └── pubsub             # Adaptadores secundários (mensageria)
    │       └── PubSubTransactionEventPublisher.java
    └── web                    # Adaptadores primários (controllers)
        └── TransactionController.java
```

### Exemplo Prático

**Porta (Interface)**

```java
// Porta secundária - Define como o domínio interage com armazenamento
public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(UUID id);
    List<Transaction> findAll(TransactionType type, int offset, int limit);
    int count(TransactionType type);
    boolean existsReversalFor(UUID originalId);
}
```

**Adaptador (Implementação)**

```java
// Adaptador secundário - Implementação concreta usando JDBC
@Repository
public class JdbcTransactionRepository implements TransactionRepository {
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public JdbcTransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public Transaction save(Transaction transaction) {
        // Implementação usando JDBC
    }
    
    // Outras implementações...
}
```

---

## Padrões de Design Complementares

Além da Arquitetura Hexagonal, o projeto utiliza outros padrões de design que se alinham bem com essa abordagem:

### Domain-Driven Design (DDD)

O DDD é aplicado para modelar o domínio complexo do sistema financeiro, com foco em:

- **Entidades**: Objetos com identidade e ciclo de vida (ex: `Transaction`)
- **Value Objects**: Objetos imutáveis sem identidade (ex: `Money`)
- **Agregados**: Grupos de objetos tratados como uma unidade (ex: `Transaction` e seus detalhes)
- **Eventos de Domínio**: Notificações sobre fatos relevantes no domínio (ex: `TransactionCreatedEvent`)

```java
// Exemplo de Value Object
public class Money {
    private final BigDecimal amount;
    private final Currency currency;
    
    private Money(BigDecimal amount, Currency currency) {
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }
    
    public static Money ofBRL(BigDecimal amount) {
        return new Money(amount, Currency.BRL);
    }
    
    public Money negate() {
        return new Money(amount.negate(), currency);
    }
    
    // Métodos imutáveis...
}
```

### Padrão Repository

Implementado para abstrair a persistência de dados, permitindo que o domínio trabalhe com coleções de objetos sem conhecer os detalhes de armazenamento:

```java
// Interface define operações sem detalhes de implementação
public interface DailyBalanceRepository {
    Optional<DailyBalance> findByDate(LocalDate date);
    DailyBalance save(DailyBalance dailyBalance);
    List<DailyBalance> findAll(BalanceStatus status, int offset, int limit);
    int count(BalanceStatus status);
    Optional<DailyBalance> findMostRecentClosedBefore(LocalDate date);
}

// Implementação concreta
@Repository
public class JdbcDailyBalanceRepository implements DailyBalanceRepository {
    // Implementação com JDBC
}
```

### Command Query Responsibility Segregation (CQRS)

Aplicado de forma leve para separar operações de leitura e escrita:

- **Commands**: Representam intenções de modificar o estado (ex: `TransactionCreateCommand`)
- **Queries**: Solicitações de dados sem modificar o estado
- **DTOs específicos**: Objetos distintos para leitura e escrita

```java
// Command para criar transação
public record TransactionCreateCommand(
    TransactionType type,
    BigDecimal amount,
    LocalDate date,
    String description
) {}

// DTO para retorno de consulta
public record TransactionDTO(
    UUID id,
    TransactionType type,
    BigDecimal amount,
    Currency currency,
    LocalDate date,
    String description,
    LocalDateTime createdAt,
    TransactionStatus status
) {}
```

### Event-Driven Architecture

Utilizada para comunicação assíncrona entre os microsserviços:

- **Eventos de Domínio**: Representam fatos ocorridos no domínio
- **Publicador de Eventos**: Adaptador para publicação de eventos
- **Consumidor de Eventos**: Adaptador para consumo e processamento de eventos

```java
// Porta para publicação de eventos
public interface TransactionEventPublisher {
    void publishTransactionCreatedEvent(Transaction transaction);
    void publishTransactionReversedEvent(Transaction originalTransaction, Transaction reversalTransaction);
}

// Adaptador para Google Cloud Pub/Sub
@Service
public class PubSubTransactionEventPublisher implements TransactionEventPublisher {
    private final PubSubTemplate pubSubTemplate;
    
    @Override
    public void publishTransactionCreatedEvent(Transaction transaction) {
        // Construir e publicar evento
    }
}
```

### Injeção de Dependência

Utilizada para fornecer implementações concretas das portas ao núcleo da aplicação:

```java
@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionEventPublisher eventPublisher;
    
    // Injeção via construtor
    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            TransactionEventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
    }
    
    // Implementação dos métodos...
}
```

---

## Benefícios da Arquitetura Adotada

- **Testabilidade**: Facilidade para testar o domínio isoladamente com mocks das portas
- **Flexibilidade**: Adaptadores podem ser substituídos sem impacto no domínio
- **Clareza**: Separação nítida entre regras de negócio e detalhes técnicos
- **Evolução Independente**: Componentes podem evoluir em ritmos diferentes
- **Resiliência**: Falhas em componentes externos são isoladas e gerenciadas

---

## Desafios e Soluções

| Desafio                                           | Solução Adotada                                                        |
|---------------------------------------------------|------------------------------------------------------------------------|
| Mapeamento entre objetos de domínio e DTOs        | Uso de mappers dedicados com conversão explícita                       |
| Tratamento de erros em diferentes camadas         | Exceções de domínio traduzidas para respostas HTTP apropriadas         |
| Transações distribuídas                           | Consistência eventual via eventos de domínio                           |
| Testes de componentes isolados                    | Mocks e stubs para portas secundárias                                  |
| Validação de entrada                              | Validação em duas camadas: básica no controlador, de negócio no domínio|
