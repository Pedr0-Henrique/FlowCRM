# Operação e deploy

Este guia prepara uma instalação própria com Docker Compose, PostgreSQL, Caddy e TLS automático. O repositório não publica imagens nem faz deploy sozinho: o operador controla o host, o DNS, os segredos e o momento da atualização.

## Requisitos do host

- Docker Engine e Docker Compose v2.
- DNS apontando `SITE_ADDRESS` para o host.
- Portas TCP 80/443 acessíveis pela internet; UDP 443 é opcional para HTTP/3.
- Espaço persistente e backups externos para o volume PostgreSQL.

## Primeira instalação

1. Copie `.env.production.example` para `.env.production`.
2. Configure `SITE_ADDRESS` e `PUBLIC_ORIGIN` para o mesmo domínio público.
3. Gere valores exclusivos para `POSTGRES_PASSWORD`, `JWT_SECRET` e `GRAFANA_ADMIN_PASSWORD`. O backend recusa segredos JWT padrão/curtos, senha fraca do banco e origens CORS que não sejam HTTPS.
4. Valide e inicie:

   ```sh
   docker compose --env-file .env.production -f docker-compose.prod.yml config --quiet
   docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
   ```

5. Confira `https://SEU_DOMINIO/` e `https://SEU_DOMINIO/api/v1/auth/me` (a segunda rota deve exigir autenticação).

Caddy termina TLS e encaminha `/api/*` à API; as demais rotas vão ao frontend. PostgreSQL não publica uma porta no host. O backend e o frontend também não publicam portas; o tráfego externo passa pelo proxy. Flyway aplica migrações ao iniciar a API.

## Métricas e alertas

Para iniciar Prometheus, Alertmanager e Grafana junto com a aplicação, configure uma senha forte em `GRAFANA_ADMIN_PASSWORD`, um relay SMTP autenticado com TLS (`SMTP_HOST` como `host:587`, `SMTP_FROM`, `SMTP_USERNAME`, `SMTP_PASSWORD`) e o e-mail operacional em `ALERT_EMAIL`:

```sh
docker compose --env-file .env.production --profile monitoring -f docker-compose.prod.yml up -d --build
```

Prometheus e Grafana ficam vinculados somente a `127.0.0.1` nas portas 9090 e 3002. Acesse-os por túnel SSH ou por uma VPN administrativa; não altere o bind para `0.0.0.0` sem adicionar autenticação e proteção de rede. O Grafana provisiona o dashboard da API e o datasource Prometheus. Prometheus avalia alertas de indisponibilidade, uso elevado do heap e taxa de respostas HTTP 5xx; Alertmanager agrupa e envia os alertas por e-mail via relay SMTP configurado.

Verifique os serviços e logs:

```sh
docker compose --env-file .env.production -f docker-compose.prod.yml ps
docker compose --env-file .env.production -f docker-compose.prod.yml logs --since 15m backend
```

O healthcheck da API é interno ao Compose. Não exponha o endpoint Prometheus diretamente na internet.

## Backup e restauração

Faça backup antes de cada atualização que possa aplicar migrações. Armazene cópias criptografadas fora do host, teste a restauração periodicamente e defina retenção compatível com a política de dados da organização.

Linux/macOS (cria um dump SQL no host):

```sh
mkdir -p backups
docker compose --env-file .env.production -f docker-compose.prod.yml exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' > backups/flowcrm-$(date +%Y%m%d-%H%M%S).sql
```

PowerShell no Windows:

```powershell
New-Item -ItemType Directory -Force backups | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$dump = "backups/flowcrm-$stamp.sql"
$command = 'docker compose --env-file .env.production -f docker-compose.prod.yml exec -T postgres sh -c "pg_dump -U $POSTGRES_USER $POSTGRES_DB" > "' + $dump + '"'
cmd.exe /c $command
if ($LASTEXITCODE -ne 0) { throw "O backup do PostgreSQL falhou." }
```

Restauração em uma instância de destino:

```sh
cat backups/ARQUIVO.sql | docker compose --env-file .env.production -f docker-compose.prod.yml exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" "$POSTGRES_DB"'
```

No PowerShell, use `cmd.exe` para preservar a transferência do dump sem conversão de encoding:

```powershell
$dump = "backups\ARQUIVO.sql"
$command = 'docker compose --env-file .env.production -f docker-compose.prod.yml exec -T postgres sh -c "psql -v ON_ERROR_STOP=1 -U $POSTGRES_USER $POSTGRES_DB" < "' + $dump + '"'
cmd.exe /c $command
if ($LASTEXITCODE -ne 0) { throw "A restauração do PostgreSQL falhou." }
```

Restaure primeiro em um ambiente isolado e confirme a integridade antes de substituir dados de produção. Mantenha o backup fora do repositório.

## Atualização e recuperação

1. Faça e verifique um backup do banco.
2. Atualize o código para a revisão desejada.
3. Valide a configuração e reconstrua: `docker compose ... config --quiet` e `docker compose ... up -d --build`.
4. Confira healthchecks, logs, login e operações essenciais do CRM.
5. Em caso de falha, volte à revisão anterior e reconstrua. Migrações Flyway são progressivas; restaurar um backup anterior pode ser necessário se uma migração incompatível já tiver sido aplicada.

As variáveis de produção são obrigatórias e devem ser mantidas fora do Git. Não use os valores demonstrativos do arquivo `.env.production.example` em um ambiente real.
