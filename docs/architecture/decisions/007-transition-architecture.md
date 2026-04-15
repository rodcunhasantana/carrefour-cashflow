# ADR 007: Arquitetura de Transição — Migração de Legado

## Status
📋 **Proposto**

---

## Contexto

O desafio técnico descreve um comerciante que precisa controlar seu fluxo de caixa diário. Na prática, esse comerciante muito provavelmente já possui algum mecanismo de controle financeiro em operação — seja uma planilha Excel, um sistema ERP monolítico, ou uma solução legada desenvolvida internamente. A arquitetura alvo deste projeto (dois microsserviços, Pub/Sub, database-per-service) representa o estado **To-Be**.

Este ADR descreve a **Arquitetura de Transição**: como migrar do estado legado (As-Is) para a arquitetura alvo sem interromper o negócio.

---

## Estado Atual Hipotético (As-Is)

Para fins de planejamento, considera-se o seguinte perfil de legado típico para um sistema de controle financeiro de comerciante:

| Aspecto | Estado Legado |
|---|---|
| **Modelo** | Monolito com banco de dados centralizado |
| **Persistência** | Tabela única `movimentos_financeiros` (tipo, valor, data, descrição) |
| **Consolidado** | Job batch noturno que calcula saldo do dia anterior |
| **Interface** | Tela desktop ou planilha — sem API REST |
| **Integração** | Exportação manual por CSV ou FTP |
| **Observabilidade** | Logs de arquivo em servidor local, sem tracing |
| **Escalabilidade** | Vertical apenas — escala comprando hardware maior |

**Problemas gerados por este modelo:**
- O relatório de saldo diário está sempre defasado (calculado no dia seguinte)
- Uma falha no sistema impede tanto lançamentos quanto consultas de saldo
- Escalar o consolidado exige escalar o sistema inteiro
- Sem API, integrações com ERP ou BI são manuais e propensas a erro

---

## Decisão

Adotar o padrão **Strangler Fig** como estratégia de migração, complementado por uma **Anti-Corruption Layer (ACL)** durante o período de coexistência.

### Por que Strangler Fig?

- Elimina o risco de *big bang migration* — não há janela de parada planejada
- Permite validação incremental: o novo sistema processa dados reais antes do cutover completo
- Rollback é possível em qualquer fase — o legado permanece funcional até a quarentena final
- Alinha com a realidade financeira: operações de crédito e débito não podem parar

---

## Fases de Transição

### Fase 1 — Coexistência e Exposição de API (semanas 1–4)

O legado continua sendo o sistema de registro (*system of record*). O novo sistema é implantado em paralelo, mas ainda não recebe tráfego de produção.

```
┌─────────────────────────────────────────────────────┐
│                   FASE 1                             │
│                                                      │
│  Usuário → [Legado]  (system of record)              │
│                ↓                                     │
│           [ACL / Adapter]  ←── lê e transforma       │
│                ↓                                     │
│     [Transaction Service]  (modo shadow / leitura)   │
│     [Daily Balance Service]                          │
└─────────────────────────────────────────────────────┘
```

**Ações:**
- Implantar os dois novos serviços em ambiente de produção (Cloud Run)
- Implementar a **Anti-Corruption Layer**: um job ou serviço que lê `movimentos_financeiros` do legado e publica eventos no formato `TransactionCreatedEvent` no Pub/Sub
- O Daily Balance Service começa a construir saldos a partir dos eventos — em paralelo ao batch legado
- Comparar saldos calculados pelo legado vs. novo sistema (reconciliação diária)

**Critério de saída:** Divergência entre saldos legado vs. novo < 0,01% por 7 dias consecutivos.

---

### Fase 2 — Dual Write (semanas 5–8)

Novos lançamentos são escritos **simultaneamente** no legado e no novo sistema. O legado ainda é o sistema de registro, mas o novo começa a ser validado em tempo real.

```
┌─────────────────────────────────────────────────────┐
│                   FASE 2                             │
│                                                      │
│  Usuário → [ACL / API Gateway]                       │
│                 ↙            ↘                       │
│          [Legado]    [Transaction Service]            │
│          (write)     (write + Pub/Sub event)         │
│                              ↓                       │
│                   [Daily Balance Service]            │
└─────────────────────────────────────────────────────┘
```

**Ações:**
- A ACL passa a interceptar criação de lançamentos e escrever nos dois sistemas
- Job de reconciliação diária compara saldos entre `movimentos_financeiros` (legado) e `daily_balances` (novo)
- Alertas automáticos caso divergência > 0% sejam detectados
- Equipe de negócio valida consultas de saldo pelo novo sistema em paralelo

**Critério de saída:** Zero divergências em 14 dias + aprovação da área financeira.

---

### Fase 3 — Migração de Dados Históricos (semanas 7–9, paralela à Fase 2)

Enquanto o dual write está em produção, migrar o histórico do legado para o novo modelo.

```
Legado: movimentos_financeiros
    ↓
[Script de migração — via Flyway ou job dedicado]
    ↓
transactions (transaction-service)
daily_balances (dailybalance-service)
daily_balance_transactions (auditoria)
```

**Ações:**
- Mapear campos do legado para o modelo novo (ex: `tipo` → `TransactionType`, `valor` → `amount` com sinal correto para DEBIT)
- Migrar em lotes por data (do mais antigo para o mais recente)
- Fechar períodos históricos via `POST /api/dailybalances/{date}/close` para os dias já consolidados
- Validar somas: `SUM(amount WHERE type=CREDIT)` legado = `total_credits` do novo

**Ferramentas:** Flyway para versionar o schema; script Java ou SQL para transformação de dados.

---

### Fase 4 — Cutover (semana 10)

O novo sistema passa a ser o único *system of record*. O legado entra em modo somente-leitura.

```
┌─────────────────────────────────────────────────────┐
│                   FASE 4                             │
│                                                      │
│  Usuário → [Transaction Service]  (system of record) │
│                  ↓ Pub/Sub                           │
│          [Daily Balance Service]                     │
│                                                      │
│  [Legado] → read-only por 30 dias → descomissionado  │
└─────────────────────────────────────────────────────┘
```

**Ações:**
- Redirecionar 100% do tráfego para o novo sistema (via API Gateway ou DNS)
- Desativar escrita no legado (permissão `REVOKE INSERT, UPDATE, DELETE`)
- Manter legado acessível para consultas históricas por 30 dias (quarentena)
- Monitorar Cloud Monitoring + alertas de erro por 72h após cutover
- Descomissionar legado após período de quarentena sem incidentes

**Critério de saída:** 72h sem alertas de erro + confirmação da área financeira.

---

## Anti-Corruption Layer (ACL)

A ACL é o componente central da transição. Sua responsabilidade é **impedir que o modelo do legado contamine o domínio novo**.

```
Legado                        ACL                      Novo Domínio
──────────────────────────────────────────────────────────────────
movimentos_financeiros    →   LegacyTransactionAdapter  →  TransactionCreatedEvent
  tipo: 'C' / 'D'              mapTipo(tipo)               type: CREDIT / DEBIT
  valor: sempre positivo        applySign(tipo, valor)      amount: positivo/negativo
  data_movimento                parseDate(data)             date: LocalDate
  historico                     mapDescription(historico)   description: String
```

A ACL pode ser implementada como:
- **Job batch** (mais simples): roda a cada N minutos, lê registros novos do legado e publica eventos
- **Change Data Capture (CDC)**: usa Debezium para capturar mudanças no banco do legado em tempo real e publicar no Pub/Sub — recomendado para volumes maiores

---

## Diagrama de Estados da Transição

```
[As-Is: Legado]
      │
      ▼
[Fase 1: Coexistência + ACL shadow]
      │ 7 dias sem divergência
      ▼
[Fase 2: Dual Write + reconciliação]
      │ 14 dias sem divergência
      ▼   ← paralelo →   [Fase 3: Migração histórica]
      │
      ▼
[Fase 4: Cutover → legado read-only]
      │ 30 dias quarentena
      ▼
[To-Be: Legado descomissionado]
```

---

## Riscos e Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Divergência de saldos durante dual write | Médio | Alto | Reconciliação diária automatizada com alerta imediato |
| Performance da ACL atrasando lançamentos | Baixo | Médio | ACL assíncrona via Pub/Sub — não bloqueia o caminho crítico |
| Dados históricos com formato inconsistente | Alto | Médio | Script de migração com validação linha a linha e relatório de rejeições |
| Rollback necessário após cutover | Baixo | Alto | Legado em read-only por 30 dias + feature flag para reverter roteamento |
| Resistência dos usuários ao novo sistema | Médio | Médio | Período de uso paralelo nas Fases 2 e 3 — usuários validam antes do cutover |

---

## O que NÃO muda na Arquitetura Alvo

A arquitetura alvo (ADR-001 a ADR-006) permanece intacta. A ACL fica **na borda**, nunca dentro dos serviços. O domínio `Transaction` e `DailyBalance` não têm conhecimento de que um legado existiu — princípio de isolamento da Arquitetura Hexagonal.

---

## Consequências

### ✅ Positivas
- **Zero downtime**: o negócio continua operando em todas as fases
- **Reversível**: rollback possível até o descomissionamento do legado
- **Validação progressiva**: o novo sistema é testado com dados reais antes de assumir o controle
- **Histórico preservado**: dados do legado são migrados e auditáveis no novo modelo

### ❌ Negativas / Custos
- **Complexidade operacional temporária**: dois sistemas em produção ao mesmo tempo por ~10 semanas
- **Custo de infraestrutura duplo** durante a transição (~$150-200 extras no período)
- **Esforço da ACL**: desenvolvimento e manutenção do adaptador durante as fases 1 e 2

---

## Observações

Este ADR descreve uma estratégia de transição **genérica e realista** para o perfil de legado mais comum em controle de fluxo de caixa de comerciantes. Em um projeto real, o As-Is seria levantado em detalhe durante a fase de descoberta, podendo revelar variações (ex: múltiplas planilhas por filial, integração com TEF, etc.) que ajustariam o plano de migração sem alterar o padrão arquitetural escolhido.

> **Referências de padrão:** Martin Fowler — *Strangler Fig Application* · Eric Evans — *Anti-Corruption Layer* (DDD)
