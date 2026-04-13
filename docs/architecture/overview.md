# Visão Geral da Arquitetura - Carrefour Cashflow

## Introdução
O **Carrefour Cashflow** é um sistema de controle de fluxo de caixa projetado para gerenciar lançamentos financeiros e gerar saldos consolidados diários. A arquitetura foi desenhada para proporcionar alta escalabilidade, resiliência e manutenibilidade.

---

## Princípios Arquiteturais
A arquitetura do sistema é baseada nos seguintes princípios:

* **Separação de Responsabilidades:** Cada componente tem uma função específica e bem definida.
* **Design Orientado ao Domínio (DDD):** O código reflete diretamente os conceitos e regras do domínio financeiro.
* **Independência de Frameworks:** O núcleo de negócio é independente de frameworks e bibliotecas.
* **Comunicação Baseada em Eventos:** Serviços se comunicam principalmente através de eventos, garantindo baixo acoplamento.
* **Testabilidade:** O design favorece testes automatizados em todos os níveis.

---

## Visão de Alto Nível
O sistema é composto por dois microserviços principais:

1.  **Transaction Service:** Responsável pelo registro e gerenciamento dos lançamentos financeiros (débitos e créditos).
2.  **Daily Balance Service:** Responsável pela consolidação e consulta de saldos diários.

Estes serviços operam de forma independente, cada um com seu próprio banco de dados e exposição de API REST.

### Diagrama de Contexto
```text
+-------------------+      +-------------------------+
|                   |      |                         |
|  Usuários         |      |     Sistemas Externos   |
|  (Web/Mobile)     |      |     (Contabilidade)     |
|                   |      |                         |
+---------+---------+      +-----------+-------------+
          |                            |
          |                            |
          v                            v
+---------------------------------------------------+
|                                                   |
|          Carrefour Cashflow System                |
|                                                   |
+------------------+----------------------------+---+
|                  |                            |
| Transaction      | Daily Balance              |
| Service          | Service                    |
|                  |                            |
+------------------+----------------------------+
```

## Fluxo de Dados Principal

O fluxo de informações entre os serviços segue um modelo orientado a eventos para garantir consistência e performance:

1. **Registro:** Os usuários registram transações financeiras (débitos/créditos) através do `Transaction Service`.
2. **Processamento:** O `Transaction Service` valida a regra de negócio, persiste os dados no banco e publica um evento de transação.
3. **Consolidação:** O `Daily Balance Service` consome os eventos de transação de forma assíncrona e atualiza os saldos diários.
4. **Consulta:** Os usuários consultam os saldos consolidados e relatórios diários através do `Daily Balance Service`.

---

## Componentes Técnicos

A stack tecnológica foi selecionada visando modernidade e suporte a longo prazo:

* **Backend:** Java 21, Spring Boot 3.x
* **Banco de Dados:** PostgreSQL
* **Mensageria:** Google Cloud Pub/Sub
* **Migrações de BD:** Flyway
* **Conteinerização:** Docker & Docker Compose

---

## Qualidades Arquiteturais

O sistema foi projetado sob os pilares de robustez e agilidade:

* **Escalabilidade:** Cada microserviço pode escalar horizontalmente de forma independente conforme a demanda.
* **Disponibilidade:** A falha isolada de um serviço não compromete a operação total do sistema (resiliência).
* **Manutenibilidade:** A organização clara e o desacoplamento facilitam a implementação de mudanças e correções.
* **Extensibilidade:** Novos recursos ou serviços podem ser adicionados com impacto mínimo na estrutura existente.
* **Observabilidade:** Implementação de logs estruturados e métricas que facilitam o monitoramento em tempo real.

