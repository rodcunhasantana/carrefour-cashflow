# Carrefour Cash Flow System

Sistema de controle de fluxo de caixa para gerenciamento de lançamentos financeiros (débitos e créditos) e geração de saldos consolidados diários.

## Visão Geral

Este sistema é composto por dois microserviços principais:

- **Transaction Service**: Gerencia os lançamentos financeiros (débitos e créditos)
- **Daily Balance Service**: Gera e consulta saldos consolidados diários

## Tecnologias

- Java 21
- Spring Boot 3.x
- Google Cloud Run (hospedagem dos microserviços)
- Google Cloud SQL — PostgreSQL (banco de dados)
- Google Cloud Pub/Sub (mensageria assíncrona)
- Google Cloud Storage (relatórios e exportações)
- Google Cloud Secret Manager (credenciais)
- Google Cloud Identity Platform (autenticação)
- Docker (desenvolvimento local)

## Estrutura do Projeto

- `/transaction-service` - Microserviço de lançamentos financeiros
- `/dailybalance-service` - Microserviço de saldo consolidado diário
- `/common` - Componentes compartilhados
- `/infrastructure` - Configurações de infraestrutura
- `/docs` - Documentação do projeto

## Começando

### Pré-requisitos

- Java 21+
- Docker Desktop
- Maven
- Google Cloud CLI (`gcloud`)

### Executando localmente

O ambiente local utiliza emuladores GCP para simular os serviços de nuvem.

```bash
# Clone o repositório
git clone https://github.com/seu-usuario/carrefour-cashflow.git
cd carrefour-cashflow

# Iniciar dependências locais (PostgreSQL + emulador Pub/Sub)
docker-compose -f infrastructure/docker/docker-compose.yml up -d

# Compilar e executar os serviços (perfil dev)
cd transaction-service

# Bash / Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# PowerShell (Windows) — aspas obrigatórias nos argumentos -D
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"

# Em outro terminal
cd dailybalance-service

# Bash / Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# PowerShell (Windows)
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"