# FlowCRM

CRM SaaS multiempresa para pequenas empresas. Stack: **Next.js 16 + TypeScript** no frontend, **Java 21 + Spring Boot 3.4** na API, **PostgreSQL 16**, **Docker** e **GitHub Actions**.

O sistema inclui autenticação JWT, controle de acesso por perfil, isolamento de dados por empresa e telas para leads, clientes, empresas, oportunidades, tarefas, atividades, relatórios e configurações.

## Arquitetura

```text
Usuário → Next.js (frontend) → REST API → Spring Boot
                                      ├── PostgreSQL (dados CRM, rate limit e auditoria)
                                      └── Prometheus → Alertmanager → e-mail (opcional)
```

## Estrutura do repositório

```text
FlowCRM/
├── frontend/     # Next.js App Router
├── backend/      # Spring Boot API
├── ops/          # Caddy, Prometheus, Alertmanager e Grafana
├── .github/      # CI, CodeQL, revisão de dependências e Dependabot
├── docker-compose.yml
├── docker-compose.prod.yml
└── README.md
```

## Pré-requisitos

- Node.js 22+
- JDK 21 (local) **ou** Docker
- Docker Desktop e Docker Compose para executar o stack completo ou o PostgreSQL local

O backend usa Java 21. Os comandos locais da API também precisam do Maven instalado. Instale o [Docker Desktop](https://www.docker.com/products/docker-desktop/) para executar o stack conteinerizado.

## Subir com Docker

```bash
docker compose up -d --build
```

- Frontend: http://localhost:3000
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

O Compose inicia PostgreSQL, API e frontend com a configuração local definida em `docker-compose.yml`. As migrações do banco são aplicadas pelo Flyway ao iniciar o backend. Os valores configurados são apenas para desenvolvimento; revise credenciais, segredo JWT, origens CORS e exposição de portas antes de qualquer implantação.

### Deploy próprio com TLS

Há um Compose de produção independente de provedor, com PostgreSQL privado, API, frontend e Caddy para HTTPS:

```powershell
Copy-Item .env.production.example .env.production
# Edite .env.production: domínio, senhas fortes e segredo JWT exclusivo.
docker compose --env-file .env.production -f docker-compose.prod.yml config --quiet
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
```

Aponte o DNS do domínio para o host e permita as portas TCP 80/443 (UDP 443 é opcional). O backend exige segredo JWT com pelo menos 256 bits, senha forte do banco e origens CORS HTTPS ao iniciar com o perfil `prod`. Nenhum serviço de aplicação ou banco publica portas diretamente; Caddy é o ponto de entrada TLS. Os valores demonstrativos de `.env.production.example` devem ser substituídos antes de usar.

Para ativar Prometheus, Alertmanager e Grafana, configure uma senha forte em `GRAFANA_ADMIN_PASSWORD`, um relay SMTP TLS autenticado (`SMTP_HOST`, `SMTP_FROM`, `SMTP_USERNAME`, `SMTP_PASSWORD`) e `ALERT_EMAIL`; depois inicie com `--profile monitoring`. As interfaces do Prometheus e Grafana ficam bindadas a `127.0.0.1:9090` e `127.0.0.1:3002`; acesse por VPN ou túnel SSH e não publique essas portas. O dashboard e datasource Grafana são provisionados automaticamente; alertas de API indisponível, heap alto e taxa HTTP 5xx são enviados por email via Alertmanager. Procedimentos de deploy, healthcheck, backup/restauração e recuperação estão em [ops/OPERATIONS.md](./ops/OPERATIONS.md).

## Desenvolvimento local

O arquivo `.env.example` lista variáveis e valores destinados ao desenvolvimento local. Copie-o para `.env` somente se a ferramenta usada para iniciar o backend carregar esse arquivo; Spring Boot não carrega automaticamente o `.env` da raiz. Não versione `.env` nem use os valores de exemplo em produção.

### Banco

```bash
docker compose up -d postgres
```

O banco local padrão fica disponível em `localhost:5432`, com database e usuário `flowcrm`. Para desenvolvimento, as credenciais padrão estão no `docker-compose.yml`.

### Backend

```bash
cd backend
# requer JDK 21 e Maven
mvn spring-boot:run
```

O backend lê `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET` e `CORS_ALLOWED_ORIGINS` das variáveis de ambiente. Ao executar fora do Docker, configure-as no terminal ou na configuração de execução da IDE. Os nomes e valores de exemplo para desenvolvimento estão documentados em `.env.example`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

O frontend em desenvolvimento fica disponível em http://localhost:3001 e usa a API em `http://localhost:8080` por padrão. Altere `NEXT_PUBLIC_API_URL` para apontar para outra instância. No Docker, o frontend fica disponível em http://localhost:3000.

### Verificações

Frontend:

```bash
cd frontend
npm ci
npm audit --omit=dev --audit-level=high
npm run lint
npx tsc --noEmit
npm test
npm run build
```

Backend (JDK 21, Maven e PostgreSQL disponível; é possível iniciar o banco local com `docker compose up -d postgres`):

```bash
cd backend
mvn verify
```

O workflow de CI executa lint, TypeScript, testes unitários e build do frontend; para o backend executa `mvn verify` com PostgreSQL 16. Os testes cobrem autenticação, autorização por perfil, isolamento entre empresas, rate limiting compartilhado e persistência de auditoria.

## Funcionalidades

- **Autenticação e usuários:** cadastro da empresa e primeiro usuário ADMIN, login compartilhado e gestão de usuários por ADMIN/MANAGER.
- **CRM:** cadastro, edição, listagem e exclusão autorizada de empresas, clientes, leads e oportunidades; leads e oportunidades incluem fluxo de pipeline.
- **Tarefas:** criação, edição, conclusão e atribuição a funcionários da empresa por ADMIN/MANAGER. Tarefas criadas por SALES/USER são sempre atribuídas ao criador; esses perfis só podem editar ou concluir tarefas atribuídas a si. Uma tarefa não concluída nem cancelada é identificada automaticamente como **em atraso** quando seu prazo passa.
- **Atividades:** registro e consulta da linha do tempo da empresa.
- **Dashboard e relatórios:** indicadores de CRM, distribuição de leads por status, oportunidades por estágio, tarefas por status e exportação de relatório CSV.
- **Preferências:** edição de perfil e alternância de tema claro/escuro.
- **Interface:** páginas responsivas em português e confirmação consistente antes de excluir registros.

O gráfico de leads no dashboard apresenta somente status com registros, mostra a contagem total e identifica cada status em português.

## API (v1)

### Auth
```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/auth/me
```

### Companies (ADMIN)
```text
GET    /api/v1/companies
GET    /api/v1/companies/{id}
GET    /api/v1/companies/slug/{slug}
POST   /api/v1/companies
PUT    /api/v1/companies/{id}
DELETE /api/v1/companies/{id}
```

### Users
```text
GET    /api/v1/users (ADMIN, MANAGER)
GET    /api/v1/users/{id}
POST   /api/v1/users (ADMIN, MANAGER)
PUT    /api/v1/users/{id}
DELETE /api/v1/users/{id} (ADMIN)
```

ADMIN e MANAGER podem criar contas para a própria empresa na página **Usuários**. MANAGER pode criar apenas contas SALES e USER; ADMIN também pode criar MANAGER. O primeiro usuário da empresa é criado como ADMIN durante o cadastro. Todos entram pela mesma página `/login` usando e-mail e senha.

### Clients
```text
GET    /api/v1/clients?search=&status=&page=&size=
GET    /api/v1/clients/{id}
POST   /api/v1/clients (ADMIN, MANAGER, SALES)
PUT    /api/v1/clients/{id} (ADMIN, MANAGER, SALES)
DELETE /api/v1/clients/{id} (ADMIN, MANAGER)
```

### Leads
```text
GET    /api/v1/leads?search=&status=&priority=&assignedTo=&page=&size=
GET    /api/v1/leads/{id}
POST   /api/v1/leads (ADMIN, MANAGER, SALES)
PUT    /api/v1/leads/{id} (ADMIN, MANAGER, SALES)
PATCH  /api/v1/leads/{id}/status (pipeline drag & drop)
DELETE /api/v1/leads/{id} (ADMIN, MANAGER)
```

### Opportunities
```text
GET    /api/v1/opportunities?search=&stage=&client=&lead=&assignedTo=&page=&size=
GET    /api/v1/opportunities/{id}
POST   /api/v1/opportunities (ADMIN, MANAGER, SALES)
PUT    /api/v1/opportunities/{id} (ADMIN, MANAGER, SALES)
PATCH  /api/v1/opportunities/{id}/stage (pipeline)
DELETE /api/v1/opportunities/{id} (ADMIN, MANAGER)
```

### Tasks
```text
GET    /api/v1/tasks?search=&status=&priority=&assignedTo=&client=&lead=&page=&size=
GET    /api/v1/tasks/{id}
POST   /api/v1/tasks (ADMIN, MANAGER, SALES, USER)
PUT    /api/v1/tasks/{id} (ADMIN, MANAGER, SALES, USER)
PATCH  /api/v1/tasks/{id}/complete
DELETE /api/v1/tasks/{id} (ADMIN, MANAGER)
```

### Activities
```text
GET    /api/v1/activities?page=&size=
POST   /api/v1/activities (ADMIN, MANAGER, SALES, USER)
DELETE /api/v1/activities/{id} (ADMIN, MANAGER)
```

### Dashboard
```text
GET /api/v1/dashboard (ADMIN, MANAGER, SALES)
```

## Multi-tenancy

Cada empresa (`companies`) isola usuários e dados via `company_id`. O tenant **nunca** é confiável a partir do body da requisição — vem do token/sessão.

## Roles

| Role    | Escopo                                      |
|---------|---------------------------------------------|
| ADMIN   | Acesso administrativo aos módulos da empresa, gestão de usuários e configurações |
| MANAGER | Gestão de usuários permitida, leads, clientes, oportunidades, tarefas e relatórios |
| SALES   | Leads, clientes, oportunidades, tarefas, atividades e dashboard |
| USER    | Tarefas, atividades e configurações do próprio perfil |

As permissões são verificadas pela API; ocultar ou exibir itens de navegação no frontend não substitui a autorização do backend. Os registros são sempre limitados à empresa associada à sessão autenticada. SALES e USER não podem atribuir, editar ou concluir tarefas pertencentes a outro funcionário.

## Roadmap

✅ 1. Setup (este repositório)
✅ 2. Auth (JWT, refresh, roles)
✅ 3. Empresas, usuários e permissões
✅ 4. Clientes
✅ 5. Leads e pipeline
✅ 6. Oportunidades
✅ 7. Tarefas e atividades
✅ 8. Dashboard, relatórios e configurações
✅ 9. Segurança (rate limiting PostgreSQL, auditoria com retenção, validação de segredos, headers, CodeQL e revisão de dependências implementados; resolver advisory alto transitivo de desenvolvimento e validar controles no ambiente real pendente)
✅ 10. Testes (cobertura de autenticação, permissões, isolamento, rate limiting, auditoria e tarefas adicionada; execução do `mvn verify`/CI após estas alterações pendente)
✅11. DevOps (CI, Compose de produção, TLS e alertas configurados; validar build/Compose e realizar primeiro deploy no host/DNS escolhidos pendente)
✅ 12. Finalização (README e procedimentos de operação prontos; validar restauração de backup e checklist de release no ambiente implantado pendente)

## Definition of Done

Uma funcionalidade só fecha com: comportamento correto, responsivo, light/dark, validação, erros padronizados, autorização, isolamento por empresa, testes, query eficiente, logs sem secrets e CI verde.

## Segurança

Não commitar `.env`, senhas, JWT ou dados pessoais. Logs não devem registrar tokens nem credenciais.

As rotas de login, cadastro e renovação de token têm limites por endereço IP (10, 5 e 20 requisições por minuto, respectivamente), compartilhados entre instâncias pelo PostgreSQL e configuráveis por `AUTH_LOGIN_RATE_LIMIT`, `AUTH_REGISTER_RATE_LIMIT`, `AUTH_REFRESH_RATE_LIMIT` e `AUTH_RATE_LIMIT_WINDOW_SECONDS`. Os contadores armazenam somente um HMAC do endereço e da rota, e os contadores expirados são removidos diariamente. Operações autenticadas de escrita na API e tentativas nas rotas públicas de autenticação geram eventos persistidos com método, rota, resultado, IDs do usuário/empresa e IP, sem armazenar corpos, tokens ou credenciais; os eventos são removidos após 90 dias por padrão (`AUDIT_RETENTION_DAYS`). O evento de auditoria registra o endereço IP para investigação de abuso; defina a base legal, controle de acesso e prazo de retenção conforme a política de privacidade da operação. Proteja e faça backup do banco de auditoria.

No perfil `prod`, o Swagger/OpenAPI fica desabilitado, respostas de erro não incluem mensagens internas ou stack traces, e o backend valida segredo JWT, senha de banco e CORS HTTPS explícito. O limite de tentativas e a auditoria dependem do PostgreSQL; se a escrita no armazenamento de rate limit falhar, a autenticação falha explicitamente em vez de ignorar a proteção. O Prometheus é permitido pela configuração de segurança da API para scraping interno e deve permanecer sem portas públicas, como no Compose de produção. O CI executa CodeQL, revisão de novas dependências e uma verificação de vulnerabilidades das dependências de produção. O `npm audit` completo ainda aponta um advisory alto em `braces`, dependência transitiva de desenvolvimento do `eslint-config-next`; a correção sugerida pelo registry rebaixa essa configuração para Next.js 14, incompatível com o projeto Next.js 16, então a dependência é mantida na versão compatível e o advisory fica registrado para atualização quando houver correção segura do upstream.
