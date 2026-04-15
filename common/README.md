# Common

Biblioteca interna compartilhada entre os microserviços do sistema **Carrefour Cashflow**. Contém os tipos de domínio fundamentais e a hierarquia de exceções de negócio utilizados pelo `transaction-service` e pelo `dailybalance-service`.

## Sumário

- [Visão Geral](#visão-geral)
- [Dependências](#dependências)
- [Domínio](#domínio)
  - [Money](#money)
  - [Currency](#currency)
  - [TransactionType](#transactiontype)
- [Exceções](#exceções)
  - [BusinessException](#businessexception)
  - [ValidationException](#validationexception)
  - [NotFoundException](#notfoundexception)
- [Como usar nos microserviços](#como-usar-nos-microserviços)
- [Estrutura do Projeto](#estrutura-do-projeto)

---

## Visão Geral

| Propriedade  | Valor                                              |
|--------------|----------------------------------------------------|
| Artefato     | `common`                                           |
| Group ID     | `com.carrefourbank`                                |
| Versão       | `1.0.0-SNAPSHOT`                                   |
| Tipo         | Biblioteca JAR (sem Spring Boot entry point)       |
| Descrição    | Componentes compartilhados entre os serviços       |

Este módulo é intencionalmente **livre de frameworks**. As classes são POJOs puros — sem anotações Spring, Jackson ou Bean Validation — o que as torna portáveis e testáveis de forma isolada.

---

## Dependências

| Dependência   | Uso                              |
|---------------|----------------------------------|
| `junit-jupiter` | Testes unitários (escopo `test`) |

Este módulo é intencionalmente **livre de dependências de framework** em tempo de compilação. Todas as versões são gerenciadas pelo POM pai (`carrefour-cashflow`).

---

## Domínio

### Money

> `com.carrefourbank.common.domain.Money`

Value Object **imutável** que representa um valor monetário com moeda associada. É a representação canônica de valores financeiros no sistema.

#### Criação

```java
// Fábricas estáticas — construtor é privado
Money creditoBRL  = Money.ofBRL(new BigDecimal("100.00"));
Money debitoUSD   = Money.ofUSD(new BigDecimal("50.00"));
Money generico    = Money.of(new BigDecimal("200.00"), Currency.EUR);
Money porCodigo   = Money.of(new BigDecimal("75.00"), "GBP");
Money zero        = Money.zero(Currency.BRL);
```

#### Aritmética

Todas as operações retornam **novas instâncias** — o objeto original nunca é modificado.

```java
Money a = Money.ofBRL(new BigDecimal("100.00"));
Money b = Money.ofBRL(new BigDecimal("30.00"));

Money soma       = a.add(b);               // R$ 130.00
Money diferenca  = a.subtract(b);          // R$ 70.00
Money dobro      = a.multiply(BigDecimal.TWO); // R$ 200.00
Money metade     = a.divide(new BigDecimal("2")); // R$ 50.00
Money invertido  = a.negate();             // R$ -100.00
Money absoluto   = invertido.abs();        // R$ 100.00
```

> `add` e `subtract` lançam `IllegalArgumentException` se as moedas forem diferentes.

#### Consultas

```java
money.isZero();      // amount == 0
money.isPositive();  // amount > 0
money.isNegative();  // amount < 0
money.amount();      // BigDecimal
money.currency();    // Currency
```

#### Igualdade

A comparação usa `BigDecimal.compareTo` — `100` e `100.00` são considerados iguais.

```java
Money.ofBRL(new BigDecimal("100"))
    .equals(Money.ofBRL(new BigDecimal("100.00"))); // true
```

#### toString

```java
Money.ofBRL(new BigDecimal("99.90")).toString(); // "R$ 99.90"
Money.ofUSD(new BigDecimal("10.00")).toString(); // "$ 10.00"
```

---

### Currency

> `com.carrefourbank.common.domain.Currency`

Enum das moedas suportadas pelo sistema.

| Constante | Símbolo | Descrição          |
|-----------|---------|--------------------|
| `BRL`     | `R$`    | Real Brasileiro    |
| `USD`     | `$`     | Dólar Americano    |
| `EUR`     | `€`     | Euro               |
| `GBP`     | `£`     | Libra Esterlina    |

```java
Currency.BRL.getSymbol();   // "R$"
Currency.BRL.isDefault();   // true — BRL é a moeda padrão do sistema
Currency.USD.isDefault();   // false
```

---

### TransactionType

> `com.carrefourbank.common.domain.TransactionType`

Enum dos tipos de transação financeira.

| Constante | Sinal     | Descrição           |
|-----------|-----------|---------------------|
| `CREDIT`  | Positivo  | Entrada de recursos |
| `DEBIT`   | Negativo  | Saída de recursos   |

```java
TransactionType type = TransactionType.CREDIT;
```

---

## Exceções

Hierarquia de exceções **unchecked** para representar falhas de regras de negócio.

```
RuntimeException
└── BusinessException (abstract)
    ├── ValidationException
    └── NotFoundException
```

### BusinessException

> `com.carrefourbank.common.exception.BusinessException`

Classe **abstrata** base para todas as exceções de negócio. Não deve ser instanciada diretamente.

```java
public abstract class BusinessException extends RuntimeException {
    public BusinessException(String message) { ... }
    public BusinessException(String message, Throwable cause) { ... }
}
```

### ValidationException

> `com.carrefourbank.common.exception.ValidationException`

Lançada quando uma regra de negócio de validação é violada.

```java
throw new ValidationException("Credit transactions must have positive amount");
```

### NotFoundException

> `com.carrefourbank.common.exception.NotFoundException`

Lançada quando um recurso solicitado não é encontrado.

```java
throw new NotFoundException("Transaction not found: " + id);
```

---

## Como usar nos microserviços

O módulo já está declarado como dependência nos microserviços via POM pai. Nenhuma configuração adicional é necessária.

```xml
<!-- Declarado automaticamente via parent BOM -->
<dependency>
    <groupId>com.carrefourbank</groupId>
    <artifactId>common</artifactId>
</dependency>
```

### Exemplo de uso em um serviço

```java
import com.carrefourbank.common.domain.Money;
import com.carrefourbank.common.domain.Currency;
import com.carrefourbank.common.domain.TransactionType;
import com.carrefourbank.common.exception.NotFoundException;
import com.carrefourbank.common.exception.ValidationException;

public class TransactionServiceImpl {

    public Transaction create(TransactionType type, BigDecimal amount) {
        Money money = Money.ofBRL(amount);

        if (type == TransactionType.CREDIT && money.isNegative()) {
            throw new ValidationException("Credit transactions must have positive amount");
        }

        // ...
    }

    public Transaction findById(String id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));
    }
}
```

---

## Estrutura do Projeto

```
common/
├── src/
│   ├── main/
│   │   └── java/com/carrefourbank/common/
│   │       ├── domain/
│   │       │   ├── Currency.java          # Enum de moedas suportadas
│   │       │   ├── Money.java             # Value Object monetário imutável
│   │       │   └── TransactionType.java   # Enum de tipos de transação
│   │       └── exception/
│   │           ├── BusinessException.java # Base abstrata para exceções de negócio
│   │           ├── NotFoundException.java # Recurso não encontrado
│   │           └── ValidationException.java # Violação de regra de negócio
│   └── test/
│       └── java/com/carrefourbank/common/
│           └── domain/
│               ├── CurrencyTest.java      # Testes de Currency
│               └── MoneyTest.java         # Testes de Money (aritmética, equals/hashCode)
└── pom.xml
```
