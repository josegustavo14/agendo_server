# Agendo API

API REST para gerenciamento de agendamentos entre profissionais e clientes.

## Tecnologias

- Java 21
- Spring Boot 4.0.3
- Spring Security (autenticação por Bearer Token)
- PostgreSQL
- Hibernate / Spring Data JPA
- Flyway (gerenciamento de migrações de schema)
- Swagger / OpenAPI 3

## Pré-requisitos

- Java 21
- PostgreSQL rodando em `localhost:5432`
- Banco de dados `agendo_db` criado

## Configuração

As configurações estão em `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/agendo_db
spring.datasource.username=admin
spring.datasource.password=1234
spring.jpa.hibernate.ddl-auto=validate

# Flyway
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0
```

### Primeiro Setup

Na primeira vez que configurar o banco, execute no PostgreSQL:

```sql
CREATE DATABASE agendo_db;
GRANT ALL ON SCHEMA public TO admin;
ALTER SCHEMA public OWNER TO admin;
```

## Como rodar

```bash
./gradlew bootRun
```

A API estará disponível em `http://localhost:8080`.

Documentação Swagger: `http://localhost:8080/swagger-ui.html`

## Autenticação

A API usa autenticação por **Bearer Token** (UUID gerado no cadastro).

Endpoints que **não precisam** de token:
- `POST /users` — cadastro
- `POST /users/login` — login
- `GET /professions` — listar profissões
- `GET /professionals` — buscar profissionais
- `GET /professionals/{id}` — detalhes do profissional
- `GET /professionals/{id}/services` — serviços do profissional

Todos os outros endpoints requerem o header:
```
Authorization: Bearer <token>
```

O token é retornado tanto no cadastro quanto no login.

## Migrações de Banco de Dados (Flyway)

A API usa **Flyway** para gerenciar mudanças no schema do PostgreSQL.

### Arquivos de Migração
Os arquivos SQL estão em `src/main/resources/db/migration/`:
- `V1__initial_schema.sql` — schema inicial com todas as tabelas
- `V2__update_appointments_schema.sql` — alterações para suportar múltiplos serviços por agendamento
- `V3__add_appointment_status.sql` — adiciona coluna `status` na tabela `appointments`

### Adicionar Novas Migrações
Quando precisar alterar o schema:

1. Crie um novo arquivo `V{numero}__descricao.sql` em `src/main/resources/db/migration/`
2. Nunca edite arquivos já criados (Flyway detectará como erro)
3. Na próxima execução, o Flyway executará automaticamente

Exemplo:
```sql
-- V3__add_new_column.sql
ALTER TABLE users ADD COLUMN last_login TIMESTAMP;
```

## Isolamento de dados

Cada usuário só acessa os **próprios dados**:

- **Agendamentos** — só aparecem agendamentos onde o usuário é o profissional ou o cliente.
- **Tipos de serviço** — cada profissional só vê e gerencia os seus próprios tipos de serviço.
- Tentar acessar dados de outro usuário retorna `404` ou `403`.

## Modelo de Dados

### Users (Usuários)
- Base para profissionais e clientes
- Pode ter role `PROFESSIONAL` ou `CLIENT`
- Gera automaticamente um token UUID na criação

### Professionals (Perfil Profissional)
- 1:1 com Users
- Contém: profissão, bio, disponibilidade
- Profissão é referência a tabela `professions`

### Clients (Perfil Cliente)
- 1:1 com Users
- Contém: CPF/CNPJ (tax_id), método de pagamento preferido

### Professions (Profissões)
- Categorias de profissionais (ex: Eletricista, Desenvolvedor, Personal Trainer)
- Referenciada por ProfessionalProfileEntity

### Service Types (Tipos de Serviço)
- Serviços oferecidos por cada profissional
- 1:N com Professionals (cada profissional pode oferecer vários serviços)
- Cada serviço tem um `price` (preço fixo por serviço)

### Appointment Services (Serviços do Agendamento)
- Tabela de junção M:N entre Appointments e ServiceTypes
- Um agendamento pode ter múltiplos serviços

### Appointments (Agendamentos)
- Relação entre profissional e cliente
- Podem incluir múltiplos serviços (M:N via `AppointmentServiceEntity`)
- `totalAmount` é calculado automaticamente como a soma dos preços dos serviços inclusos
- `status` representa o ciclo de vida do agendamento:

| Status | Descrição |
|--------|-----------|
| `PENDING` | Criado pelo cliente, aguardando aprovação do profissional |
| `APPROVED` | Profissional aprovou |
| `REJECTED` | Profissional rejeitou |
| `CANCELLED` | Cancelado por cliente ou profissional após aprovação |
| `COMPLETED` | Atendimento realizado |

---

## Endpoints

Base URL: `http://seu-servidor/`
Header de autenticação (quando necessário): `Authorization: Bearer {token}`

### 👤 Users — `/users`

#### `POST /users` — Criar conta
Público. Cria um novo usuário com seu perfil (profissional ou cliente).

**Body (Profissional):**
```json
{
  "name": "João Silva",
  "email": "joao@email.com",
  "phone": "11999999999",
  "password": "senha123",
  "role": "PROFESSIONAL",
  "professionId": 1,
  "bio": "Eletricista com 10 anos de experiência"
}
```

**Body (Cliente):**
```json
{
  "name": "Maria Santos",
  "email": "maria@email.com",
  "phone": "11988888888",
  "password": "senha123",
  "role": "CLIENT",
  "taxId": "123.456.789-00",
  "preferredPaymentMethod": "PIX"
}
```

**Resposta 201:**
```json
{
  "id": 1,
  "name": "João Silva",
  "email": "joao@email.com",
  "phone": "11999999999",
  "role": "PROFESSIONAL",
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "professionalProfile": {
    "professionId": 1,
    "professionName": "Eletricista",
    "bio": "Eletricista com 10 anos de experiência",
    "isAvailable": true
  },
  "clientProfile": null
}
```

---

#### `POST /users/login` — Login
Público. Retorna o token para usar nas demais requisições.

**Body:**
```json
{
  "email": "joao@email.com",
  "password": "senha123"
}
```

**Resposta 200:**
```json
{
  "id": 1,
  "name": "João Silva",
  "email": "joao@email.com",
  "token": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

#### `GET /users/me` — Dados do usuário logado
Autenticado. Retorna os dados completos do usuário pelo token.

**Resposta 200:**
```json
{
  "id": 1,
  "name": "João Silva",
  "email": "joao@email.com",
  "phone": "11999999999",
  "role": "PROFESSIONAL",
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "professionalProfile": {
    "professionId": 1,
    "professionName": "Eletricista",
    "bio": "Eletricista com 10 anos de experiência",
    "isAvailable": true
  }
}
```

---

#### `GET /users` — Listar usuários
Autenticado. Lista todos ou filtra por role.

**Query params:**
- `?role=PROFESSIONAL` — apenas profissionais
- `?role=CLIENT` — apenas clientes
- sem parâmetro — todos

---

#### `GET /users/{id}` — Buscar usuário por ID
Autenticado.

---

### 💼 Professionals — `/professionals`

#### `GET /professionals` — Buscar profissionais
Público. Busca profissionais com filtros opcionais. **Retorna o ID do profissional (user_id)**.

**Query params:**
- `?name=maria` — filtrar por nome (busca parcial)
- `?professionId=1` — filtrar por profissão (use `/professions` para obter IDs)
- `?serviceTypeName=web` — filtrar por tipo de serviço (busca parcial)
- Combine múltiplos filtros: `?name=maria&professionId=1`

**Resposta 200:**
```json
[
  {
    "id": 2,
    "name": "Maria Oliveira",
    "phone": "11999990002",
    "professionName": "Desenvolvedor",
    "bio": "Desenvolvedora fullstack especializada em Java e React",
    "isAvailable": true
  }
]
```

---

#### `GET /professionals/{id}` — Detalhes do profissional
Público. Retorna perfil completo do profissional pelo ID (user_id).

**Resposta 200:**
```json
{
  "id": 2,
  "name": "Maria Oliveira",
  "phone": "11999990002",
  "professionName": "Desenvolvedor",
  "bio": "Desenvolvedora fullstack especializada em Java e React",
  "isAvailable": true
}
```

---

#### `GET /professionals/{id}/services` — Serviços do profissional
Público. Retorna todos os tipos de serviço oferecidos pelo profissional.

**Resposta 200:**
```json
[
  {
    "id": 3,
    "name": "Desenvolvimento Web",
    "description": "Criação de sites e aplicações web",
    "price": 200.00
  },
  {
    "id": 4,
    "name": "Desenvolvimento de API",
    "description": "Criação de APIs REST e integração de sistemas",
    "price": 180.00
  }
]
```

---

### 🏢 Professions — `/professions`

#### `GET /professions` — Listar todas as profissões
Público. Retorna todas as profissões cadastradas para uso nos filtros de busca.

**Resposta 200:**
```json
[
  { "id": 1, "name": "Eletricista" },
  { "id": 2, "name": "Desenvolvedor" },
  { "id": 3, "name": "Encanador" },
  { "id": 4, "name": "Designer" },
  { "id": 5, "name": "Personal Trainer" }
]
```

---

### 🗂️ Service Types — `/service-types`

Todos os endpoints requerem autenticação.

#### `POST /service-types` — Criar tipo de serviço
Autenticado. Cria um serviço vinculado ao profissional autenticado. O `price` define o valor cobrado por esse serviço.

**Body:**
```json
{
  "name": "Instalação Elétrica",
  "description": "Instalação de tomadas, disjuntores e fiação",
  "price": 150.00
}
```

**Resposta 201:**
```json
{
  "id": 1,
  "name": "Instalação Elétrica",
  "description": "Instalação de tomadas, disjuntores e fiação",
  "price": 150.00
}
```

---

#### `GET /service-types` — Listar meus serviços
Retorna apenas os serviços do usuário autenticado.

**Resposta 200:**
```json
[
  {
    "id": 1,
    "name": "Instalação Elétrica",
    "description": "Instalação de tomadas, disjuntores e fiação",
    "price": 150.00
  }
]
```

---

#### `GET /service-types/{id}` — Buscar serviço por ID
Retorna apenas se o serviço pertencer ao usuário autenticado. `404` caso contrário.

---

### ⭐ Ratings — `/ratings`

#### `POST /ratings` — Avaliar profissional
Autenticado. Apenas clientes podem avaliar profissionais.

**Body:**
```json
{
  "professionalId": 1,
  "score": 5,
  "comment": "Trabalho excelente, muito profissional!"
}
```

**Resposta 201:**
```json
{
  "id": 1,
  "score": 5,
  "comment": "Trabalho excelente, muito profissional!",
  "clientId": 2,
  "clientName": "Maria Santos",
  "professionalId": 1,
  "professionalName": "João Silva",
  "createdAt": "2026-03-12T14:30:00"
}
```

---

#### `GET /ratings/professional/{id}` — Avaliações do profissional
Público. Retorna todas as avaliações recebidas por um profissional. A média é atualizada automaticamente.

**Resposta 200:**
```json
[
  {
    "id": 1,
    "score": 5,
    "comment": "Trabalho excelente!",
    "clientId": 2,
    "clientName": "Maria Santos",
    "professionalId": 1,
    "professionalName": "João Silva",
    "createdAt": "2026-03-12T14:30:00"
  },
  {
    "id": 2,
    "score": 4,
    "comment": "Bom trabalho",
    "clientId": 3,
    "clientName": "Pedro Costa",
    "professionalId": 1,
    "professionalName": "João Silva",
    "createdAt": "2026-03-11T10:15:00"
  }
]
```

---

#### `GET /ratings/my-ratings` — Minhas avaliações
Autenticado. Retorna todas as avaliações feitas pelo cliente autenticado.

**Resposta 200:**
```json
[
  {
    "id": 1,
    "score": 5,
    "comment": "Trabalho excelente!",
    "clientId": 2,
    "clientName": "Maria Santos",
    "professionalId": 1,
    "professionalName": "João Silva",
    "createdAt": "2026-03-12T14:30:00"
  }
]
```

---

### 📅 Appointments — `/appointments`

Todos os endpoints requerem autenticação.

#### `POST /appointments` — Criar agendamento
O usuário autenticado deve ser o profissional **ou** o cliente. Pode incluir múltiplos serviços. O valor total é calculado automaticamente pela soma dos preços dos serviços.

**Body:**
```json
{
  "professionalId": 1,
  "clientId": 2,
  "serviceTypeIds": [1, 3],
  "scheduleDate": "2026-03-20T14:00:00"
}
```

**Resposta 201:**
```json
{
  "id": 1,
  "professional": { "id": 1, "name": "João Silva" },
  "client": { "id": 2, "name": "Maria" },
  "services": [
    { "id": 1, "name": "Instalação Elétrica", "price": 150.00 },
    { "id": 3, "name": "Inspeção", "price": 50.00 }
  ],
  "totalAmount": 200.00,
  "scheduleDate": "2026-03-20T14:00:00",
  "requestDate": "2026-03-11T10:00:00",
  "status": "PENDING"
}
```

---

#### `GET /appointments` — Listar agendamentos
Filtra por role do usuário autenticado.

**Query params:**
- `?role=professional` — agendamentos onde é profissional
- `?role=client` — agendamentos onde é cliente
- sem parâmetro — todos

---

#### `GET /appointments/{id}` — Buscar agendamento por ID
Retorna apenas se o usuário autenticado for profissional ou cliente do agendamento. `404` caso contrário.

---

#### `PATCH /appointments/{id}/approve` — Aprovar agendamento
Autenticado. Apenas o **profissional** do agendamento pode aprovar. O status deve estar em `PENDING`.

**Resposta 200:** appointment com `"status": "APPROVED"`

---

#### `PATCH /appointments/{id}/reject` — Rejeitar agendamento
Autenticado. Apenas o **profissional** pode rejeitar. O status deve estar em `PENDING`.

**Resposta 200:** appointment com `"status": "REJECTED"`

---

#### `PATCH /appointments/{id}/cancel` — Cancelar agendamento
Autenticado. Cliente **ou** profissional pode cancelar. O status deve estar em `APPROVED`.

**Resposta 200:** appointment com `"status": "CANCELLED"`

---

#### `PATCH /appointments/{id}/complete` — Concluir agendamento
Autenticado. Apenas o **profissional** pode marcar como concluído. O status deve estar em `APPROVED`.

**Resposta 200:** appointment com `"status": "COMPLETED"`

---

## Resumo rápido

| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| POST | `/users` | ❌ | Criar conta |
| POST | `/users/login` | ❌ | Login |
| GET | `/users/me` | ✅ | Dados do usuário logado |
| GET | `/users` | ✅ | Listar usuários |
| GET | `/users/{id}` | ✅ | Buscar usuário |
| GET | `/professions` | ❌ | Listar profissões |
| GET | `/professionals` | ❌ | Buscar profissionais com filtros |
| GET | `/professionals/{id}` | ❌ | Detalhes do profissional |
| GET | `/professionals/{id}/services` | ❌ | Serviços do profissional |
| POST | `/ratings` | ✅ | Avaliar profissional |
| GET | `/ratings/professional/{id}` | ❌ | Avaliações do profissional |
| GET | `/ratings/my-ratings` | ✅ | Minhas avaliações |
| POST | `/service-types` | ✅ | Criar serviço |
| GET | `/service-types` | ✅ | Listar meus serviços |
| GET | `/service-types/{id}` | ✅ | Buscar serviço |
| POST | `/appointments` | ✅ | Criar agendamento |
| GET | `/appointments` | ✅ | Listar agendamentos |
| GET | `/appointments/{id}` | ✅ | Buscar agendamento |
| PATCH | `/appointments/{id}/approve` | ✅ | Aprovar agendamento (profissional) |
| PATCH | `/appointments/{id}/reject` | ✅ | Rejeitar agendamento (profissional) |
| PATCH | `/appointments/{id}/cancel` | ✅ | Cancelar agendamento (cliente ou profissional) |
| PATCH | `/appointments/{id}/complete` | ✅ | Concluir agendamento (profissional) |

---

## Testes

```bash
./gradlew test --tests IntegrationTest
```

## Seed do banco de dados

Na primeira execução, a apl/usaicação popula automaticamente o banco com dados de exemplo (profissões, usuários, serviços e agendamentos). Todos os usuários têm senha padrão: `123456`.

## Como rodar?

Para produção:
```bash
docker compose --profile prod up
```

Para desenvolvimento (c/ hot reload ativado):
```bash
docker compose --profile dev up
```

## Fluxo de Pull Requests

### Como abrir um PR

1. **Faça push da sua branch:**
```bash
git push origin sua-branch
```

2. **Abra o PR usando GitHub CLI:**
```bash
gh pr create --title "seu titulo" --body "sua descricao" --head sua-branch --base target-branch
```

3. **Substitua `target-branch` por:**
   - `develop` — para mudanças de desenvolvimento
   - `release` — para preparar uma versão (release candidates)
   - `main` — para fazer merge de releases finalizadas

### Exemplo prático:

```bash
# Feature de desenvolvimento → develop
gh pr create --title "feat: add hot reload" --body "Configures Docker with hot reload support" --head feature/hot-reload --base develop

# Release candidate → release
gh pr create --title "chore: version 1.0.0" --body "Release candidate v1.0.0" --head develop --base release

# Release finalizada → main
gh pr create --title "Merge release into main" --body "Release v1.0.0" --head release --base main
```

**Padrão neste projeto:**
- `feature/*` → faz PR para `develop` (opcional, mas recomendado)
- `develop` → faz PR para `release` (com testes passing)
- `release` → faz PR para `main` (versão final testada)

