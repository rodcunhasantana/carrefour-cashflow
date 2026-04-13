# ADR 001: Adoção de Arquitetura de Microsserviços

## Status
✅ **Aceito**

---

## Contexto
O projeto **Carrefour Cashflow** precisa de uma arquitetura que permita:
* Evolução independente de funcionalidades de transações e saldos.
* Escalabilidade adequada para lidar com picos de carga.
* Isolamento de falhas para garantir disponibilidade.
* Manutenibilidade e clareza de responsabilidades.

---

## Decisão
Adotaremos uma arquitetura de microsserviços, com dois serviços principais:

1.  **Transaction Service:** Gerenciamento de transações financeiras.
2.  **Daily Balance Service:** Consolidação e consulta de saldos diários.

**Cada serviço terá:**
* Sua própria base de código.
* Seu próprio banco de dados (Database-per-Service).
* APIs REST independentes.
* Implantação (deployment) independente.

---

## Alternativas Consideradas

### 1. Monolito Modular
* **Vantagens:** Simplicidade de desenvolvimento inicial, comunicação direta entre módulos.
* **Desvantagens:** Acoplamento maior, escalabilidade limitada, risco de se tornar um "Big Ball of Mud".

### 2. Sistema Distribuído com Banco de Dados Compartilhado
* **Vantagens:** Consistência de dados mais simples, menos duplicação.
* **Desvantagens:** Forte acoplamento via banco de dados, limitações de escalabilidade, risco de esquema compartilhado.

---

## Consequências

### ✅ Positivas
* Serviços podem evoluir em ritmos diferentes.
* Possibilidade de escalar cada serviço independentemente.
* Falhas em um serviço não afetam diretamente o outro.
* Fronteiras de contexto claras entre domínios.

### ❌ Negativas
* Maior complexidade operacional.
* Desafios de consistência de dados entre serviços.
* Necessidade de mecanismos de comunicação entre serviços (mensageria).
* Possível duplicação de alguns dados ou lógica.

---

## Implicações
* Precisaremos implementar **comunicação assíncrona** entre serviços.
* Cada serviço precisará de sua própria estratégia de persistência.
* Será necessário lidar com **consistência eventual** entre serviços.
* Teremos que implementar estruturas de **observabilidade distribuída** (logs e traces).

---

## Observações
Embora a arquitetura de microsserviços traga complexidade adicional, os benefícios de escalabilidade, resiliência e independência evolutiva justificam essa escolha para o domínio de fluxo de caixa, que tem requisitos distintos para processamento de transações e consolidação de saldos.

> **Nota:** Inicialmente, manteremos o número de serviços limitado a dois para manter a complexidade gerenciável.