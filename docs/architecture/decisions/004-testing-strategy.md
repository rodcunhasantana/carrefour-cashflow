# ADR 004: Estratégia de Testes

## Status
✅ **Aceito**

---

## Contexto
O sistema **Carrefour Cashflow** requer uma estratégia de testes abrangente para garantir a qualidade do software e a correta implementação das regras de negócio. Com a arquitetura de microsserviços adotada, precisamos de uma abordagem que cubra desde componentes isolados até a integração completa entre serviços e contratos de API.

---

## Decisão
Adotamos uma estratégia de testes em múltiplas camadas, seguindo o modelo da **Pirâmide de Testes**, priorizando testes rápidos e isolados na base e testes mais complexos no topo.

### Camadas implementadas

1. **Testes Unitários de Domínio** — JUnit 5 puro, sem Spring, sem mocks de framework. Cobrem entidades, value objects e regras de negócio.
2. **Testes Unitários de Serviço** — JUnit 5 + Mockito. Cobrem application services com dependências mockadas.
3. **Testes de Integração JDBC** — H2 in-memory com schema idêntico ao PostgreSQL (`schema-test.sql`). Cobrem os repositórios com SQL real.
4. **Testes Web (MockMvc standalone)** — Spring MockMvc sem Spring Security aplicado. Cobrem os controllers REST, validações e status HTTP.
5. **Smoke Test de Contexto** — `@SpringBootTest` verifica inicialização completa do contexto Spring (incluindo Security, Pub/Sub mockado, tracing).

### Estado atual

- **Total de testes:** 117 (0 falhas, 0 skips)
- **Execução:** `mvn clean install` (todos os módulos)

---

## Tecnologias e Frameworks

| Camada | Tecnologia |
|---|---|
| Unitários de domínio | JUnit 5 puro |
| Unitários de serviço | JUnit 5 + Mockito |
| Integração JDBC | JUnit 5 + H2 in-memory (`MODE=PostgreSQL`) |
| Web / REST | Spring MockMvc (standalone) |
| Smoke test | `@SpringBootTest` + `application-test.yml` |

> **Nota sobre TestContainers:** O ADR original previa TestContainers para testes de integração com banco real. Na implementação atual, optou-se por H2 in-memory com `MODE=PostgreSQL` por ser mais rápido na pipeline local e não exigir Docker durante os testes. TestContainers pode ser adotado em uma fase posterior para maior fidelidade ao PostgreSQL.

---

## Configuração dos Testes

Ambos os serviços usam `application-test.yml` com:
- H2 in-memory (`jdbc:h2:mem:...;MODE=PostgreSQL`)
- Schema inicializado via `schema-test.sql` (idêntico ao schema PostgreSQL)
- Pub/Sub desabilitado (`spring.cloud.gcp.pubsub.enabled: false`)
- Auto-configurações excluídas: JPA, GCP Context, `UserDetailsServiceAutoConfiguration`
- API Key de teste: `test-api-key`
- Resilience4j: Circuit Breaker sem health indicator, Retry com `maxAttempts: 1`

---

## Alternativas Consideradas

### 1. Foco apenas em Testes End-to-End
- **Vantagens:** Validação real da experiência do usuário.
- **Desvantagens:** Execução lenta, testes frágeis ("flaky tests") e dificuldade em diagnosticar a causa raiz de falhas.

### 2. Abordagem "Testing Trophy" (Foco em Integração)
- **Vantagens:** Equilíbrio entre custo e confiança, menos uso de mocks.
- **Desvantagens:** Testes mais pesados que os unitários, exigindo mais infraestrutura.

---

## Consequências

### ✅ Positivas
- **Feedback Rápido:** Testes unitários detectam erros em segundos durante o desenvolvimento.
- **Sem dependências externas:** Testes rodam sem Docker, sem banco externo, sem emulador Pub/Sub.
- **Confiança:** Camadas superiores garantem que a integração entre peças móveis funciona.
- **Granularidade:** Facilita a identificação exata de onde o bug foi introduzido.

### ❌ Negativas / Limitações
- **H2 vs PostgreSQL:** Algumas features específicas do PostgreSQL (ex: tipos customizados, índices parciais) não são cobertas pelos testes de integração com H2.
- **Sem testes E2E automatizados:** Os fluxos cross-service (Transaction → Pub/Sub → Daily Balance) são validados manualmente ou via testes de fumaça no ambiente Docker.

---

## Executando os Testes

```bash
# Todos os módulos
mvn clean install

# Por serviço
mvn test -pl transaction-service
mvn test -pl dailybalance-service

# Com relatório de cobertura (Surefire)
mvn verify -pl transaction-service
```

---

## Observações
Esta estratégia é um investimento na **velocidade sustentável** de desenvolvimento. Embora o custo inicial de escrita seja maior, o custo de manutenção e a incidência de bugs em produção são drasticamente reduzidos. A estratégia será revisada à medida que o projeto evoluir para produção, podendo incluir TestContainers e testes de contrato (Pact) quando múltiplas equipes consumirem os eventos.
