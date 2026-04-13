# ADR 004: Estratégia de Testes

## Status
✅ **Aceito**

---

## Contexto
O sistema **Carrefour Cashflow** requer uma estratégia de testes abrangente para garantir a qualidade do software e a correta implementação das regras de negócio. Com a arquitetura de microsserviços adotada, precisamos de uma abordagem que cubra desde componentes isolados até a integração completa entre serviços e contratos de API.

---

## Decisão
Adotaremos uma estratégia de testes em múltiplas camadas, seguindo o modelo da **Pirâmide de Testes**, priorizando testes rápidos e isolados na base e testes mais complexos no topo:

1.  **Testes Unitários:** Focados em lógica de negócio e componentes isolados.
2.  **Testes de Integração:** Verificam a interação entre componentes e recursos externos (BD/Mensageria).
3.  **Testes de API:** Validam os contratos REST e protocolos de comunicação.
4.  **Testes de Componente:** Testam o microsserviço como uma unidade funcional completa.
5.  **Testes End-to-End (E2E):** Verificam fluxos de negócio que atravessam múltiplos serviços.

### Tecnologias e Frameworks
* **Unitários / Integração:** JUnit 5, Mockito.
* **Infraestrutura de Teste:** TestContainers (para bancos reais), H2 (banco em memória).
* **API:** Spring MockMvc, REST Assured.
* **E2E:** Cucumber com Selenium ou Playwright.

---

## Alternativas Consideradas

### 1. Foco apenas em Testes End-to-End
* **Vantagens:** Validação real da experiência do usuário.
* **Desvantagens:** Execução lenta, testes frágeis ("flaky tests") e dificuldade em diagnosticar a causa raiz de falhas.

### 2. Abordagem "Testing Trophy" (Foco em Integração)
* **Vantagens:** Equilíbrio entre custo e confiança, menos uso de mocks.
* **Desvantagens:** Testes mais pesados que os unitários, exigindo mais infraestrutura.

---

## Consequências

### ✅ Positivas
* **Feedback Rápido:** Testes unitários detectam erros em segundos durante o desenvolvimento.
* **Confiança:** Camadas superiores garantem que a integração entre peças móveis funciona.
* **Granularidade:** Facilita a identificação exata de onde o bug foi introduzido.
* **TDD:** O design favorece o Desenvolvimento Orientado a Testes.

### ❌ Negativas
* **Esforço Inicial:** Exige mais tempo para configurar diversos frameworks e ambientes.
* **Manutenção:** Mudanças estruturais podem exigir atualizações em múltiplos níveis de teste.
* **Curva de Aprendizado:** A equipe precisa dominar diferentes ferramentas (ex: TestContainers, Cucumber).

---

## Implementação Detalhada

| Nível | Escopo | Meta de Cobertura |
| :--- | :--- | :--- |
| **Unitário** | Modelos, Services, Regras de Negócio | > 80% das classes de negócio |
| **Integração** | Repositórios, Consumidores de Eventos | 100% dos adaptadores externos |
| **API** | Endpoints REST, Validações, HTTP Status | 100% dos endpoints expostos |
| **Componente** | O serviço completo com contexto Spring | Fluxos principais de cada serviço |
| **End-to-End** | Fluxo crítico (ex: Transação -> Saldo Diário) | Caminhos felizes e exceções críticas |

---

## Observações
Esta estratégia é um investimento na **velocidade sustentável** de desenvolvimento. Embora o custo inicial de escrita seja maior, o custo de manutenção e a incidência de bugs em produção serão drasticamente reduzidos.

> **Nota:** A estratégia será revisada trimestralmente para ajustes baseados na performance da pipeline de CI/CD e estabilidade dos testes.