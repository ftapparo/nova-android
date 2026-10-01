# AGENTS.md

## Purpose

This file is the operational guide for AI coding agents working on the **Nova Residence mobile app (Android)** — the residents' and staff app for Condomínio Nova Residence.

It has two parts:

1. **Engineering rules** — how code in this repository is organized and changed.
2. **Backend contract** — how the app talks to `nova-api` v3, the only backend it uses.

Keep the working context minimal. Do not read large parts of the repository unless the task requires it. Prefer local discovery over global exploration.

---

## Ecosystem Overview

```text
Android app ──HTTPS──► nova-api v3 (/v3/api/*)
                          ├── Supabase Auth (GoTrue)   internal only, never called by the app
                          ├── Postgres                 internal only
                          ├── Firebird (condo ERP)     internal only
                          ├── nova-tag (gates, RFID)   internal only
                          ├── nova-cie (fire panel)    internal only
                          └── exhaust modules          internal only
```

- The app talks **only** to `nova-api` v3. Never call Supabase Auth, Google APIs (beyond Sign-In), databases or devices directly.
- `nova-api` v2 exists for the legacy web panel. **The app never uses v2.**
- The live OpenAPI spec is the source of truth for request/response shapes: `GET <V3_BASE_URL>/v3/apispec_1.json` (Swagger UI at `/v3/swagger`, available while `SWAGGER_V3_ENABLED=true` on the server). When this file and the spec disagree, the spec wins — then update this file.
- Local copies on the development machine: `D:\Git\Nova Residence\API\src\v3\openapi.json` (hand-written spec) and `APP-GUIDE.md` (backend guide for this app, including WebSocket `/v3/ws`, CIE commands and `/me/settings`). Backend changes needed by the app go only in `src/v3` of that repository — never `src/v2`.

---

## Repository Structure

Feature-first modules, the same principle used by the backend projects.

```text
app/src/main/java/<package>/
├── feature/              # One package per business feature
│   ├── auth/             # login, signup, Google, password
│   ├── residence/        # link status, units, blocked state
│   ├── exhaust/          # BBQ exhaust fans
│   ├── fire/             # fire panel (CIE), read-only for residents
│   └── gate/             # gates (TAG), staff only
├── core/                 # Genuinely shared infrastructure
│   ├── network/          # HTTP client, envelope parsing, auth interceptor
│   ├── session/          # token storage and refresh
│   └── ui/               # design system, shared components
└── App.kt
```

Inside a feature, keep screen, state, data access and models together:

```text
feature/exhaust/
├── ExhaustScreen.kt
├── ExhaustViewModel.kt
├── ExhaustRepository.kt
└── ExhaustModels.kt
```

Do not distribute a feature across global `ui/`, `data/`, `domain/` directories.

---

## UI e design

Leia `DESIGN_GUIDE.md` antes de alterar interface. Ele define a hierarquia tipográfica, espaçamento, componentes e adaptação a janelas. Consulte os nós correspondentes pelo MCP do Figma e compare com o aplicativo renderizado; o Figma é uma referência visual revisável. Quando houver divergência, aplique o guia, valide no aparelho e registre a correção necessária no Figma. Mantenha textos funcionais em fonte Android/Material 3 e tokens compartilhados em `core/ui`.

---

## Context Strategy

For every task:

1. Identify the feature involved.
2. Inspect files inside that feature package.
3. Read a local `AGENTS.md` in the feature when present.
4. Read the **Backend contract** section below only for the endpoints the task touches.
5. Inspect `core/` or other features only when a dependency requires it.
6. Use repository-wide search only when local discovery is insufficient.

---

## Core Architecture Rules

### Feature Locality

Feature-specific code stays in `feature/<name>/`. Move code to `core/` only when two or more features genuinely use it.

### Cohesion

Keep related code together. Separate code when responsibilities differ, not because a pattern allows another file. Do not optimize for minimum file count, and do not create files merely to keep files small.

### Progressive Complexity

Start with the simplest structure that correctly represents the feature. Do not introduce `domain/`, `usecase/`, `data/` layers, mappers or DI modules preemptively. Add them when a real requirement appears (multiple data sources, offline cache, test substitution).

### Boundaries

- UI does not know HTTP details: screens talk to a ViewModel, the ViewModel to a repository.
- Only `core/network` knows the response envelope and HTTP status codes; features receive typed results.
- Only `core/session` reads or writes tokens.

### Abstractions

Do not create interfaces with a single implementation, factories without construction complexity, wrappers that only forward calls, or DTO/mapper pairs for every small type. Introduce an abstraction when it provides concrete value (test substitution, external boundary, multiple implementations).

---

## Naming

Use predictable, domain-specific names: `ExhaustRepository`, `ResidenceScreen`, `SessionStore`. Avoid `Manager`, `Helper`, `Utils`, `Common` when a specific name is possible.

Domain vocabulary (use these terms consistently):

| Term | Meaning |
|---|---|
| Unidade | Apartment. Shown as `label`, e.g. `A-124` (tower A, apartment 124) |
| Torre / quadra | Tower `A`, `B` or `C` |
| Prumada | Vertical line of apartments sharing the same final digit (A-114, A-124… → prumada A4) |
| Morador | Resident |
| Proprietário / locatário | Owner / tenant |
| Portaria / porteiro | Front desk / doorman |
| Síndico | Building manager (the janitor uses this role too) |
| CIE | Fire alarm panel |
| Exaustor | BBQ exhaust fan, one per prumada |
| Vínculo | Link between the app account and the person record in the condo ERP |

User-facing text is **Brazilian Portuguese**.

---

## Scope Discipline

Keep changes focused on the requested task. Do not perform unrelated refactoring, rename unrelated files, reorganize unrelated features, add libraries without necessity, or change architecture as a side effect of a small feature. If a broader change is needed, state why before doing it.

---

## Dependencies

Before adding a dependency, verify the requirement cannot reasonably be met with what is already in the project or the Android platform. Prefer established, maintained libraries (AndroidX, Kotlin official libraries, OkHttp/Retrofit or Ktor). Do not add a library for a few lines of straightforward code.

---

## Comments

Comments explain **why**, not **what**.

```kotlin
// The refresh token is single-use: two concurrent refreshes would
// invalidate each other, so refresh is serialized behind a mutex.
```

---

## Error Handling

- Never silently swallow errors.
- Map backend errors to user-facing states by `error.type` (see contract), never by parsing `detail` text.
- Never show stack traces, tokens or internal identifiers to the user.

---

## Security

- Never hardcode credentials, API secrets or tokens. The Google **web client ID** is not a secret and may live in build config; the client **secret** must never be in the app.
- Store tokens only through `core/session`, encrypted with a key held in the Android Keystore. Never in plain `SharedPreferences`, logs or crash reports.
- Never log tokens, passwords, temporary passwords or recovery codes.
- Treat everything from the server as untrusted input for UI rendering.
- Use HTTPS in release builds; any cleartext exception is debug-only.

---

## Testing and Validation

### Commits

Follow `skills/commit/SKILL.md` for commit preparation, changelog updates and approval. Commit subjects and bodies must always be written in Brazilian Portuguese.

Validate at the smallest useful scope first: unit tests for the changed logic, then feature tests, then the full suite.

Default validation before considering a change complete:

```bash
./gradlew lint
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

If a validation step cannot be run, state explicitly which one was skipped.

Test behavior, not implementation details. Do not remove or weaken tests to make a change pass.

**Never point automated tests at the production API.** Tests that need HTTP use a fake server (e.g. OkHttp `MockWebServer`) with responses shaped like the envelope below. Commands to exhaust fans act on real hardware.

---

## Before Completing a Task

Verify:

- the requested behavior is implemented;
- changes stay within the right feature boundaries;
- no unnecessary abstractions were introduced;
- no unrelated refactoring was performed;
- tests exist or were updated and pass;
- lint and build pass;
- this file was updated if the backend contract or the architecture changed.

---

# Backend Contract — nova-api v3

## Base URL

`<V3_BASE_URL>` comes from build configuration (per build type). Never hardcode it in feature code. All paths below are relative to `<V3_BASE_URL>/v3/api`.

## Response Envelope

Every response, success or error, is JSON in this shape.

Success:

```json
{
  "success": true,
  "data": { },
  "meta": {
    "timestamp": "2026-09-28T22:38:37.423Z",
    "requestId": "req-1",
    "version": "v3",
    "pagination": { "limit": 25, "offset": 0, "total": 137 }
  }
}
```

`meta.pagination` exists only on paginated endpoints.

Error (RFC 7807, adapted):

```json
{
  "success": false,
  "error": {
    "type": "urn:nova-api:unauthorized",
    "title": "Autenticação necessária",
    "status": 401,
    "detail": "Credenciais inválidas.",
    "instance": "/v3/api/auth/login",
    "validationErrors": [{ "path": "/email", "message": "..." }]
  },
  "meta": { "timestamp": "...", "requestId": "...", "version": "v3" }
}
```

`error.type` is a closed set — branch on it:

| `type` | Status | App reaction |
|---|---|---|
| `urn:nova-api:validation-error` | 400 | Show field errors from `validationErrors` |
| `urn:nova-api:unauthorized` | 401 | Wrong credentials on login; elsewhere, refresh once, then log out |
| `urn:nova-api:forbidden` | 403 | See "Mandatory password change" below; otherwise "Acesso negado" |
| `urn:nova-api:not-found` | 404 | Resource missing or not accessible to this user |
| `urn:nova-api:conflict` | 409 | e.g. e-mail already registered |
| `urn:nova-api:rate-limited` | 429 | Honor the `Retry-After` header (seconds); disable the action meanwhile |
| `urn:nova-api:upstream-error` | 502/503 | Service or device unavailable; offer retry |
| `urn:nova-api:internal-error` | 500 | Generic error message |

`detail` is safe to display but is not stable — do not parse it. Include `meta.requestId` in bug reports.

## Authentication

- Every protected request sends `Authorization: Bearer <accessToken>`.
- **Access token** lives 15 minutes (`expiresIn: 900`, `expiresAt` in epoch seconds).
- **Refresh token** is single-use: every refresh returns a new pair. Serialize refreshes (one in flight at a time) and replace both tokens atomically.
- Refresh proactively shortly before `expiresAt`, or reactively on 401: refresh once and retry the request once; if the refresh fails, clear the session and go to login.
- Logout revokes the refresh token on the server; an already-issued access token remains valid until it expires. Always clear local tokens on logout even if the call fails.

Session response (`SessionData`), returned by login, signup, Google, refresh and password endpoints:

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "kxco2lm2n5hd",
  "tokenType": "bearer",
  "expiresIn": 900,
  "expiresAt": 1790636017,
  "user": {
    "id": "9152eea2-c5e7-4fec-a296-85f20c68f51f",
    "email": "morador@gmail.com",
    "role": null,
    "mustChangePassword": false
  }
}
```

### Endpoints

| Method | Path | Auth | Body | Returns |
|---|---|---|---|---|
| POST | `/auth/signup` | — | `{ email, password }` (password ≥ 8) | 201 `SessionData` |
| POST | `/auth/login` | — | `{ email, password }` | 200 `SessionData` |
| POST | `/auth/google` | — | `{ idToken, nonce? }` | 200 `SessionData` |
| POST | `/auth/refresh` | — | `{ refreshToken }` | 200 `SessionData` |
| POST | `/auth/logout` | Bearer | — | 200 `null` |
| GET | `/auth/me` | Bearer | — | 200 `AuthUser` |
| POST | `/auth/password/change` | Bearer | `{ currentPassword, newPassword }` | 200 `SessionData` |
| POST | `/auth/password/recover` | — | `{ email }` | 202 `null` (always, even if the e-mail has no account) |
| POST | `/auth/password/recover/confirm` | — | `{ email, code, newPassword }` (`code` = 6 digits from the e-mail, valid 15 min) | 200 `SessionData` |

Rate limits (all answer 429 with `Retry-After`): login 20 per IP and 5 wrong passwords per e-mail per 15 min; signup 5 per IP per hour; recovery 3 e-mails per address per hour; recovery code 5 wrong attempts per 15 min.

### Sign in with Google

- Two OAuth clients exist in Google Cloud, with different jobs. **Do not create a new web client** — use the existing one:

  | Client | Status | Role | Where it appears |
  |---|---|---|---|
  | Web — `628782431663-jcdpiqiph63a50kk21iin4b0dfk32gga.apps.googleusercontent.com` | Exists; also configured in the backend (`nova-auth`) | Audience of the `idToken` — the backend only accepts tokens issued for it | App code: `serverClientId` (build config) |
  | Android — package `br.com.novaresidence` + signing SHA-1 | One per signing key (debug per machine, release, Play App Signing) | Proves to Google the request comes from this signed app | **Nowhere in the code** |

- Use **Credential Manager** (`GetGoogleIdOption` / Sign in with Google) with `serverClientId` = the **web** client ID above, never the Android client ID. The web client ID is not a secret; the web client **secret** is backend-only and must never be in the app or its repository.
- If sign-in fails with a configuration/developer error, the usual cause is a missing Android client for the SHA-1 of the key that signed the running build.
- Send the returned `idToken` to `POST /auth/google`.
- `nonce`: send it **only if** the app set a nonce in the Google request. If you do use one, pass the **raw** nonce to the API and its SHA-256 hash to Google. Sending a nonce that is not in the token makes the login fail.
- First Google sign-in creates the account (no role, pending link). If an account with the same e-mail already exists, the Google identity is attached to it — same user id.

### Mandatory password change

Staff can reset a user's password to a temporary one. While `user.mustChangePassword` is `true`:

- Only `/auth/password/change`, `/auth/me` and `/auth/logout` work; every other endpoint answers **403** with `error.title = "Troca de senha obrigatória"`.
- After login, if `mustChangePassword` is `true`, go straight to the change-password screen (the temporary password is the `currentPassword`).
- The change returns a fresh `SessionData` without the flag — replace the stored session with it.

## Account State — Residence

Right after login (and on app resume), call `GET /residence/me` (Bearer). It decides what the app enables.

```json
{
  "status": "ativo",
  "blocked": false,
  "person": { "sequencia": 91, "nome": "FULANO DE TAL" },
  "units": [
    {
      "sequencia": 97, "quadra": "A", "lote": "124", "label": "A-124",
      "morador": true, "proprietario": true, "locatario": false,
      "blocked": false, "blockedReason": null
    }
  ]
}
```

| `status` | Meaning | UI |
|---|---|---|
| `pendente` | Account not yet linked to the condo record by staff | "Cadastro em análise pela administração" — nothing enabled |
| `ativo` | Linked, has at least one unit | Show units; enable actions only for units with `blocked: false` |
| `sem-unidade` | Linked, but no unit today (moved out) | "Seu cadastro não possui unidade ativa" — nothing enabled |

- `blocked: true` at the top level → the whole app is read-only/disabled for resident features. **Login still works.**
- Per unit, `blocked: true` → show the unit but disable every action on it. Messages by `blockedReason`:
  - `nao-morador` — "Você não consta como morador desta unidade."
  - `unidade-com-inquilino` — "Esta unidade possui inquilino cadastrado; o acesso é do inquilino."
  - `cadastro-inconsistente` — "Seu cadastro precisa ser regularizado na administração."
- The server caches this for up to 2 minutes; a change made by staff may take that long to appear.

## Roles and Permissions

`user.role` is `null` or `morador` for regular accounts, or a staff role: `porteiro`, `sindico`, `admin` (confirmed in the v3 OpenAPI). A resident is an account with `role: null` or `morador` whose residence is `ativo` with at least one unblocked unit. Staff may also be residents.

| Feature | Endpoints | Who |
|---|---|---|
| Exhaust fans | `/exhausts…` | Everyone. Staff: all 24. Resident: only the prumada of their unblocked units |
| Fire panel (CIE) | `/cie/…` (read-only) | Staff, and residents with an active unit |
| Gates (TAG) | `/tag/…` | `porteiro`, `sindico`, `admin` |
| Accounts and links | `/auth/users`, `/residence/links…` | `sindico`, `admin` |
| Staff password reset | `/auth/password/staff-reset` | `porteiro` (residents only), `sindico` (residents and doormen), `admin` (anyone) |

Hide features the user cannot use, but always handle 403/404 anyway — the server is the authority.

## Exhaust Fans

### Implementação Android das telas 14 a 20 (Figma de 30/09/2026)

- O app envia `POST /exhausts/{id}/on` com `{ "minutes": 180 }`; a API agenda e executa o desligamento. O app nunca chama `/off` automaticamente.
- **Reiniciar tempo** (tela 16): reenviar o mesmo `/on` com o exaustor ligado recalcula o `expiresAt` a partir de agora, sem mexer no relé. Só habilitado com `processStatus: executado`; o botão fica desabilitado durante o envio e nunca repete sozinho.
- `command` (`ligar` | `desligar` | `null`) distingue "Ligando" (15) de "Desligando" (17): durante o desligamento a API já devolve `on: false` com `processStatus: iniciando`.
- `maintenance: true` → tela 20 (selo "Manutenção", sem ação). `/on` responde 409 nesse caso; o app mostra o `detail` e não repete. O painel web v2 ignora a manutenção.
- O contador usa somente `expiresAt` retornado pela API. Ao chegar a zero, apresenta finalização e continua consultando; somente a resposta do servidor confirma que desligou.
- As consultas ocorrem enquanto a área logada está visível (home e tela do exaustor, pois a home mostra o estado — Figma 08b): a cada 5 segundos, ou 2 segundos durante transições. Respeitar `Retry-After`; nenhuma falha causa repetição automática do acionamento. O evento WebSocket `exhaust.changed` pode substituir essa consulta numa etapa futura.
- A seleção do equipamento intersecta torre/final da unidade ativa com a lista autorizada de `/exhausts`. Não fabricar equipamentos ou permissões ausentes dessa lista.
- Quando a resposta for inconclusiva, usar os textos aprovados “Não foi possível confirmar a ativação” e “Atualize o estado antes de tentar novamente”, sem afirmar proteção ou encerramento físico não confirmados.

| Method | Path | Body | Returns |
|---|---|---|---|
| GET | `/exhausts` | — | 200 `Exhaust[]` (only the ones this user may see) |
| POST | `/exhausts/{id}/on` | `{ minutes? }` (1–1440; omit = stays on until turned off; resend while on = restart timer) | 200 `Exhaust`; 409 in maintenance |
| POST | `/exhausts/{id}/off` | — | 200 `Exhaust` |
| PUT | `/exhausts/{id}/maintenance` | `{ maintenance }` | 200 `Exhaust` — `sindico`/`admin` only, not used by the app yet |

```json
{ "id": "A4", "tower": "A", "final": 4, "on": true, "expiresAt": 1790636017000, "processStatus": "executado", "moduleOnline": true, "command": "ligar", "maintenance": false }
```

- `id` = tower + final digit of the apartment (A-124 → `A4`). The list already contains only what the user may operate — do not compute access in the app.
- `expiresAt` is epoch **milliseconds** (auto-off), or `null`.
- `processStatus`: `iniciando` | `executado` | `erro` | `null` — show progress/error for the last command.
- `moduleOnline: false` → disable the controls and say the device is offline.
- Commands act on **physical hardware**: disable the button while a command is in flight; 10 commands per minute per account (429 + `Retry-After`); 503 = module offline.
- 404 on a command means "not yours or does not exist" — refresh the list.

## Preferências locais e telas 21 a 23

- A unidade padrão foi removida do Figma e do app (30/09/2026). A unidade ativa é escolhida na tela 22 ("Selecionar unidade") com um toque; a seleção vale durante a sessão enquanto vinculada. No acesso seguinte vale a regra inicial (`selectResidenceUnit`). O seletor continua mostrando unidades bloqueadas com os motivos do contrato.
- Configurações (21) exibe "Código Freedom" = `person.sequencia`. É o código do morador no cadastro do condomínio e ajuda no atendimento; exceção consciente à regra de não exibir identificadores internos.
- Idioma é local; inglês e espanhol ficam desabilitados até existir tradução completa. `/me/settings` existe na API e pode sincronizar a preferência no futuro.
- As preferências residenciais não contêm tokens; ficam em `feature/residence`. O armazenamento de sessão continua exclusivo de `core/session`.
- Manter os dados demonstrativos fora das integrações reais. Mockups eventualmente necessários por ausência de contrato devem ser identificados como simulação e documentados na entrega.

## Fire Panel (CIE) — read-only

`GET /cie/status`, `/cie/panel`, `/cie/alarms/active`, `/cie/counters/blocks`, `/cie/counters/outputs`, and `/cie/logs?type=&limit=&cursor=` (cursor-based: pass the cursor returned in the previous page's `data`, not `meta.pagination`). Shapes are in the OpenAPI spec. There are no command endpoints for the app.

## Gates (TAG) — staff only

`GET /tag/gate/state?numeroDispositivo=<n>`, `GET /tag/cache?numeroDispositivo=<n>&type=<positive|negative|all|whitelist|blacklist>`. Read-only for now.

## Staff Operations

Available only to the roles in the permissions table; hide them otherwise.

| Method | Path | Body | Returns |
|---|---|---|---|
| GET | `/auth/users?page=1&perPage=50` | — | Accounts (`AuthUser` + `createdAt`) |
| POST | `/auth/users` | `{ email, password, role }` | 201 `AuthUser` (only `admin` can create `admin`) |
| POST | `/auth/password/staff-reset` | `{ accountId }` **or** `{ personSequencia }` | `{ accountId, email, temporaryPassword }` |
| GET | `/residence/links` | — | Account ↔ person links |
| PUT | `/residence/links/{accountId}` | `{ personSequencia }` | Link created/replaced (404 person not found, 409 person already linked) |
| DELETE | `/residence/links/{accountId}` | — | Link removed |

- Show the temporary password once, on screen, for the staff member to hand over. Never store, log or share it through the app.

---

## Primary Principle

When choosing between two implementations, prefer the one that lets a future developer or agent understand and modify the feature while loading the least amount of unrelated context.

> Keep related code together.
> Preserve meaningful boundaries.
> Prefer explicit, simple structures.
> Load context progressively.
> Optimize for relevant context, not minimum file count.
