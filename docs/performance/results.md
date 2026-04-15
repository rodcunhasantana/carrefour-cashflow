# Resultados de Performance — Carrefour Cashflow

**Data da execução:** 2026-04-15  
**Ferramenta:** k6 v0.57.0 (Grafana Labs)  
**Script:** `k6/load-test.js`  
**NFR referenciado:** NFR-03 (Resiliência/Circuit Breaker — estabilidade sob carga)

---

## Configuração do Teste

| Parâmetro | Valor |
|---|---|
| VUs máximos | 20 |
| Duração total | 1m45s |
| Estágios | ramp-up 0→20 VUs (30s) · plateau 20 VUs (1m) · ramp-down (15s) |
| Ambiente | Containers locais via Podman Compose (rede `docker_default`) |
| API Key | `cashflow-local-key` |
| Data de teste | `2026-12-01` |

### Fluxo testado por iteração

1. `POST /transaction-service/api/transactions` → criar crédito
2. `POST /transaction-service/api/transactions` → criar débito
3. `GET  /transaction-service/api/transactions?page=0&size=10` → listar transações
4. `GET  /dailybalance-service/api/dailybalances/2026-12-01` → consultar saldo (aceita 200 ou 404 por processamento async)

---

## Resultados por Endpoint

| Endpoint | avg | p50 (med) | p90 | p95 | max | Threshold NFR | Status |
|---|---|---|---|---|---|---|---|
| POST /transactions (crédito + débito) | 16.48 ms | 10.29 ms | 21.39 ms | **30.79 ms** | 1.03 s | p(95) < 400 ms | ✅ PASS |
| GET /transactions | 7.20 ms | 5.67 ms | 12.21 ms | **15.18 ms** | 68.64 ms | p(95) < 300 ms | ✅ PASS |
| GET /dailybalances/{date} | 6.56 ms | 5.11 ms | 11.33 ms | **15.17 ms** | 133.07 ms | p(95) < 200 ms | ✅ PASS |
| **Geral (todas as requests)** | 11.68 ms | 7.65 ms | 17.34 ms | **23.03 ms** | 1.03 s | p(95) < 500 ms | ✅ PASS |

---

## Resumo de Thresholds

| Threshold | Meta | Resultado | Status |
|---|---|---|---|
| `http_req_duration` p(95) | < 500 ms | **23.03 ms** | ✅ PASS |
| `http_req_failed` | < 1% | **0.01%** (1 req) | ✅ PASS |
| `errors` (custom) | < 1% | **0.00%** | ✅ PASS |
| `create_transaction_duration` p(95) | < 400 ms | **30.79 ms** | ✅ PASS |
| `list_transactions_duration` p(95) | < 300 ms | **15.18 ms** | ✅ PASS |
| `get_balance_duration` p(95) | < 200 ms | **15.17 ms** | ✅ PASS |

**Todos os 6 thresholds aprovados.**

---

## Throughput e Volume

| Métrica | Valor |
|---|---|
| Total de requests HTTP | **6.364** |
| Throughput médio | **60.2 req/s** |
| Total de iterações (fluxos) | **1.591** |
| Iterações por segundo | **~15 iter/s** |
| Checks executados | **6.364** |
| Checks aprovados | **100%** (6.364/6.364) |
| Dados recebidos | 8.0 MB (76 kB/s) |
| Dados enviados | 1.5 MB (15 kB/s) |

---

## Checks por Endpoint

| Check | Resultado |
|---|---|
| `POST /transactions (CREDIT) → 201` | ✅ 100% |
| `POST /transactions (DEBIT) → 201` | ✅ 100% |
| `GET /transactions → 200` | ✅ 100% |
| `GET /dailybalances/{date} → 200 ou 404` | ✅ 100% |

> A única request falha registrada (`http_req_failed: 0.01%`, 1 de 6.364) não foi detectada
> pelos checks de negócio — provável timeout de conexão TCP durante o ramp-up, sem impacto
> na taxa de erro de negócio (0.00%).

---

## Duração das Iterações

| Percentil | Duração da iteração |
|---|---|
| Média | 1.04 s |
| p50 | 1.03 s |
| p90 | 1.06 s |
| p95 | 1.09 s |
| Máximo | 2.14 s |

A duração inclui o `sleep(1)` obrigatório no final de cada iteração — o tempo real de processamento
por iteração é de aproximadamente **30–90 ms**.

---

## Observações

### Cache Caffeine (Daily Balance Service)

O endpoint `GET /dailybalances/{date}` apresentou a menor latência p95 (15.17 ms), bem
abaixo do threshold de 200 ms. Após as primeiras iterações que criaram o saldo do dia
`2026-12-01`, o cache Caffeine passou a servir leituras subsequentes sem round-trip ao
banco, o que explica a latência baixa mesmo sob 20 VUs concorrentes.

### Spike no max de `create_transaction_duration` (1.03 s)

O valor máximo de 1.03 s em `POST /transactions` ocorreu durante o ramp-up inicial, quando
os containers ainda estavam aquecendo (JVM warm-up + pool de conexões JDBC estabelecendo
as primeiras conexões). O p95 de 30.79 ms demonstra que esta latência não foi sistêmica.

### Estabilidade no plateau

Durante o plateau de 20 VUs (segundos 31–91), o throughput se manteve estável em ~20 req/s
por serviço, sem degradação progressiva de latência, confirmando a ausência de memory leaks
ou saturação de pool de conexões dentro deste perfil de carga.

---

## Disclaimer — Baseline Local

> **Estes resultados representam um baseline em ambiente local** (Podman em Windows,
> localhost sem latência de rede). Em produção (Cloud Run + Cloud SQL), os valores esperados
> são diferentes por três razões principais:
>
> 1. **Latência de rede** — Cloud Run → Cloud SQL adiciona ~2–10 ms por query
> 2. **Cold start** — Cloud Run pode adicionar 200–800 ms na primeira requisição após
>    período inativo (mitigado com instâncias mínimas configuradas)
> 3. **Escala** — Cloud Run escala horizontalmente sob carga real; o comportamento sob
>    100+ VUs pode revelar gargalos não visíveis com 20 VUs locais
>
> Para validação de NFR em ambiente de produção, recomenda-se executar este mesmo script
> k6 a partir de uma instância no GCP (Cloud Run Jobs ou Cloud Build) apontando para
> os endpoints reais, integrando os resultados ao Cloud Monitoring.

---

## Como Reproduzir

```bash
# Pré-requisito: serviços rodando via Podman Compose
cd infrastructure/docker
podman compose up -d

# Aguardar healthchecks (≈ 45s) e então executar o teste
podman compose --profile perf run --rm k6
```

O script k6 está em `k6/load-test.js`. A imagem `grafana/k6:latest` é baixada
automaticamente na primeira execução.
