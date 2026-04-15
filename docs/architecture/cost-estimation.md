# Estimativa de Custo — Google Cloud Platform

Estimativa mensal para o sistema **Carrefour Cashflow** rodando em produção no GCP. Os valores são baseados nos preços públicos da GCP (região `southamerica-east1` — São Paulo) e em premissas de volume realistas para um sistema de fluxo de caixa corporativo interno.

> **Importante:** Este é um documento de referência para planejamento, não uma fatura. Preços variam com negociação de contrato, committed-use discounts e descontos de uso sustentado (SUDs, aplicados automaticamente pelo Cloud Run e Cloud SQL).

---

## Premissas de Volume

| Premissa | Valor | Justificativa |
|---|---|---|
| Transações por dia | ~2.000 | Operação corporativa média |
| Transações por mês | ~44.000 | 22 dias úteis |
| Pico de requisições/segundo | ~10 req/s | Horário de fechamento do caixa |
| Requisições HTTP/mês (ambos os serviços) | ~200.000 | Inclui consultas, criações, health checks |
| Eventos Pub/Sub/mês | ~100.000 | Cada transação gera 1 evento + DLQ overhead |
| Tamanho médio de mensagem Pub/Sub | ~1 KB | JSON de TransactionCreatedEvent |
| Tamanho da base de dados (transaction-db) | ~5 GB/ano | 44k rows/mês × 12 × ~2 KB/row |
| Tamanho da base de dados (dailybalance-db) | ~1 GB/ano | Saldos diários + auditoria (~3× rows de transações) |
| Logs gerados/mês | ~2 GB | Logback JSON structured, 100% tracing sampling |
| Instâncias Cloud Run (média) | 1 por serviço | Scale-to-zero fora do horário comercial |

---

## Cloud Run

Cada serviço (`transaction-service`, `dailybalance-service`) roda em Cloud Run.

**Configuração estimada por serviço:**
- CPU: 1 vCPU
- Memória: 512 MB → **ver nota de cache abaixo**
- Concorrência máxima: 80 requisições/instância
- Mínimo de instâncias: 0 (scale-to-zero)
- Máximo de instâncias: 3 (proteção contra pico)

**Cálculo mensal (por serviço):**

| Recurso | Consumo estimado | Preço unitário (us-east1) | Subtotal |
|---|---|---|---|
| vCPU-seconds | 50h/mês × 3600 s × 1 vCPU = 180.000 vCPU-s | $0,00002400/vCPU-s | $4,32 |
| Memory GiB-seconds | 180.000 s × 0,5 GiB = 90.000 GiB-s | $0,00000250/GiB-s | $0,23 |
| Requisições | 100.000 req/mês | Primeiros 2M gratuitos | $0,00 |

> **Free tier Cloud Run:** 180.000 vCPU-s/mês, 360.000 GiB-s/mês, 2M requisições/mês — cobertos pelo free tier para volumes deste porte.

**Estimativa Cloud Run (2 serviços):** **~$0 – $10/mês** (dentro do free tier para este volume; custo relevante começa acima de ~500 req/s sustentados).

> #### Impacto do cache Caffeine (Daily Balance Service)
> O `dailybalance-service` inclui um cache Caffeine in-process (`maximumSize=500, expireAfterWrite=10m`). Isso tem dois efeitos opostos no Cloud Run:
>
> **Memória (+):** O cache ocupa espaço no heap JVM. Com `maximumSize=500` entradas de `DailyBalanceDTO` (~1–2 KB cada em Java heap), o overhead é de **~1–2 MB** — irrelevante frente aos 512 MB alocados.
>
> **CPU/tempo de resposta (-):** Consultas `GET /balances/{date}` cacheadas eliminam o round-trip ao PostgreSQL (~5–20 ms). Menos tempo de CPU alocado por requisição = **menos vCPU-seconds consumidos**. Em termos práticos: se 60% das consultas forem cache hit, o consumo de CPU do dailybalance-service cai ~30–40%, mantendo a instância no free tier com mais folga.
>
> **Conclusão de custo:** O cache não adiciona nenhum custo ao Cloud Run e pode reduzir marginalmente o consumo de vCPU-seconds. **Impacto financeiro: neutro a levemente positivo.**

---

## Cloud SQL (PostgreSQL 15)

Dois bancos separados (`transaction-db` e `dailybalance-db`), seguindo o padrão database-per-service do ADR-003.

**Configuração recomendada para produção (por instância):**

| Parâmetro | Valor |
|---|---|
| Tipo de máquina | `db-custom-2-3840` (2 vCPU, 3,75 GB RAM) |
| Storage | SSD 20 GB (auto-expand ativado) |
| Alta Disponibilidade (HA) | Sim (standby em zona diferente) |
| Backups automáticos | Sim, retenção 7 dias |
| Point-in-time recovery | Sim |
| Conexões máximas | ~100 (HikariCP usa 10/serviço) |

**Cálculo mensal (por instância):**

| Recurso | Preço mensal estimado (`southamerica-east1`) |
|---|---|
| Instância `db-custom-2-3840` | ~$70/mês |
| Storage SSD 20 GB | ~$4/mês |
| Alta Disponibilidade (réplica stand-by) | ~$70/mês adicional |
| Backups (7 dias de retenção) | ~$1/mês |
| **Total por instância com HA** | **~$145/mês** |

**Estimativa Cloud SQL (2 instâncias com HA):** **~$290/mês**

> **Otimização:** Para ambientes de desenvolvimento/staging, usar instância compartilhada menor (`db-f1-micro` ou `db-g1-small`, ~$10–20/mês) e sem HA.

> **Nota ADR-003:** O ADR especifica `db-custom-4-15360` (4 vCPU, 15 GB) como spec original. Para o volume estimado acima, `db-custom-2-3840` é suficiente. Se o volume crescer para >10k transações/dia, avaliar upgrade.

> #### Impacto do cache Caffeine no Cloud SQL
> O cache Caffeine no `dailybalance-service` reduz diretamente a carga sobre o `dailybalance-db`:
>
> **Leituras evitadas:** `SELECT * FROM daily_balances WHERE date = ?` é a query mais frequente do sistema — disparada a cada `GET /balances/{date}` e também internamente em cada evento Pub/Sub processado via `findOrCreateForDate`. Com o cache absorvendo hits repetidos na mesma data, o número de reads ao banco cai proporcionalmente à taxa de cache hit.
>
> **Conexões ativas:** Menos queries simultâneas = menos conexões HikariCP ocupadas ao mesmo tempo. Isso alivia o pool de conexões do `dailybalance-db` e reduz o risco de timeout no pico de fechamento de caixa.
>
> **Impacto no custo Cloud SQL:** O Cloud SQL é cobrado por **instância-hora** (não por query), então a redução de leituras não diminui a fatura diretamente. O benefício é **operacional**: a mesma instância `db-custom-2-3840` suporta um volume de tráfego significativamente maior antes de precisar de upgrade — adiando o eventual crescimento para `db-custom-4-7680` (~+$70/mês).
>
> **Conclusão de custo:** Sem impacto na fatura atual. Com crescimento de volume (>5k transações/dia), o cache pode **evitar um upgrade de instância** que custaria ~$70–140/mês adicional.
>
> **O `transaction-db` não é afetado** — o `transaction-service` não tem cache; suas queries são predominantemente writes (INSERT) e reads pontuais por ID, sem perfil de repetição que justifique caching.

---

## Cloud Pub/Sub

4 tópicos, 4 subscriptions (incluindo DLQs `transaction-events-dlq` e `period-events-dlq`).

| Recurso | Volume/mês | Preço | Subtotal |
|---|---|---|---|
| Mensagens publicadas | 100.000 msgs × 1 KB = ~100 MB | Primeiros 10 GB gratuitos | $0,00 |
| Mensagens entregues (subscriptions) | 200.000 entregas × 1 KB = ~200 MB | Primeiros 10 GB gratuitos | $0,00 |
| Retenção de mensagens não confirmadas | ~1 MB | Incluída | $0,00 |

**Estimativa Cloud Pub/Sub:** **$0/mês** (bem dentro do free tier de 10 GB/mês).

> Pub/Sub se torna relevante no custo acima de ~10 GB/mês de dados publicados, o que corresponde a ~10 milhões de mensagens de 1 KB — fora do escopo desta operação.

---

## Secret Manager

Armazena: `APP_API_KEY`, credenciais de banco (`DB_USER`, `DB_PASSWORD`, `DB_URL`) — estimativa de ~6 secrets.

| Recurso | Volume/mês | Preço | Subtotal |
|---|---|---|---|
| Versões ativas de secrets | 6 | $0,06/versão/mês | $0,36 |
| Acesso a secrets (requests) | ~10.000 (startups + health checks) | Primeiros 10k gratuitos | $0,00 |

**Estimativa Secret Manager:** **~$0,36/mês**

---

## Cloud Logging

Logs estruturados JSON gerados pelo Logback de ambos os serviços. Com `sampling.probability: 1.0`, cada requisição gera pelo menos 1 entrada de log com traceId/spanId.

| Recurso | Volume/mês | Preço | Subtotal |
|---|---|---|---|
| Ingestão de logs | ~2 GB | Primeiros 50 GB gratuitos | $0,00 |
| Retenção (_Default_ 30 dias) | ~2 GB | Incluída no free tier | $0,00 |

**Estimativa Cloud Logging:** **$0/mês**

> **Atenção:** `management.tracing.sampling.probability: 1.0` está configurado para desenvolvimento. Em produção com alto volume, reduzir para `0.1` (10%) para evitar custo excessivo de Cloud Trace e reduzir volume de logs.

---

## Cloud Trace (Micrometer Tracing + Brave)

O `micrometer-tracing-bridge-brave` popula `traceId`/`spanId` no MDC mas **não exporta spans para Cloud Trace por padrão** (não há exportador configurado no projeto). Os traces ficam apenas nos logs estruturados.

**Para habilitar Cloud Trace em produção** (opcional): adicionar `spring-cloud-gcp-starter-trace` e configurar `spring.cloud.gcp.trace.enabled: true`.

| Cenário | Volume/mês | Custo estimado |
|---|---|---|
| Sem exportador (configuração atual) | — | $0/mês |
| Com Cloud Trace (100% sampling) | ~200.000 spans | $0,20/mês (primeiros 2,5M gratuitos) |

---

## Artifact Registry

Para armazenar as imagens Docker construídas nos Dockerfiles do projeto.

| Recurso | Volume | Preço | Subtotal |
|---|---|---|---|
| Storage de imagens | ~2 GB (2 imagens ~1 GB cada) | $0,10/GB/mês | $0,20/mês |
| Transferência dentro da mesma região | Irrelevante | Gratuito | $0,00 |

**Estimativa Artifact Registry:** **~$0,20/mês**

---

## Resumo Consolidado

| Serviço GCP | Estimativa Mensal | Observações |
|---|---|---|
| Cloud Run (2 serviços) | $0 – $10 | Free tier para este volume |
| Cloud SQL (2 instâncias, HA) | ~$290 | Maior custo; reduzir em dev/staging |
| Cloud Pub/Sub | $0 | Free tier de 10 GB/mês não atingido |
| Secret Manager | ~$0,36 | Negligível |
| Cloud Logging | $0 | Free tier de 50 GB não atingido |
| Cloud Trace | $0 | Não configurado (apenas MDC local) |
| Artifact Registry | ~$0,20 | Negligível |
| **Total estimado** | **~$290 – $300/mês** | Dominado pelo Cloud SQL |

---

## Otimizações Recomendadas

### ✅ Já implementadas

| Otimização | Componente | Efeito |
|---|---|---|
| **Cache Caffeine in-process** | `dailybalance-service` | Elimina round-trips ao `dailybalance-db` em consultas repetidas; reduz pressão no pool de conexões HikariCP; suporta maior volume sem upgrade de instância Cloud SQL |

### Redução de custo imediata (produção)

1. **Sampling de tracing:** Reduzir `management.tracing.sampling.probability` de `1.0` para `0.1` — reduz volume de logs e eventual custo de Cloud Trace sem impacto funcional.

2. **Cloud SQL em dev/staging:** Usar `db-f1-micro` sem HA para ambientes não-produtivos → ~$10/mês vs $145/mês.

3. **Cloud Run min-instances:** Manter em `0` para ambientes não críticos (scale-to-zero elimina custo em períodos sem tráfego).

### Otimizações para crescimento de volume

4. **Connection pooling externo:** Se o número de instâncias Cloud Run crescer para >5, adicionar **Cloud SQL Auth Proxy + PgBouncer** para evitar estourar o limite de conexões do PostgreSQL. O cache Caffeine já mitiga parte desse risco ao reduzir o número de queries concorrentes no `dailybalance-db`.

5. **Log routing:** Configurar Cloud Logging para rotear logs de DEBUG para um bucket barato (Cold Storage) separado dos logs de INFO/ERROR.

6. **Comprimir mensagens Pub/Sub:** Para volumes >10M mensagens/mês, comprimir o payload JSON antes de publicar reduz o custo de dados e a latência.

---

## Referências de Preço

- [Cloud Run Pricing](https://cloud.google.com/run/pricing)
- [Cloud SQL Pricing](https://cloud.google.com/sql/pricing)
- [Cloud Pub/Sub Pricing](https://cloud.google.com/pubsub/pricing)
- [Secret Manager Pricing](https://cloud.google.com/secret-manager/pricing)
- [Cloud Logging Pricing](https://cloud.google.com/stackdriver/pricing)
- [Artifact Registry Pricing](https://cloud.google.com/artifact-registry/pricing)

> Preços verificados em abril/2026. Confirmar valores atuais no [GCP Pricing Calculator](https://cloud.google.com/products/calculator) antes de usar em planejamento orçamentário.
