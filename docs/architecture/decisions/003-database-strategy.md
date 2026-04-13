# ADR 003: Estratégia de Banco de Dados por Serviço

## Status
✅ **Aceito**

---

## Contexto
Com a adoção de uma arquitetura de microsserviços (ver [ADR-001](./ADR-001.md)), precisamos definir a estratégia para persistência de dados. Especificamente, precisamos decidir se cada serviço deve ter seu próprio banco de dados ou se devemos compartilhar uma única instância entre os serviços.

---

## Decisão
Adotaremos uma estratégia de **Banco de Dados por Serviço (Database per Service)**, onde:

* O **Transaction Service** terá seu próprio banco de dados PostgreSQL para armazenar transações financeiras.
* O **Daily Balance Service** terá seu próprio banco de dados PostgreSQL para armazenar saldos consolidados diários.

Cada serviço será o único proprietário de seu banco de dados, com acesso exclusivo e direto a ele. A comunicação de dados entre serviços nunca ocorrerá via banco.

---

## Alternativas Consideradas

### 1. Banco de Dados Compartilhado
* **Vantagens:** Garantia imediata de consistência, facilidade de queries entre domínios, operações atômicas (Transactions ACID) entre contextos.
* **Desvantagens:** Forte acoplamento, dificuldade de evolução independente, risco de "esquema compartilhado" e violação de limites de contexto.

### 2. Banco Único com Schemas Separados
* **Vantagens:** Menor sobrecarga operacional, facilidade de backup/restore unificado.
* **Desvantagens:** Ainda permite acoplamento indevido via queries diretas, risco de impacto cruzado em performance (um serviço pesado afeta o outro).

### 3. Abordagem de CQRS Completa
* **Vantagens:** Otimização específica para leitura vs. escrita, modelos de dados altamente especializados.
* **Desvantagens:** Complexidade significativamente maior e maior esforço de sincronização para o estágio atual do projeto.

---

## Consequências

### ✅ Positivas
* **Isolamento completo:** Mudanças no esquema de um banco não afetam o outro serviço.
* **Escalabilidade Independente:** Podemos escalar os recursos de banco conforme a carga específica de cada domínio.
* **Poliglotismo:** Liberdade para escolher tecnologias de banco diferentes no futuro se necessário.
* **Alta Coesão:** Reforça as fronteiras do *Bounded Context*.

### ❌ Negativas
* **Consistência Eventual:** Mitigada pelo design orientado a eventos ([ADR-002](./ADR-002.md)).
* **Complexidade de Queries:** Impossibilidade de realizar `JOINs` SQL entre dados de diferentes serviços.
* **Sobrecarga Operacional:** Necessidade de gerenciar múltiplos bancos, backups e monitoramento.

---

## Implicações
* **Migrações:** Implementação de migrações independentes via **Flyway**.
* **Comunicação:** Acesso a dados externos deve ser feito estritamente via API ou Eventos.
* **Composição:** Operações que exigem dados de ambos os domínios devem ser compostas na camada de aplicação (Aggregator).
* **Backup:** Estratégia robusta de backup/restore coordenada para garantir integridade em caso de desastres.

---

## Implementação
* Cada serviço terá sua própria configuração de `DataSource` no Spring Boot.
* Scripts Flyway serão mantidos em diretórios separados dentro de cada projeto.
* Eventuais replicações de dados (cache local de leitura) serão geridas via eventos de domínio.

---

## Observações
Esta estratégia reforça o isolamento entre *bounded contexts*. Embora introduza desafios de consistência eventual, estes são aceitáveis para o domínio de fluxo de caixa, onde a reconciliação baseada em períodos contábeis é uma prática padrão.