# ADR 006: Autenticação via API Key

## Status
✅ **Aceito**

---

## Contexto

Os endpoints REST de ambos os microserviços estavam públicos — qualquer cliente sem credencial poderia criar transações, fechar períodos ou consultar saldos. Para um sistema financeiro, isso é inaceitável mesmo em ambientes de desenvolvimento.

---

## Decisão

Adotar autenticação por **API Key** via header HTTP `X-API-Key`, implementada como filtro Spring Security (`OncePerRequestFilter`) em cada serviço.

### Como funciona

```
Cliente → X-API-Key: <chave>  →  ApiKeyAuthFilter
                                      │
                              chave válida? ──── sim ──→ autentica, continua
                                      │
                                     não
                                      │
                                  HTTP 401 + JSON
```

1. O `ApiKeyAuthFilter` intercepta **toda requisição** antes dos controllers
2. Lê o header `X-API-Key`
3. Compara com o valor configurado em `app.security.api-key`
4. Se válido → injeta `Authentication` no `SecurityContextHolder` e passa para o próximo filtro
5. Se inválido ou ausente → responde `401 Unauthorized` imediatamente

### Endpoints públicos (não exigem API Key)

| Padrão | Motivo |
|---|---|
| `/actuator/**` | Health checks e métricas para orquestração (Kubernetes, Cloud Run) |
| `/swagger-ui/**`, `/swagger-ui.html` | Documentação interativa da API |
| `/v3/api-docs/**` | Spec OpenAPI para ferramentas de geração de cliente |

### Configuração

A chave é lida da variável de ambiente `APP_API_KEY`. Se não definida, usa o default `cashflow-dev-key` (apenas para desenvolvimento local sem Docker).

```yaml
# application.yaml
app:
  security:
    api-key: ${APP_API_KEY:cashflow-dev-key}
```

```yaml
# docker-compose.yml
environment:
  APP_API_KEY: cashflow-local-key
```

Em produção (Cloud Run), `APP_API_KEY` deve ser injetado via **Secret Manager**.

---

## Alternativas Consideradas

### JWT (JSON Web Token)
- **Vantagens**: Carrega claims do usuário, expiração, revogação granular
- **Desvantagens**: Requer servidor de autenticação (Identity Platform / Keycloak), mais complexo para este escopo onde não há sessões de usuário

### HTTP Basic Auth
- **Vantagens**: Nativo no Spring Security, simples
- **Desvantagens**: Requer `UserDetailsService`, encoding base64 não agrega segurança real sobre HTTPS

### mTLS (Mutual TLS)
- **Vantagens**: Autenticação na camada de transporte, forte para service-to-service
- **Desvantagens**: Complexidade operacional alta, requer PKI

---

## Consequências

### ✅ Positivas
- Todos os endpoints `/api/**` protegidos com zero impacto nas regras de negócio
- Stateless — sem sessões, sem banco de dados de tokens
- Simples de usar por clientes (Postman, k6, curl)
- Escalável: API Key pode ser rotacionada via variável de ambiente sem redeploy de código

### ❌ Negativas / Limitações
- Sem diferenciação de permissões por cliente (todos com a mesma chave têm acesso total)
- Sem expiração automática da chave
- Sem auditoria de qual cliente fez cada requisição (todos aparecem como `api-client`)

Estas limitações são aceitáveis para o escopo atual. Evolução natural seria JWT com Identity Platform quando houver múltiplos tipos de cliente.

---

## Observações

> **Rotação de chave em produção:** basta atualizar o secret no Cloud Secret Manager e fazer redeploy. Sem mudança de código.

> **Testes automatizados:** os testes de controller usam `MockMvcBuilders.standaloneSetup`, que não aplica filtros Spring Security — continuam funcionando sem o header. Os testes `@SpringBootTest` carregam o contexto completo com Spring Security ativo, mas não fazem chamadas HTTP, portanto não são afetados.
