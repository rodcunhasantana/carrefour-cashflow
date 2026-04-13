# Modelo de Domínio - Transaction Service

Este documento descreve o núcleo do **Transaction Service**, detalhando as entidades, objetos de valor e as regras de negócio que garantem a integridade dos lançamentos financeiros.

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
| `status` | `TransactionStatus` | Estado atual (`PENDING`, `COMPLETED`, etc). |
| `createdAt` | `LocalDateTime` | Timestamp de auditoria da criação. |

#### **Regras de Negócio de Transaction**
* **Sinalização por Tipo:** * Transações de **Crédito** devem obrigatoriamente possuir valor positivo.
    * Transações de **Débito** devem obrigatoriamente possuir valor negativo.
* **Imutabilidade:** Uma vez criada, uma transação não pode ser editada. Erros devem ser corrigidos via **Estorno (Reversal)**, gerando um novo registro compensatório.
* **Integridade:** A descrição é um campo obrigatório e não pode ser nulo ou vazio.

---

## 2. Value Objects (VOs)

### `Money`
Objeto de valor que encapsula a lógica financeira, evitando erros de arredondamento com `double` ou `float`.
* **Atributos:** `amount` (BigDecimal) e `currency` (Enum).
* **Comportamentos:** Soma de valores, inversão de sinal (negate) e validação de sinal (positivo/negativo).

### Enums
* **`TransactionType`**: Define a natureza do fluxo (`CREDIT`, `DEBIT`).
* **`TransactionStatus`**: Controla o ciclo de vida (`PENDING`, `COMPLETED`, `CANCELED`, `REVERSED`).
* **`Currency`**: Moedas suportadas pelo sistema (`BRL`, `USD`, `EUR`).

---

## 3. Serviços e Repositórios

### `TransactionService` (Domain Service)
Orquestra a lógica que não pertence naturalmente a uma única entidade:
* Executa a fábrica de criação de transações.
* Gerencia a lógica de busca e filtros por período.
* Coordena o processo de estorno vinculando a nova transação à original.

### `TransactionRepository`
Interface de abstração para persistência (PostgreSQL):
* `save(Transaction)`: Persiste o estado atual.
* `findByDateBetween(...)`: Recupera transações para relatórios ou consolidação.

---

## 4. Eventos de Domínio

Os eventos permitem que o sistema seja extensível e que o **Daily Balance Service** seja atualizado de forma assíncrona.

* **`TransactionCreatedEvent`**: Contém o ID, valor, tipo e data da nova transação.
* **`TransactionReversedEvent`**: Contém o ID da transação original e o ID da transação de estorno, além do motivo.

---

## 5. Diagrama de Classes Simplificado

```text
+----------------+       +----------------+       +-----------------+
|  Transaction   |       |     Money      |       | TransactionType |
+----------------+       +----------------+       +-----------------+
| - id: UUID     |       | - amount: Big  |       | CREDIT          |
| - type: Type   |<>---->| - currency: Cur|       | DEBIT           |
| - amount: Money|       +----------------+       +-----------------+
| - date: Date   |
| - description  |       +-----------------+
| - createdAt    |       |TransactionStatus|
| - status       |       +-----------------+
+----------------+       | PENDING         |
| + validate()   |       | COMPLETED       |
| + createRev...()|      | CANCELED        |
+----------------+       | REVERSED        |
        ^                +-----------------+
        |
        |
+----------------------+
| Reversal Transaction |
+----------------------+
```
---

## Conclusão

O modelo de domínio do Transaction Service foi projetado para ser resiliente a falhas humanas através do uso de imutabilidade e para ser altamente auditável.

A utilização de Value Objects para a representação de valores monetários e a implementação de Eventos de Domínio para a comunicação entre serviços garantem que o sistema mantenha a precisão financeira e a escalabilidade técnica necessárias para uma operação de fluxo de caixa robusta.