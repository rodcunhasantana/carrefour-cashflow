# Modelo de Domínio - Daily Balance Service

Este documento descreve o modelo de domínio do **Daily Balance Service**, detalhando as principais entidades, *value objects* e as regras de negócio para a consolidação de saldos.

---

## 1. Entidades Principais

### `DailyBalance`
Representa o saldo consolidado de um dia específico. É o agregado principal responsável por manter a integridade financeira diária.

| Atributo | Tipo | Descrição |
| :--- | :--- | :--- |
| `id` | `UUID` | Identificador único do saldo diário. |
| `date` | `LocalDate` | Data de referência do saldo. |
| `openingBalance` | `Money` | Saldo inicial (vinda do fechamento do dia anterior). |
| `totalCredits` | `Money` | Soma de todos os créditos processados no dia. |
| `totalDebits` | `Money` | Soma de todos os débitos processados no dia (valor negativo). |
| `closingBalance` | `Money` | Saldo final (`openingBalance + totalCredits + totalDebits`). |
| `status` | `BalanceStatus` | Estado do dia (`OPEN` ou `CLOSED`). |
| `balancedAt` | `LocalDateTime` | Data/hora da última atualização. |

#### **Regras de Negócio**
* **Cálculo de Saldo:** O saldo de fechamento é sempre recalculado a cada nova transação para garantir precisão.
* **Continuidade:** O saldo de abertura de hoje deve ser obrigatoriamente o saldo de fechamento de ontem.
* **Unicidade:** O sistema garante que exista apenas um registro de `DailyBalance` por data.
* **Imutabilidade de Fechamento:** Um saldo com status `CLOSED` não aceita novas transações ou alterações. Este processo é irreversível.

#### **Comportamentos**
* `addCredit(amount)`: Incrementa o total de créditos e atualiza o fechamento.
* `addDebit(amount)`: Incrementa o total de débitos (preservando o sinal negativo) e atualiza o fechamento.
* `close()`: Altera o status para `CLOSED` e finaliza a contabilização do dia.

---

## 2. Value Objects

* **`Money`**: Encapsula o valor numérico (`BigDecimal`) e a moeda (`Currency`).
* **`BalanceStatus` (Enum)**:
    * `OPEN`: Permite entrada de novos eventos de transação.
    * `CLOSED`: Saldo consolidado e bloqueado para alterações.

---

## 3. Serviços e Repositórios

### `DailyBalanceService`
Responsável pela orquestração do domínio:
* Busca ou criação de saldos sob demanda.
* Cálculo de saldos acumulados em períodos (semana, mês).
* Garantia de que a sequência de saldos de abertura/fechamento não seja quebrada.

### `DailyBalanceRepository`
* `findByDate(LocalDate)`: Localiza o saldo de um dia específico.
* `findLatestBeforeDate(LocalDate)`: Recupera o último saldo fechado para iniciar um novo dia.

---

## 4. Fluxo de Eventos (Consumo)

O serviço é alimentado assincronamente através dos eventos originados no **Transaction Service**:

* **Ao receber `TransactionCreatedEvent`**:
    1. Identifica a data da transação.
    2. Recupera/Cria o saldo para aquela data.
    3. Aplica o valor no campo correspondente (`Credits` ou `Debits`).
    4. Salva o estado atualizado.

* **Ao receber `TransactionReversedEvent`**:
    1. Trata o estorno como uma transação compensatória.
    2. Aplica o valor invertido na data do estorno para manter o histórico auditável.

---

## 5. Diagrama de Classes Simplificado

```text
+----------------+       +----------------+       +-----------------+
|  DailyBalance  |       |     Money      |       |  BalanceStatus  |
+----------------+       +----------------+       +-----------------+
| - id: UUID     |       | - amount: Big  |       | OPEN            |
| - date: Date   |       | - currency: Cur|       | CLOSED          |
| - openingBal:  |<>---->|                |       +-----------------+
| - totalCredits:|       +----------------+              
| - totalDebits: |                   
| - closingBal:  |       
| - balancedAt   |       
| - status       |       
+----------------+       
| + addCredit()  |       
| + addDebit()   |       
| + recalculate()|       
| + close()      |       
+----------------+
```

---

## 6. Processos de Negócio e Observações

### Criação e Propagação
* **Criação sob Demanda:** Se uma transação chega para uma data que ainda não possui um registro de saldo, o sistema busca o fechamento mais recente anterior àquela data para definir o saldo de abertura inicial.
* **Integridade Retroativa:** Mudanças em um dia `N` nunca alteram o passado (dias `N-1`), garantindo a imutabilidade histórica.
* **Efeito Cascata:** Alterações em um saldo diário podem disparar recálculos em cascata para os dias subsequentes (`N+1`), caso o sistema exija consistência absoluta em tempo real para os saldos de abertura futuros.

---

## Conclusão

Este modelo de domínio foi desenhado para ser altamente eficiente em cenários de **leitura de grandes volumes**. Ele permite que o Dashboard financeiro consulte saldos consolidados de forma instantânea, eliminando a necessidade técnica de processar e somar milhões de transações individuais a cada nova requisição do usuário.