# Documentação de Banco de Dados - Síndico App

## ⚠️ AVISO DE SEGURANÇA

**NUNCA exponha credenciais, senhas ou tokens neste documento.**

Este documento contém **apenas informações de arquitetura e estrutura**. Credenciais devem estar **exclusivamente** em arquivos `.env` que estão no `.gitignore`.

---

## 🏗️ Arquitetura de Banco de Dados

### Ambientes

| Ambiente | Localização | Configuração |
|----------|-------------|--------------|
| **Desenvolvimento** | Local (máquina do desenvolvedor) | Ver `.env.example` |
| **VPS** | Hostinger VPS `76.13.163.235` | PostgreSQL 16 nativo |
| **Produção** | (a definir) | Ver `application.yml` profile `prod` |

---

## 🔧 Configuração VPS (Atual)

### PostgreSQL Nativo

- **Versão:** PostgreSQL 16
- **IP da VPS:** `76.13.163.235` (IPv4) / `2a02:4780:6e:cebf::1` (IPv6)
- **Porta:** `5433` (não padrão - porta 5432 ocupada por container Docker)
- **IPs de escuta:**
  - `127.0.0.1` (localhost)
  - `172.17.0.1` (Docker bridge padrão)
  - `172.24.0.1` (rede Docker do pgAdmin)
  - `::1` (IPv6 localhost)

### Acesso via pgAdmin

- **URL:** `http://76.13.163.235:5050/browser/`
- **Host de conexão:** `172.24.0.1` (gateway da rede Docker do container pgAdmin)
- **Porta:** `5433`
- **Database:** `sindico`
- **Username:** `sindico_user`
- **Password:** `(ver .env.vps - NUNCA commitar)`

### Arquivos de Configuração no Servidor

```
/etc/postgresql/16/main/postgresql.conf
  └─ listen_addresses = 'localhost,172.17.0.1,172.24.0.1'

/etc/postgresql/16/main/pg_hba.conf
  └─ Regras de acesso para redes Docker (172.17.0.0/16 e 172.24.0.0/16)

/var/lib/postgresql/16/main/
  └─ Diretório de dados (data_directory)
```

---

## 📊 Estrutura do Banco `sindico`

### Tabelas Criadas (12 tabelas)

| # | Tabela | Descrição |
|---|--------|-----------|
| 1 | `usuarios` | Usuários do sistema (síndicos, moradores, admins) |
| 2 | `condominios` | Cadastro de condomínios |
| 3 | `usuarios_condominios` | Relacionamento usuário ↔ condomínio |
| 4 | `compromissos` | Agenda de compromissos |
| 5 | `reunioes` | Reuniões de condomínio |
| 6 | `participantes_reuniao` | Participantes de cada reunião |
| 7 | `pendencias` | Pendências e tarefas |
| 8 | `fornecedores` | Cadastro de fornecedores |
| 9 | `manutencoes` | Registro de manutenções |
| 10 | `ativos` | Ativos do condomínio |
| 11 | `anexos` | Arquivos anexados |
| 12 | `flyway_schema_history` | Controle de versões de migrations (Flyway) |

### Migrations Aplicadas

- ✅ **V1__create_tables.sql** - Estrutura inicial
- ✅ **V2__add_admin_user.sql** - Usuário admin padrão
- ✅ **V3__remove_google_integration.sql** - Remoção de integração Google

**Estado atual:** Migrations aplicadas manualmente via `psql`. Flyway configurado com `baseline-on-migrate: true` e `baseline-version: 3`.

---

## 🔐 Gestão de Credenciais

### Arquivos de Ambiente (NUNCA COMMITAR)

```
.env           ← desenvolvimento local (gitignored)
.env.vps       ← credenciais VPS (gitignored)
.env.example   ← template SEM credenciais (pode commitar)
```

### Variáveis de Ambiente Necessárias

```bash
# Banco de dados
DB_HOST=           # Endereço do servidor PostgreSQL
DB_PORT=           # Porta (5433 no VPS)
DB_NAME=           # Nome do banco (sindico)
DB_USERNAME=       # Usuário do banco
DB_PASSWORD=       # Senha (NUNCA commitar)

# Aplicação
APP_ADMIN_USERNAME=    # Username admin inicial
APP_ADMIN_PASSWORD=    # Senha admin (NUNCA commitar)
```

### Rotação de Credenciais

**Quando rotacionar:**
- ✅ Após qualquer exposição acidental (commit, logs, chat)
- ✅ Periodicamente (a cada 90 dias recomendado)
- ✅ Após saída de membros da equipe
- ✅ Após detecção de acesso suspeito

**Como rotacionar:**
```bash
# Conectar ao PostgreSQL
sudo -u postgres psql -h 127.0.0.1 -p 5433

# Alterar senha do usuário
ALTER USER sindico_user WITH PASSWORD 'nova_senha_forte_aqui';

# Atualizar .env.vps
# Reiniciar aplicação
```

---

## 🚀 Conexão da Aplicação Spring Boot

### Profiles Disponíveis

#### `dev` (desenvolvimento local)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:sindico}
```

#### `vps` (VPS Hostinger)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/sindico
  flyway:
    baseline-on-migrate: true
    baseline-version: 3
```

#### `prod` (produção futura)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT:5433}/${DB_NAME}
```

### Como Conectar via SSH Tunnel

Para desenvolvimento local conectando na VPS:

```bash
# Iniciar túnel SSH
./tunnel-vps.sh start

# Usar profile vps
mvn spring-boot:run -Dspring-boot.run.profiles=vps

# Parar túnel
./tunnel-vps.sh stop
```

---

## 🛡️ Segurança Implementada

### PostgreSQL

- ✅ Usuário dedicado com permissões mínimas (`sindico_user`)
- ✅ Autenticação `scram-sha-256` (não MD5)
- ✅ Restrição de acesso por IP/rede no `pg_hba.conf`
- ✅ Não exposto publicamente na internet
- ✅ Acessível apenas via redes Docker internas e localhost
- ⚠️ **TODO:** Configurar backup automatizado
- ⚠️ **TODO:** Configurar SSL/TLS para conexões remotas

### Aplicação

- ✅ Senhas hashadas com BCrypt
- ✅ Queries parametrizadas (JPA)
- ✅ Validação de entrada com Bean Validation
- ✅ Headers de segurança (CSP, X-Frame-Options, etc.)
- ✅ Controle de sessão
- ✅ Credenciais em variáveis de ambiente

### Pendências de Segurança

- [ ] Implementar rate limiting no banco
- [ ] Configurar auditoria de queries sensíveis
- [ ] Implementar backup automatizado criptografado
- [ ] Configurar monitoramento de acessos suspeitos
- [ ] Implementar rotação automática de credenciais
- [ ] Adicionar SSL/TLS nas conexões PostgreSQL
- [ ] Configurar replicação para alta disponibilidade

---

## 📝 Procedimentos Operacionais

### Backup Manual

```bash
# Conectar na VPS
ssh root@76.13.163.235

# Fazer backup
PGPASSWORD='senha_aqui' pg_dump -h 127.0.0.1 -p 5433 -U sindico_user -d sindico \
  --no-owner --no-acl -F c -f /tmp/backup_sindico_$(date +%Y%m%d_%H%M%S).dump

# Transferir backup para local seguro
# (implementar automação)
```

### Restauração

```bash
# Conectar na VPS
ssh root@76.13.163.235

# Restaurar backup
PGPASSWORD='senha_aqui' pg_restore -h 127.0.0.1 -p 5433 -U sindico_user -d sindico_restore \
  --no-owner --no-acl /tmp/backup_sindico_YYYYMMDD_HHMMSS.dump
```

### Verificação de Saúde

```bash
# Conectar no PostgreSQL
PGPASSWORD='senha' psql -h 127.0.0.1 -p 5433 -U sindico_user -d sindico

# Verificar conexões ativas
SELECT count(*) FROM pg_stat_activity WHERE datname = 'sindico';

# Verificar tamanho do banco
SELECT pg_size_pretty(pg_database_size('sindico'));

# Verificar tabelas e tamanho
\dt+

# Verificar último vacuum
SELECT schemaname, relname, last_vacuum, last_autovacuum 
FROM pg_stat_user_tables;
```

---

## 🔍 Troubleshooting

### Problema: "Connection refused"

**Causa:** PostgreSQL não está escutando no IP/porta esperado.

**Solução:**
```bash
# Verificar se PostgreSQL está rodando
systemctl status postgresql@16-main

# Verificar portas abertas
ss -tlnp | grep postgres

# Verificar listen_addresses
sudo cat /etc/postgresql/16/main/postgresql.conf | grep listen_addresses
```

### Problema: "password authentication failed"

**Causa:** Credenciais incorretas ou método de autenticação incompatível.

**Solução:**
```bash
# Verificar pg_hba.conf
sudo cat /etc/postgresql/16/main/pg_hba.conf

# Resetar senha se necessário
sudo -u postgres psql -h 127.0.0.1 -p 5433
ALTER USER sindico_user WITH PASSWORD 'nova_senha';
```

### Problema: pgAdmin não conecta

**Causa:** Host incorreto ou rede Docker errada.

**Solução:**
- ✅ Usar host `172.24.0.1` (gateway da rede do container pgAdmin)
- ✅ Usar porta `5433`
- ✅ Verificar que PostgreSQL está escutando nesse IP

---

## 📚 Referências

- [PostgreSQL Documentation](https://www.postgresql.org/docs/16/)
- [Spring Boot Data JPA](https://spring.io/projects/spring-data-jpa)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [OWASP Database Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Database_Security_Cheat_Sheet.html)

---

## 📞 Contatos e Suporte

- **Mantenedor:** Wesley Oliveira
- **Repository:** `WesleyOliveirajf/sindico`
- **Documentação:** `/docs/`

---

**Última atualização:** 2026-05-02
