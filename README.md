# Carrefour Cash Flow System

Sistema de controle de fluxo de caixa para gerenciamento de lançamentos financeiros (débitos e créditos) e geração de saldos consolidados diários.

## Visão Geral

Este sistema é composto por dois microserviços principais:

- **Transaction Service**: Gerencia os lançamentos financeiros (débitos e créditos)
- **Daily Balance Service**: Gera e consulta saldos consolidados diários

## Tecnologias

- Java 21
- Spring Boot 3.x
- PostgreSQL
- Google Cloud Platform (GCP)
- Docker

## Estrutura do Projeto

- `/transaction-service` - Microserviço de lançamentos financeiros
- `/dailybalance-service` - Microserviço de saldo consolidado diário
- `/common` - Componentes compartilhados
- `/infrastructure` - Configurações de infraestrutura
- `/docs` - Documentação do projeto

## Começando

### Pré-requisitos

- Java 21+
- Docker Desktop para Windows
- Maven

### Executando localmente

```bash
# Clone o repositório
git clone https://github.com/seu-usuario/carrefour-cashflow.git
cd carrefour-cashflow

# Iniciar dependências (PostgreSQL, etc.)
docker-compose -f infrastructure/docker/docker-compose.yml up -d

# Compilar e executar os serviços
cd transaction-service
./mvnw spring-boot:run

# Em outro terminal
cd dailybalance-service
./mvnw spring-boot:run