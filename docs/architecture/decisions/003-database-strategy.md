# ADR-003: Estratégia de Banco de Dados

## Status

Aceito

## Contexto

O sistema Carrefour Cashflow requer uma estratégia robusta para persistência de dados que atenda às necessidades de confiabilidade, escalabilidade, performance e manutenibilidade. Precisamos selecionar a tecnologia de banco de dados apropriada e o modelo de implantação.

Os principais requisitos para nossa estratégia de banco de dados são:

1. Suporte a transações ACID para garantir integridade financeira
2. Capacidade para lidar com consultas complexas sobre dados históricos
3. Escalabilidade para acomodar crescimento no volume de dados
4. Alta disponibilidade com mínimo downtime
5. Segurança robusta para dados financeiros sensíveis
6. Facilidade de manutenção e backup

## Decisão

Decidimos utilizar **PostgreSQL hospedado no Cloud SQL (Google Cloud Platform)** como nossa solução de banco de dados para o sistema Carrefour Cashflow.

Além disso, manteremos bancos de dados separados para cada microsserviço, seguindo o padrão "database-per-service", com dois bancos principais:

1. **Transaction Database**: Para o Transaction Service
2. **Daily Balance Database**: Para o Daily Balance Service

## Justificativa

### Por que PostgreSQL?

1. **Confiabilidade e Maturidade**:
   - Sistema de banco de dados estabelecido com décadas de desenvolvimento e uso em ambientes críticos
   - Forte aderência ao padrão SQL e compliance ACID

2. **Recursos Avançados**:
   - Tipos de dados ricos (incluindo JSON para flexibilidade)
   - Índices sofisticados (B-tree, Hash, GiST, GIN)
   - Constraints complexos e triggers para garantir integridade dos dados
   - Particionamento de tabelas para dados históricos

3. **Performance**:
   - Otimização de consultas robusta
   - Índices parciais e funcionais
   - Estatísticas detalhadas para o planejador de consultas
   - Bom desempenho para leituras e escritas

4. **Adequação ao Domínio**:
   - Excelente para sistemas financeiros com necessidade de integridade transacional
   - Tipos decimais precisos para valores monetários
   - Suporte a constraints que podem refletir regras de negócio

### Por que Cloud SQL (Google Cloud)?

1. **Operações Simplificadas**:
   - Gerenciamento automatizado de patches e atualizações
   - Backups automatizados e point-in-time recovery
   - Monitoramento integrado e alertas

2. **Alta Disponibilidade**:
   - Replicação automatizada
   - Failover automático
   - SLA de 99.95% de disponibilidade

3. **Segurança**:
   - Criptografia em repouso e em trânsito por padrão
   - Controle de acesso via IAM
   - Rede privada via VPC
   - Conformidade com padrões regulatórios

4. **Escalabilidade**:
   - Escalabilidade vertical com mínimo downtime
   - Opções de réplicas de leitura para escalabilidade horizontal de leituras
   - Provisionamento simples de recursos adicionais

5. **Integração com o Ecossistema GCP**:
   - Fácil integração com Cloud Run (onde nossos microsserviços estão hospedados)
   - Monitoramento integrado com Cloud Monitoring
   - Autenticação integrada com Secret Manager

### Por que Database-per-Service?

1. **Isolamento**:
   - Cada microsserviço tem controle total sobre seu modelo de dados
   - Alterações no esquema não impactam outros serviços

2. **Escalabilidade**:
   - Cada banco pode ser dimensionado independentemente
   - Padrões de acesso diferentes podem ser otimizados separadamente

3. **Segurança**:
   - Separação clara de responsabilidades e acesso aos dados
   - Credenciais separadas para cada serviço

4. **Resiliência**:
   - Falhas em um banco não afetam diretamente outros serviços

## Alternativas Consideradas

### NoSQL (Document DB)

**Vantagens**:
- Esquema flexível para evolução
- Potencialmente melhor escalabilidade horizontal

**Desvantagens**:
- Garantias transacionais mais fracas
- Consultas analíticas mais limitadas
- Menos maduro para sistemas financeiros críticos

### MySQL/MariaDB

**Vantagens**:
- Familiar e amplamente utilizado
- Bom desempenho para operações de leitura

**Desvantagens**:
- Menos recursos avançados que PostgreSQL
- Implementação de SQL menos padronizada

### Banco de Dados Compartilhado

**Vantagens**:
- Facilita consultas que abrangem múltiplos domínios
- Simplifica o backup e a recuperação

**Desvantagens**:
- Acoplamento entre serviços
- Dificulta a escalabilidade independente
- Risco de afetar múltiplos serviços em caso de problemas

## Configurações Principais

### Cloud SQL para PostgreSQL

- Versão: PostgreSQL 14
- Tipo de máquina:
   - Produção: db-custom-4-15360 (4 vCPUs, 15GB RAM)
   - Outros ambientes: db-custom-2-7680 (2 vCPUs, 7.5GB RAM)
- Armazenamento:
   - Produção: 100GB, SSD
   - Outros ambientes: 20GB, SSD
- Alta disponibilidade: Habilitada para produção
- Backups:
   - Diários automáticos
   - 7 dias de retenção
   - Point-in-time recovery habilitado
- Manutenção: Janela programada fora do horário comercial
- Networking: Private IP via VPC peering

## Consequências

### Positivas

- Sistema de banco de dados robusto e confiável para dados financeiros
- Operações simplificadas com serviço gerenciado
- Boa performance para os padrões de acesso esperados
- Flexibilidade para evoluir com o sistema
- Segurança e conformidade aprimoradas

### Negativas

- Custo maior comparado a soluções auto-hospedadas
- Algum grau de lock-in ao provedor de nuvem
- Latência entre serviços e banco de dados geograficamente distribuídos
- Complexidade adicional com múltiplos bancos para administrar

### Mitigação de Riscos

Para mitigar o risco de lock-in:
- Utilizaremos recursos PostgreSQL padrão, evitando extensões específicas do GCP
- Abstrairemos o acesso ao banco via repositories e interfaces
- Manteremos scripts de migração independentes da plataforma

## Verificação

Esta decisão será considerada bem-sucedida se:

1. O sistema mantiver performance adequada sob carga esperada
2. Não ocorrerem perdas de dados ou inconsistências
3. O custo operacional estiver dentro do orçamento
4. A manutenção e evolução dos esquemas ocorrer sem interrupção de serviço significativa

## Links

- [Documentação do Cloud SQL para PostgreSQL](https://cloud.google.com/sql/docs/postgres)
- [Práticas recomendadas para PostgreSQL](https://cloud.google.com/sql/docs/postgres/best-practices)
- [Database-per-Service Pattern](https://microservices.io/patterns/data/database-per-service.html)