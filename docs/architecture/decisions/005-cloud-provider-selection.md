# ADR-005: Seleção de Provedor de Nuvem

## Status

Aceito

## Contexto

O sistema Carrefour Cashflow requer uma plataforma de nuvem robusta para hospedar seus microsserviços, bancos de dados e componentes de infraestrutura. Precisamos selecionar um provedor de nuvem que atenda às necessidades de escalabilidade, segurança, confiabilidade e custo-benefício do sistema.

Os três principais provedores considerados foram:
- Google Cloud Platform (GCP)
- Amazon Web Services (AWS)
- Microsoft Azure

## Decisão

Decidimos utilizar o **Google Cloud Platform (GCP)** como provedor de nuvem para o sistema Carrefour Cashflow.

## Justificativa

### Por que Google Cloud em vez de AWS?

1. **Preços mais simples e previsíveis**:
    - Modelo de preços mais transparente e previsível
    - Descontos por uso contínuo automaticamente aplicados sem compromissos antecipados
    - Geralmente menor custo para workloads de microsserviços com escala variável

2. **Serviços serverless superiores**:
    - Cloud Run oferece modelo mais flexível que AWS Fargate ou Lambda
    - Menor cold start time para aplicações Java
    - Menos restrições em termos de recursos e tempo de execução

3. **Rede global de alta performance**:
    - Backbone de rede global proprietário do Google
    - Menor latência de rede global em média
    - Load balancing global sem configuração adicional

4. **Operações mais simples**:
    - Menor sobrecarga operacional com serviços gerenciados
    - Experiência do usuário mais integrada e coesa
    - Menos necessidade de conhecimento especializado para operações básicas

### Por que Google Cloud em vez de Azure?

1. **Serviços nativos de dados e analytics superiores**:
    - BigQuery oferece vantagens em análise de dados financeiros
    - Integração nativa com ferramentas de BI como Looker e Data Studio
    - Capacidades de ML mais acessíveis para futuros desenvolvimentos

2. **Melhor integração com Kubernetes e containers**:
    - Google é o criador original do Kubernetes
    - GKE oferece gerenciamento mais maduro e simplificado
    - Cloud Run não tem equivalente direto no Azure com a mesma simplicidade

3. **Experiência de desenvolvimento mais suave**:
    - Cloud SDK e ferramentas de linha de comando mais consistentes
    - Documentação e exemplos mais claros
    - APIs mais consistentes entre serviços

4. **Melhor alinhamento com tecnologias existentes**:
    - Integração superior com Spring Boot/Java
    - Melhor suporte para PostgreSQL gerenciado

## Consequências

### Positivas

- Acesso a serviços gerenciados de alta qualidade com mínimo overhead operacional
- Escalabilidade automática para atender picos de demanda
- Modelo de custos previsível e otimizado para nossa carga de trabalho
- Infraestrutura como código facilitada via Terraform/Cloud Deployment Manager
- Ambiente único e coeso para todos os componentes do sistema

### Negativas

- Necessidade de aprendizado sobre o ecossistema Google Cloud
- Potencial lock-in com alguns serviços específicos do GCP
- Migração mais complexa se precisarmos mudar de provedor no futuro

### Mitigação de Riscos

Para mitigar o risco de lock-in do provedor:
- Utilizaremos padrões abertos quando possível (PostgreSQL, REST, etc.)
- Encapsularemos integrações específicas do GCP em adaptadores
- Manteremos a arquitetura hexagonal para permitir trocas de adaptadores

## Links

- [Comparativo de custos GCP vs AWS vs Azure](https://cloud.google.com/pricing/calculator)
- [Documentação do Google Cloud](https://cloud.google.com/docs)
- [Arquitetura de referência para microsserviços no GCP](https://cloud.google.com/architecture/microservices-architecture-reference-architecture)