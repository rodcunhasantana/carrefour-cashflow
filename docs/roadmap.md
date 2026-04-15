# Roadmap de Evoluções — Carrefour Cashflow

Evoluções priorizadas pelo valor que entregam ao negócio. Para cada item: o que o negócio ganha, qual problema resolve hoje e como será implementado.

---

## 1. Entrega Contínua com Cloud Build

### O que o negócio ganha

**Hoje:** qualquer atualização do sistema — seja uma correção de bug ou uma nova funcionalidade — depende de alguém executar comandos manualmente, construir a imagem Docker e fazer o deploy no Cloud Run. Isso cria um gargalo humano, aumenta o risco de erro e torna difícil rastrear o que está em produção e quando foi colocado.

**Com Cloud Build:**
- Cada mudança aprovada no repositório é testada, construída e publicada automaticamente — sem intervenção manual
- Todo deploy é rastreável: qual commit, por quem, em qual horário, com quais testes passando
- Problemas de qualidade (bugs, vulnerabilidades de segurança) são detectados antes de chegar à produção
- O time pode entregar melhorias com muito mais frequência e segurança, sem cerimônia de deploy

### Como funciona

Três etapas automáticas no **Cloud Build** (serviço GCP nativo — sem dependência de ferramentas externas):

| Etapa | Quando dispara | O que faz |
|---|---|---|
| **Validação** | A cada Pull Request | Roda os 121 testes; bloqueia o merge se algum falhar |
| **Build & Publicação** | A cada merge na `main` | Constrói as imagens Docker e publica no **Artifact Registry** |
| **Deploy** | Após publicação (com aprovação opcional) | Atualiza os serviços no **Cloud Run** |

Os Dockerfiles já existem e usam multi-stage build — o Cloud Build os reutiliza sem modificação.

**Análise de qualidade de código** integrada via SonarCloud: detecta vulnerabilidades, código duplicado e regressões de cobertura de testes diretamente no Pull Request, antes do merge.

**Serviços GCP envolvidos:** Cloud Build, Artifact Registry, Cloud Run (já em uso).

---

## 2. Relatórios Financeiros

### O que o negócio ganha

**Hoje:** os dados de fluxo de caixa estão no sistema mas não há como exportá-los. Qualquer necessidade de relatório — fechamento mensal, auditoria, conciliação com o ERP — exige acesso direto ao banco de dados ou extração manual.

**Com o relatório implementado:**
- A área financeira consulta e exporta o consolidado de qualquer período diretamente pela API, sem depender de TI
- Relatórios de fechamento periódico (diário, semanal, mensal) podem ser gerados sob demanda ou agendados
- Arquivos ficam disponíveis no **Cloud Storage** com histórico auditável — quem gerou, quando, qual período
- URLs de acesso expiram automaticamente, impedindo que relatórios com dados financeiros fiquem expostos indefinidamente

### Como funciona

Duas modalidades, entregues em sequência:

**Fase 1 — Download imediato:**
```
GET /api/dailybalances/report?from=2025-01-01&to=2025-01-31
→ Arquivo CSV entregue diretamente como download
```
Retorna colunas: data, saldo de abertura, créditos, débitos, saldo de fechamento, status do período.

**Fase 2 — Relatório persistido com histórico:**
```
POST /api/dailybalances/report  →  202 Accepted + { "jobId": "..." }
GET  /api/dailybalances/report/{jobId}  →  { "status": "READY", "url": "..." }
```
O arquivo é salvo no **Cloud Storage** (bucket `cashflow-reports`). A URL de acesso expira em 1 hora. Cada geração fica registrada com quem solicitou e quando.

**Serviços GCP envolvidos:** Cloud Storage (Fase 2).

---

## 3. Evolução Segura do Banco de Dados com Flyway

### O que o negócio ganha

**Hoje:** quando o sistema precisa de uma mudança no banco de dados (adicionar um campo, criar um índice, ajustar uma constraint), não há um processo controlado. A mudança precisa ser aplicada manualmente por alguém com acesso ao banco, fora do deploy normal. Isso cria risco de inconsistência — banco de produção em um estado diferente do que o código espera.

**Com Flyway:**
- Toda mudança no banco é parte do código: passa pelos mesmos processos de revisão, teste e deploy
- O sistema verifica automaticamente se o banco está na versão correta antes de subir
- É possível saber exatamente qual versão do schema está em cada ambiente (dev, staging, produção)
- Mudanças são aplicadas com zero downtime — Flyway executa os scripts na ordem certa, sem intervenção manual
- Em caso de problema, o histórico completo de mudanças está disponível para auditoria

### Como funciona

Cada alteração no banco vira um arquivo versionado dentro do próprio repositório:

```
db/migration/
├── V1__create_daily_balances.sql
├── V2__create_daily_balance_transactions.sql
├── V3__create_processed_events.sql
└── V4__...próxima_mudança.sql   ← adicionado pelo time quando necessário
```

Na inicialização, o Flyway aplica automaticamente apenas os scripts ainda não executados naquele ambiente. O histórico fica registrado na tabela `flyway_schema_history` do próprio banco.

---

## 4. Observabilidade com Cloud Monitoring e Cloud Trace

### O que o negócio ganha

**Hoje:** quando algo dá errado em produção, a investigação depende de leitura manual de logs. Não há dashboards, não há alertas automáticos, não há visibilidade proativa sobre a saúde do sistema.

**Com Cloud Monitoring e Cloud Trace:**
- Problemas são detectados e alertados automaticamente — antes que o usuário perceba
- É possível rastrear uma transação financeira específica do início ao fim entre os dois serviços, com o tempo exato de cada etapa
- O time de negócio tem visibilidade em tempo real: quantas transações por hora, qual a taxa de erros, se o fechamento de período está travado
- Conformidade facilitada: logs estruturados + traces completos são evidência auditável de que cada operação financeira foi processada corretamente
- Custo do Cloud Trace estimado em $0/mês para o volume atual (ver [cost-estimation.md](architecture/cost-estimation.md))

### O que será monitorado

| Indicador | Alerta automático |
|---|---|
| Taxa de erro por endpoint | > 1% por 5 minutos |
| Latência p95 por operação | > 500ms |
| Mensagens não processadas na fila (DLQ) | > 0 por 10 minutos |
| Eficiência do cache de saldos | Hit rate < 50% por 5 minutos |

O rastreamento distribuído via **Cloud Trace** conecta automaticamente uma requisição HTTP ao evento Pub/Sub que ela gerou, permitindo ver o caminho completo: `POST /transactions → evento Pub/Sub → atualização do saldo diário`.

**Serviços GCP envolvidos:** Cloud Monitoring, Cloud Trace, Cloud Logging (já em uso).

---

## 5. Desenvolvimento e Evolução com IA

### O que o negócio ganha

**Hoje:** adicionar um novo endpoint, escrever testes ou atualizar documentação são tarefas que consomem tempo de desenvolvimento mesmo quando seguem padrões já bem estabelecidos no projeto.

**Com ferramentas de IA integradas ao fluxo:**
- Novas funcionalidades são implementadas mais rápido — a IA conhece os padrões do projeto (arquitetura hexagonal, convenções de teste, estrutura dos serviços) e gera código consistente
- Bugs introduzidos são detectados mais cedo, ainda no Pull Request, antes de chegarem à produção
- Dependências desatualizadas (que podem conter vulnerabilidades de segurança) são atualizadas automaticamente via Pull Requests gerados pelo Dependabot, sem esforço manual
- O time consegue manter maior velocidade de entrega com o mesmo tamanho de equipe

### Ferramentas e papéis

| Ferramenta | Papel no fluxo |
|---|---|
| **Claude Code** | Geração de código e testes alinhados com os padrões do projeto; atualização de documentação após mudanças; análise de impacto de novas funcionalidades |
| **GitHub Copilot** | Autocompletar inline no IDE durante desenvolvimento — especialmente em código repetitivo (mapeamentos, DTOs, testes de repositório) |
| **SonarCloud** | Qualidade e segurança contínuas no Pull Request — detecta vulnerabilidades, código duplicado e regressões de cobertura antes do merge |
| **Dependabot** | Pull Requests automáticos semanais para atualizar dependências (Spring Boot, Caffeine, Resilience4j) — mantém o projeto seguro sem esforço manual |

### Fluxo com IA

```
Nova funcionalidade solicitada
  → Claude Code: implementa seguindo padrões hexagonais + gera testes
  → Pull Request aberto → Cloud Build: roda testes automaticamente
  → SonarCloud: analisa qualidade e segurança
  → Aprovação humana → merge → deploy automático via Cloud Build
```

---

## Sumário

| Evolução | Ganho principal para o negócio | Esforço |
|---|---|---|
| Cloud Build (CI/CD) | Deploys seguros, rastreáveis e sem intervenção manual | Médio |
| Relatórios financeiros | Visibilidade e exportação de dados sem depender de TI | Baixo → Médio |
| Flyway | Evolução do banco sem downtime e com histórico auditável | Baixo |
| Cloud Monitoring + Trace | Detecção proativa de problemas e rastreabilidade financeira | Baixo–Médio |
| IA no desenvolvimento | Maior velocidade de entrega com menor risco de regressão | Baixo (configuração) |
