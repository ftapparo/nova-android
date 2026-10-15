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
│   ├── notification/     # push preferences screen (Settings → Notificações)
│   └── gate/             # gates (TAG), staff only
├── core/                 # Genuinely shared infrastructure
│   ├── network/          # HTTP client, envelope parsing, auth interceptor
│   ├── push/             # FCM service, channels, device registration, permission
│   ├── realtime/         # WebSocket /v3/ws client (foreground only)
│   ├── session/          # token storage and refresh
│   └── ui/               # design system, shared components
├── NovaApplication.kt    # process graph (session, API client, push) — the FCM service runs without an Activity
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

Leia `DESIGN_GUIDE.md` antes de alterar interface. Ele define a hierarquia tipográfica, espaçamento, componentes e adaptação a janelas. Consulte os nós correspondentes pelo MCP do Figma e compare com o aplicativo renderizado; o Figma é uma referência visual revisável. Quando houver divergência, aplique o guia, valide no aparelho e registre a correção necessária no Figma. Mantenha textos funcionais em fonte Android/Material 3 e tokens compartilhados em `core/ui`. O app usa densidade proporcional ao frame de 412 dp do Figma em telas mais estreitas (ver `DESIGN_GUIDE.md` §6); use os valores do Figma em dp/sp sem compensações manuais por largura. Para comparar com o Figma, capture o aparelho e o PNG do nó lado a lado na mesma escala.

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
| `urn:nova-api:forbidden` | 403 | See "Mandatory password change" below; exhaust location refusals by stable `title` (see Exhaust Fans); otherwise "Acesso negado" |
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
    "mustChangePassword": false,
    "mustAcceptTerms": false
  }
}
```

### Endpoints

| Method | Path | Auth | Body | Returns |
|---|---|---|---|---|
| POST | `/auth/signup` | — | `{ email, password, acceptedTermsVersion }` (password ≥ 8; version = current Terms, else 400 and no account) | 201 `SessionData` |
| POST | `/auth/login` | — | `{ email, password }` | 200 `SessionData` |
| POST | `/auth/google` | — | `{ idToken, nonce?, acceptedTermsVersion? }` | 200 `SessionData` |
| POST | `/auth/refresh` | — | `{ refreshToken }` | 200 `SessionData` |
| POST | `/auth/logout` | Bearer | — | 200 `null` |
| GET | `/auth/me` | Bearer | — | 200 `AuthUser` |
| DELETE | `/auth/me` | Bearer | `{ confirm: true }` | 200 `null` — deletes the own account; staff get 403; 503 = nothing deleted |
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

### Terms of Use and Privacy Policy

- `GET /legal` (public, no token): current documents — `document` (`termo-de-uso` | `politica-de-privacidade`), `title`, `version`, `effectiveDate`, `sha256` of the official PDF, `htmlUrl`, `pdfUrl`. The public pages live outside `/v3/api` (`/termos-de-uso`, `/politica-de-privacidade`, `/<doc>/pdf`), so store links do not change with the API version. The PDF is the official document; the HTML page is for reading on the phone.
- The app (`feature/legal`) shows the HTML page in a restricted WebView (no JavaScript, no file access, no cache — the unversioned URL changes content with each new version and the acceptance screen records the current one; other links and the PDF open outside the app). The server also sends `Cache-Control: no-cache` for the pages and the unversioned PDF; `/<doc>/<version>/pdf` is immutable. The welcome (Google can create the account) and signup screens say "Ao continuar, você concorda com o Termo de Uso e a Política de Privacidade" with links; signup sends the current version, Google sends it when the catalog loaded (otherwise the account comes back with `mustAcceptTerms`). The version is never hard-coded and is fetched from `GET /legal` at the moment of signup, Google sign-in and the acceptance screen (the in-memory catalog may be days old when a new version is published).
- `user.mustAcceptTerms: true` (older account or new Terms version) → acceptance screen before the logged-in area, `POST /legal/terms/accept` `{ version }`, then `GET /auth/me`. 409 = a newer version was published: reload `GET /legal` and show it. The API does not block other routes without acceptance.
- Public account-deletion page (Google Play requirement): `/excluir-conta` on the same host.

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
  "person": { "sequencia": 91, "nome": "FULANO DE TAL", "cpfMasked": "•••.456.789-••" },
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

### Implementação Android das telas 19 a 24 (Figma de 12/10/2026)

- O exaustor é uma aba do menu inferior (sem botão voltar). O cartão "Unidade ativa · Equipamento" abre a seleção de unidade; as ilustrações por estado ficam em `res/drawable-<densidade>/nova_exhaust_*.webp` e `nova_fire_*.webp` (geradas dos originais do Figma, ver `docs/revisao-figma-e-integracao.md`) e encolhem em telas mais baixas que o frame.
- O app envia `POST /exhausts/{id}/on` com `{ "minutes": 180 }`; a API agenda e executa o desligamento. O app nunca chama `/off` automaticamente.
- **Reiniciar tempo** (tela 21): reenviar o mesmo `/on` com o exaustor ligado recalcula o `expiresAt` a partir de agora, sem mexer no relé. Só habilitado com `processStatus: executado`; o botão fica desabilitado durante o envio e nunca repete sozinho.
- `command` (`ligar` | `desligar` | `null`) distingue "Ligando" (20) de "Desligando" (22): durante o desligamento a API já devolve `on: false` com `processStatus: iniciando`.
- `maintenance: true` → tela 24 (selo "Manutenção", sem ação). `/on` responde 409 nesse caso; o app mostra o `detail` e não repete. O painel web v2 ignora a manutenção.
- O contador ("Tempo restante · 02:59:00") usa somente `expiresAt` retornado pela API. Ao chegar a zero, apresenta finalização e continua consultando; somente a resposta do servidor confirma que desligou.

#### Localização para ligar o exaustor (decidido em 13/10/2026, implementado em 14/10/2026)

O morador só liga ou reinicia o exaustor dentro da área do condomínio.

- **A API é a autoridade.** No toque em "Ligar exaustor" ou "Reiniciar tempo", `ExhaustLocator` (`feature/exhaust/ExhaustLocation.kt`) lê a localização uma vez e o app envia `location: { latitude, longitude, accuracy, mock }` no `POST /exhausts/{id}/on`. A área é um círculo (centro + raio) configurado no servidor (`EXHAUST_GEOFENCE_*`), nunca no app; a API aceita quando o círculo de precisão toca a área e `accuracy` ≤ 100 m (configurável).
- **Equipe isenta:** `porteiro`, `sindico` e `admin` não enviam localização e o app nem pede a permissão (`ExhaustViewModel.start(..., staff)`). Quem é equipe e também morador conta como equipe.
- **Recusas da API:** 403 `forbidden` com `title` estável — "Localização necessária", "Localização simulada", "Localização imprecisa", "Fora da área do condomínio" (`locationIssueFor`). Nada chegou ao relé: não é perda de acesso (não chama `accessChanged`) nem comando incerto. Área não configurada no servidor = 503. As recusas contam no limite de 10 comandos por minuto.
- **Desligar nunca exige localização** (segurança). O app continua sem chamar `/off` automaticamente.
- **Permissão:** `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` (o Android 12+ exige pedir as duas), só "durante o uso", pedida a partir do aviso da tela. Sem localização em segundo plano. Sem permissão, com só a aproximada ou com a localização desligada, nada é enviado e a tela mostra o aviso com a ação (`LocationEnabler`: pedido do sistema enquanto ele ainda aparece, depois as configurações do app; diálogo do Play Services para ligar a localização). Na volta, o aviso some e o comando só sai com novo toque.
- **Precisão:** provedor do Google Play Services (`play-services-location`, `getCurrentLocation` com alta precisão, leitura de até 30 s, espera de 15 s). Localização simulada (`Location.isMock` / `isFromMockProvider`) é enviada com `mock: true` e a API recusa.
- **Telas:** Figma 25c–25i (localização necessária, não permitida, aproximada, desligada, fora do condomínio, imprecisa, simulada); o botão mostra "Verificando localização..." durante a leitura. O aviso só substitui os estados com botão de comando (desligado e ligado).
- **Privacidade:** a localização só valida o comando; não é armazenada como trajeto nem registrada em log, no app ou na API (que registra só o resultado). Coberta pela Política de Privacidade 1.2 (§3.8 e §8-A); declarar "Local exato" na "Segurança dos dados" (`docs/seguranca-dos-dados.md`).
- As consultas ocorrem enquanto a área logada está visível (home e tela do exaustor, pois a home mostra o estado — Figma 08b). Com o WebSocket conectado, `exhaust.changed` do exaustor da unidade ativa dispara a reconsulta na hora e o laço fica em 30 s (5 s em transição); sem ele, 5 s (2 s em transição). Respeitar `Retry-After`; nenhuma falha causa repetição automática do acionamento.
- A seleção do equipamento intersecta torre/final da unidade ativa com a lista autorizada de `/exhausts`. Não fabricar equipamentos ou permissões ausentes dessa lista.
- Quando a resposta for inconclusiva, usar os textos aprovados “Não foi possível confirmar a ativação” e “Atualize o estado antes de tentar novamente”, sem afirmar proteção ou encerramento físico não confirmados.

| Method | Path | Body | Returns |
|---|---|---|---|
| GET | `/exhausts` | — | 200 `Exhaust[]` (only the ones this user may see) |
| POST | `/exhausts/{id}/on` | `{ minutes?, location? }` (1–1440; omit = stays on until turned off; resend while on = restart timer; `location` required for residents) | 200 `Exhaust`; 403 location refused; 409 in maintenance |
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

## Home, mensagens e configurações (telas 08a–08d e 09–17)

- O menu inferior tem três abas: **Início · Alarme · Exaustor**. Configurações abre pelo ícone de perfil no cabeçalho da home; o sino abre Mensagens (17).
- Comunicados (08d, 17 e 18) ficam em `feature/message` e usam `/messages` (seção "Condo messages" abaixo): carrossel da home vindo pronto de `GET /messages/carousel` (TTL, fixados e prioridade decididos pela API), contador de não lidas no sino, lista com filtro "Todas/Não lidas" agrupada por dia e detalhe. Abrir o detalhe chama `POST /messages/{id}/read`. Consulta ao entrar na área logada e quando chega o push `message.published` (sem polling); só para equipe ou morador com unidade liberada.
- A unidade padrão foi removida do Figma e do app (30/09/2026). A unidade ativa é escolhida na tela 10 ("Selecionar unidade") com um toque; a seleção vale durante a sessão enquanto vinculada. No acesso seguinte vale a regra inicial (`selectResidenceUnit`). O seletor continua mostrando unidades bloqueadas com os motivos do contrato.
- Configurações (09) exibe "Código Freedom" = `person.sequencia`. É o código do morador no cadastro do condomínio e ajuda no atendimento; exceção consciente à regra de não exibir identificadores internos.
- Privacidade (12): Termos (13) e Política (14) exibem a página vigente publicada pela API (`feature/legal`, seção "Terms of Use and Privacy Policy"). Dados pessoais (15) mostra nome, CPF mascarado (`person.cpfMasked`; a API nunca envia o CPF completo, e a linha some quando vem nulo), e-mail e unidade ativa; "dados complementares" não existem no contrato e não são exibidos.
- Excluir conta (16): a própria tela é a confirmação; "Excluir conta" chama `DELETE /auth/me` e, só depois da resposta de sucesso, limpa a sessão e volta às boas-vindas com "Sua conta foi excluída.". Em falha, a sessão continua e o erro aparece na tela (503: nada foi excluído). Para equipe (`porteiro`, `sindico`, `admin`) o botão fica desabilitado: essas contas são removidas pela administração.
- Idioma é local; inglês e espanhol ficam desabilitados até existir tradução completa. `/me/settings` existe na API e pode sincronizar a preferência no futuro.
- As preferências residenciais não contêm tokens; ficam em `feature/residence`. O armazenamento de sessão continua exclusivo de `core/session`.
- Manter os dados demonstrativos fora das integrações reais. Mockups eventualmente necessários por ausência de contrato devem ser identificados como simulação e documentados na entrega.

## Condo messages (comunicados)

Reading: staff, or a resident with an unblocked unit (403 otherwise). Publishing and archiving: `sindico`/`admin` (not used by the app). A message published with `notify: true` (default) sends the `message.published` push when it becomes visible (see "Push notifications").

| Method | Path | Body / query | Returns |
|---|---|---|---|
| GET | `/messages` | `?filter=all\|unread&limit=20&offset=0` (limit ≤ 50) | `{ items: CondoMessage[], unreadCount }` + `meta.pagination` |
| GET | `/messages/carousel` | — | `{ items: CondoMessage[], unreadCount }` — home carousel of this account (≤ 5, already filtered and ordered) |
| GET | `/messages/{id}` | — | `CondoMessage` |
| POST | `/messages/{id}/read` | — | `CondoMessage` with `read: true` (idempotent) |

```json
{ "id": 12, "category": "manutencao", "label": "Manutenção programada", "title": "…", "body": "…", "publishedAt": "2026-10-13T13:30:00.000Z", "expiresAt": null, "carouselTtlHours": 24, "pinned": false, "read": false }
```

- `category`: `manutencao` | `comunicado` | `aviso` | `urgente` (icon and color in the app). `label` is the tag on the card/detail; `null` → category name.
- Only visible messages come back (published, not archived, not expired), newest first. `unreadCount` ignores filter and page (bell badge).
- Home carousel (decided on 13/10/2026; the app only displays what `/messages/carousel` returns and never applies these rules itself):
  - `carouselTtlHours` (default 24, set by the síndico): hours in the carousel from `publishedAt`. `0` = leaves the carousel of each account once that account reads it — after `POST /messages/{id}/read` the app reloads the carousel.
  - `pinned`: stays until the síndico unpins it; always TTL 0, so after unpinning it leaves for those who read it and, for the others, when they read it.
  - Order: category priority first (`urgente` > `aviso` > `manutencao` > `comunicado`), pinned first within the category, then newest. Up to 5 items.
  - The list (17) is not affected: it keeps every visible message until `expiresAt` or archiving. Pin/TTL are set by staff (`POST /messages`, `PATCH /messages/{id}`) in the new web frontend, not in the app.

## Fire Panel (CIE) — read-only

`GET /cie/status`, `/cie/panel`, `/cie/alarms/active`, `/cie/events/active?type=`, `/cie/installation`, `/cie/counters/blocks`, `/cie/counters/outputs`, and `/cie/logs?type=&kind=&limit=&cursor=` (cursor-based: pass the cursor returned in the previous page's `data`, not `meta.pagination`). Shapes are in the OpenAPI spec. Command endpoints (`/cie/commands/*`) exist for staff but **the app never calls them** — `feature/fire` only reads.

Every log/event item carries `event` (`category`, `label`, `kind`, `severity`) and `display` (`device`, `deviceName`, `place.towerLabel`/`floorLabel`/`area`, `rule`, `loop`, `address`, `zoneName`), already interpreted by the backend — the app does not interpret panel codes. `/cie/alarms/active` also returns `activeAlarms` (active now; `latestAlarmLogs` is history).

### Implementação Android das telas 08c e 26–30c (12/10/2026)

- `FireRepository` usa somente `GET /cie/panel`, `/cie/alarms/active`, `/cie/events/active` e `/cie/logs`. Nunca adicionar comandos sem pedido explícito: a central é equipamento de segurança real.
- Estado: `alarms.isTriggered` → Alarme (28 e home 08c, prevalece sobre tudo); painel sem `online`/`connected` → sem comunicação; `falha`/`supervisao` > 0 → Atenção (27); senão Normal (26). Dispositivos apenas bloqueados não geram atenção para o morador.
- `/cie/events/active` só é consultado quando os contadores indicam atenção, para mostrar o "Local do aviso"; se falhar, o estado de atenção continua exibido sem o local.
- Consulta enquanto a área logada estiver visível (home mostra o estado); para em `onStop`; respeita `Retry-After`. Com o WebSocket conectado, mudanças da central disparam a reconsulta na hora e o laço fica em 60 s; sem ele, 10 s.
- A faixa da tela 26 depende do estado real do push (`PushStatus`), conferido a cada retorno ao app (o morador pode desligar notificações a qualquer momento):

  | Situação | Texto da faixa |
  |---|---|
  | Aparelho registrado na API **e** notificações permitidas (`POST_NOTIFICATIONS` no Android 13+, `areNotificationsEnabled()` e canal `cie_alarm` não bloqueado) | "Você será avisado se o estado da central mudar." (texto do Figma) |
  | Notificações negadas, canal do alarme bloqueado ou aparelho ainda não registrado | "O estado é atualizado enquanto o aplicativo estiver aberto." + ação "Ativar notificações" |

  **Quem recebe o quê (decidido em 13/10/2026).** A prioridade vem da gravidade do evento (`event.severity`), nunca da localização do usuário: localização em segundo plano exige revisão especial da Play e o erro de GPS dentro do prédio poderia rebaixar um alarme real.

  | Perfil | Alarme de incêndio | Falha / supervisão | Normalização |
  |---|---|---|---|
  | Morador (`role` nulo ou `morador`) | Push de **alta prioridade** | Não recebe | Não recebe |
  | Equipe (`porteiro`, `sindico`, `admin`) | Push de **alta prioridade** | Push convencional | Push silencioso (opcional) |

  Quem é equipe e também morador recebe como equipe. A API decide os destinatários pelo papel; o app apenas exibe.

  Alertas de incêndio usam mensagem FCM de prioridade alta e canal próprio com som. A entrega não é garantida em tempo real (economia de bateria, otimizações da Samsung), então nenhum texto promete aviso imediato. Implementação: seção "Push notifications".
- A faixa da tela 27 diz "A ocorrência foi registrada pela central e deve ser verificada pela equipe responsável." em vez de "A equipe responsável está verificando a ocorrência.", porque a API não informa se há verificação em andamento.
- Histórico (29): últimos 30 dias, até 4 páginas de 50 eventos seguindo `nextCursor`, agrupados por dia e filtrados no app (Alarmes; Falhas = falha + supervisão; Operação = operação + bloqueio). Detalhe (30a–30c) usa só campos retornados; "Dados técnicos" mostra o registro como veio da API.
- Rótulos usam `display`/`event` quando presentes. Servidores sem esses campos caem no fallback: nomes do painel em caixa alta e com `\u0000` são normalizados por `cieName()`/`floorLabel()` ("TORRE B" → "Torre B", "8 ANDAR B T-B" → "8º andar").

## Real-time (WebSocket `/v3/ws`)

Foreground only, in `core/realtime/RealtimeClient.kt`; the push covers the app closed. Contract in `APP-GUIDE.md` §6.

- Started while the logged-in area is visible for accounts with services (staff, or resident with an unblocked unit — others get 403); stopped in `onStop`. One socket at a time (the API allows 3 per account).
- Connect: `POST /ws/ticket` (single use, 30 s) → `wss://<host>/v3/ws?ticket=…`. Close 4001 (access token expired) → new ticket and reconnect at once; handshake 403 → stop; 401 → one immediate retry; anything else → backoff 1, 2, 4… 30 s. Own OkHttp client with `readTimeout(0)` and a 20 s ping.
- **Events only trigger a REST reload**; the app never builds state from WebSocket payloads. After every (re)connect the client emits `realtime.connected` and the screens reload.
  - Fire panel: `cie.active.updated`, `cie.connection.changed`, `upstream.connection.changed` (`service: "cie"`) and `cie.status.updated` only when `cieSignature` changes (connection, restart and counters; the payload arrives every ~1 s and its clock fields change every time). At most one reload every 2 s.
  - Exhaust: `exhaust.changed` of the active unit's exhaust. The socket never sends commands.
  - Everything else (gates, tags, devices, command log) is ignored: the app has no such screens.
- While connected, polling becomes a safety net (fire 60 s; exhaust 30 s, 5 s in transition); when the socket drops, the loops return to their normal pace at once.

## Push notifications (FCM)

Native push uses **only** `/notifications/*` (decided on 13/10/2026). `/push/*` is the web panel's Web Push (VAPID) and is never used by the app.

| Method | Path | Body | Returns |
|---|---|---|---|
| PUT | `/notifications/devices/android` | `{ installationId, token, appVersion?, osVersion?, deviceModel?, notificationsEnabled, alarmChannelEnabled? }` | `{ platform, installationId, notificationsEnabled, alarmChannelEnabled, registeredAt }` (30/h per account) |
| DELETE | `/notifications/devices/android/{installationId}` | — | `null` (idempotent) |
| PUT / DELETE | `/notifications/devices/ios…` | same | `null` — reserved for the iOS app, nothing stored |
| GET / PUT | `/notifications/preferences` | `{ exhaustExpiring?, exhaustOff?, messages?, cieStaffAlerts? }` | `{ exhaustExpiring, exhaustExpiringMinutes, exhaustOff, messages, cieStaffAlerts }` |
| POST | `/notifications/test` | — | `{ targets, sent, failed }` (3 per 10 min; 503 when push is disabled on the server) |
| POST | `/notifications/test/fire-alarm` | `{ installationId }` | `{ targets, sent, failed }` — admin only, validation tool (curl), **not called by the app**: the real `cie.alarm` push ("Alarme de incêndio (teste)") to one device, without touching the panel |

- **App side** (`core/push`): `NovaApplication` holds the process graph because `NovaMessagingService` runs with the app closed. `installationId` is a UUID kept in `push_preferences`; the FCM token is never stored in clear (only a hash to detect changes) and never logged.
- **Registration** (`PushRegistration`): after login with `mustChangePassword` and `mustAcceptTerms` resolved, on each return to the app, in `onNewToken` and after the permission flow. The API is only called when account, token, permission, alarm channel or app version changed, or after 24 h.
- **Logout**: `AuthRepository.logout` calls `DELETE …/{installationId}` with the captured access token **before** `/auth/logout`, then the app deletes the FCM token (if the DELETE fails, the server invalidates the old token on the next send). Account deletion: the API removes the devices; the app deletes the FCM token. A push received without a local session is dropped and the FCM token deleted.
- **Payload**: data-only message, all values strings: `type`, `title`, `body`, `channel`, `target` (`fire` | `exhaust` | `message` | `home`), `sentAt`, `dispatchId`, plus `exhaustId` or `messageId`/`category`. The app shows the server texts as they come and never interprets them; the tap opens `target` (message detail by `messageId`) only if the account can still see it.
- **Channels** (ids fixed by the API; `PushChannels`): `cie_alarm` (high, alarm sound with `USAGE_ALARM`; a channel's sound cannot change after creation — use a new id), `cie_status` and `cie_info` (staff, created on their first push), `exhaust` (high), `messages`, `general`. No full-screen intent (Play restricts it to calling and alarm-clock apps).

| `type` | Who receives (the API decides) | Channel |
|---|---|---|
| `cie.alarm` | staff + residents with an unblocked unit; one per alarm episode; high priority | `cie_alarm` |
| `cie.alarm.cleared`, `cie.failure`, `cie.supervision`, `cie.restarting`, `cie.offline` · `cie.blocked`, `cie.normalized` | staff only | `cie_status` · `cie_info` |
| `exhaust.expiring` (X min before the auto-off; X fixed on the server, default 15), `exhaust.off`, `exhaust.off.unconfirmed` | **only the owner of the current cycle**: the account of the last turn-on/restart through the app. Earlier accounts stop receiving once someone else restarts; a timer changed in the v2 web panel notifies nobody; turning it off yourself sends nothing | `exhaust` |
| `message.published` | staff + residents with an unblocked unit, when a message with `notify` becomes visible | `messages` |
| `test` | the account that asked | `general` |

- **Preferences** (Settings → Notificações, `feature/notification`): exhaust warning, exhaust off, messages and, for staff, panel alerts. The fire alarm is not a preference: its control is the Android channel.
- **Permission** (Android 13+): asked from the home card "Ative as notificações", the screen 26 action or the Notifications screen; once the system stops showing the dialog, those actions open the app's notification settings (or the `cie_alarm` channel when only it is blocked).

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
