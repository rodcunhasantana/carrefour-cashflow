/**
 * Carrefour Cashflow — Teste de Performance
 *
 * NFR-03 (Resiliência/Circuit Breaker): valida que o sistema mantém estabilidade sob carga
 *
 * Cenário: happy path do fluxo principal
 *   1. POST /api/transactions  → criar crédito
 *   2. POST /api/transactions  → criar débito
 *   3. GET  /api/transactions  → listar transações
 *   4. GET  /api/dailybalances/{date} → consultar saldo (pode ser 200 ou 404 — processamento async)
 *
 * Serviços alvo (container names via rede Docker/Podman Compose):
 *   transaction-service   → http://transaction-service:8080
 *   dailybalance-service  → http://dailybalance-service:8081
 *
 * Para rodar (da raiz do projeto, com serviços já rodando):
 *   podman compose -f infrastructure/docker/docker-compose.yml --profile perf run --rm k6
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// ── Métricas customizadas ──────────────────────────────────────────────────
const errorRate        = new Rate('errors');
const createLatency    = new Trend('create_transaction_duration', true);
const listLatency      = new Trend('list_transactions_duration', true);
const balanceLatency   = new Trend('get_balance_duration', true);

// ── Configuração do teste ──────────────────────────────────────────────────
export const options = {
    stages: [
        { duration: '30s', target: 20 },   // ramp-up  → 20 VUs
        { duration: '1m',  target: 20 },   // plateau  → 20 VUs durante 1 min
        { duration: '15s', target: 0  },   // ramp-down
    ],
    thresholds: {
        // NFR: p95 < 500 ms em baseline local (sem network hop de Cloud Run/Cloud SQL)
        http_req_duration:               ['p(95)<500'],
        // NFR: < 1% de erros em todo o teste
        http_req_failed:                 ['rate<0.01'],
        errors:                          ['rate<0.01'],
        // Thresholds por endpoint
        create_transaction_duration:     ['p(95)<400'],
        list_transactions_duration:      ['p(95)<300'],
        get_balance_duration:            ['p(95)<200'],
    },
};

// ── Constantes ─────────────────────────────────────────────────────────────
const BASE_TS  = 'http://transaction-service:8080/transaction-service';
const BASE_DBS = 'http://dailybalance-service:8081/dailybalance-service';
const HEADERS  = {
    'X-API-Key':    'cashflow-local-key',
    'Content-Type': 'application/json',
};

// Data no futuro — sem risco de período fechado de testes anteriores
const TEST_DATE = '2026-12-01';

// ── Fluxo por VU ───────────────────────────────────────────────────────────
export default function () {

    // 1. Criar crédito
    const creditAmount = (randomIntBetween(100, 5000) / 100).toFixed(2);
    const creditRes = http.post(
        `${BASE_TS}/api/transactions`,
        JSON.stringify({
            type:        'CREDIT',
            amount:      parseFloat(creditAmount),
            date:        TEST_DATE,
            description: `k6 credito vu${__VU} iter${__ITER}`,
        }),
        { headers: HEADERS }
    );
    createLatency.add(creditRes.timings.duration);
    errorRate.add(!check(creditRes, {
        'POST /transactions (CREDIT) → 201': (r) => r.status === 201,
    }));

    // 2. Criar débito
    const debitAmount = (randomIntBetween(10, 200) / 100).toFixed(2);
    const debitRes = http.post(
        `${BASE_TS}/api/transactions`,
        JSON.stringify({
            type:        'DEBIT',
            amount:      -parseFloat(debitAmount),
            date:        TEST_DATE,
            description: `k6 debito vu${__VU} iter${__ITER}`,
        }),
        { headers: HEADERS }
    );
    createLatency.add(debitRes.timings.duration);
    errorRate.add(!check(debitRes, {
        'POST /transactions (DEBIT) → 201': (r) => r.status === 201,
    }));

    // 3. Listar transações
    const listRes = http.get(
        `${BASE_TS}/api/transactions?page=0&size=10`,
        { headers: HEADERS }
    );
    listLatency.add(listRes.timings.duration);
    errorRate.add(!check(listRes, {
        'GET /transactions → 200': (r) => r.status === 200,
    }));

    // 4. Consultar saldo diário
    //    Aceita 200 (saldo já processado via Pub/Sub) ou 404 (ainda em processamento async)
    const balRes = http.get(
        `${BASE_DBS}/api/dailybalances/${TEST_DATE}`,
        { headers: HEADERS }
    );
    balanceLatency.add(balRes.timings.duration);
    errorRate.add(!check(balRes, {
        'GET /dailybalances/{date} → 200 ou 404': (r) => r.status === 200 || r.status === 404,
    }));

    sleep(1);
}
