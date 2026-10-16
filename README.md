<div align="center">

# 🏢 Nova Android - App do Condomínio Nova Residence

**App Android de moradores e equipe: central de incêndio, exaustores, comunicados e notificações**

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/ftapparo/nova-android)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-purple)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-green)](https://developer.android.com/jetpack/compose)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green)](https://developer.android.com/)
[![License](https://img.shields.io/badge/license-propriet%C3%A1ria-red.svg)](LICENSE)

</div>

---

## 📋 Índice

- [Sobre o Projeto](#-sobre-o-projeto)
- [Funcionalidades](#-funcionalidades)
- [Arquitetura](#-arquitetura)
- [Tecnologias](#-tecnologias)
- [Instalação](#-instalação)
- [Configuração](#-configuração)
- [Execução e Testes](#-execução-e-testes)
- [Backend](#-backend)
- [Idiomas](#-idiomas)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Segurança](#-segurança)
- [Documentação](#-documentação)
- [Changelog](#-changelog)
- [Licença](#-licença)
- [Autor](#-autor)

---

## 🎯 Sobre o Projeto

**Nova Android** é o app oficial do **Condomínio Nova Residence** para moradores e equipe (portaria, síndico e administração). Ele consome exclusivamente a **nova-api v3** e reúne em um só lugar o estado da central de incêndio, o acionamento dos exaustores das churrasqueiras, os comunicados da administração e os avisos push.

### ✨ Diferenciais

- **Acesso pelo vínculo com o cadastro do condomínio**: o app só libera serviços para morador com unidade ativa ou para a equipe
- **Central de incêndio em tempo real**: WebSocket com o app aberto e push de alta prioridade para alarme com o app fechado
- **Comando de hardware real com proteção**: localização validada na API para ligar o exaustor, sem comando duplicado e com respeito ao `Retry-After`
- **Três idiomas**: português, inglês e espanhol, com a API respondendo no mesmo idioma
- **Sessão segura**: tokens criptografados com chave no Android Keystore

---

## 🚀 Funcionalidades

### Conta e Acesso
- ✅ Login por e-mail e senha e com Google (Credential Manager)
- ✅ Cadastro, recuperação por código de 6 dígitos e troca obrigatória de senha temporária
- ✅ Aceite do Termo de Uso e da Política de Privacidade (versão vigente consultada na API)
- ✅ Exclusão da própria conta

### Serviços do Condomínio
- ✅ Central de incêndio: normal, atenção, sem comunicação e alarme com orientações de segurança; histórico de 30 dias para a equipe
- ✅ Exaustor da unidade ativa: ligar por 180 min, reiniciar tempo, contador vindo da API, manutenção e offline
- ✅ Verificação de localização para ligar o exaustor (morador), sem localização em segundo plano

### Comunicados e Notificações
- ✅ Carrossel na home, lista com filtro Todas/Não lidas e detalhe que marca como lido
- ✅ Push nativo (FCM): alarme de incêndio com canal e som próprios, avisos do exaustor, comunicados e alertas da central para a equipe
- ✅ Preferências de notificação e envio de teste

### Configurações
- ✅ Seleção de unidade com motivos de bloqueio, dados pessoais, privacidade e idioma

---

## 🏗️ Arquitetura

Organização por funcionalidade (feature-first): cada feature mantém tela, estado, acesso a dados e modelos juntos; só a infraestrutura compartilhada fica em `core/`.

```
┌─────────────────────────────────────────────┐
│       feature/ — Telas e regras de uso       │
│  auth, residence, exhaust, fire, message,   │
│  notification, legal                        │
│  Screen (Compose) → ViewModel → Repository   │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│       core/ — Infraestrutura compartilhada   │
│  network (envelope e erros), session        │
│  (tokens), push (FCM), realtime (WebSocket), │
│  ui (design system, idioma, UiText)          │
└─────────────────┬───────────────────────────┘
                  │ HTTPS
┌─────────────────▼───────────────────────────┐
│       nova-api v3 (/v3/api, /v3/ws)          │
│  Auth, ERP, central de incêndio, exaustores, │
│  comunicados e envio de push                 │
└─────────────────────────────────────────────┘
```

### Regras de Fronteira

- A tela não conhece HTTP: fala com o ViewModel, que fala com o repositório
- Só `core/network` conhece o envelope de resposta e os códigos HTTP
- Só `core/session` lê ou grava tokens
- `NovaApplication` mantém o grafo do processo (sessão, cliente da API, push), porque o serviço do FCM roda sem Activity

---

## 🛠️ Tecnologias

### Linguagem e Interface
- **Kotlin 2.2** - Linguagem
- **Jetpack Compose + Material 3** - Interface declarativa
- **AndroidX Lifecycle / ViewModel** - Estado das telas

### Rede e Dados
- **OkHttp** - HTTP e WebSocket
- **kotlinx.serialization** - JSON
- **kotlinx.coroutines** - Concorrência

### Serviços Google
- **Credential Manager + Google Identity** - Login com Google
- **Firebase Cloud Messaging** - Push nativo
- **Play Services Location** - Localização para o exaustor

### Testes
- **JUnit 4 + MockWebServer** - Testes unitários com servidor HTTP falso
- **Compose UI Test** - Testes de tela no aparelho

### Build
- **Android Gradle Plugin 9.4** - `compileSdk`/`targetSdk` 37, `minSdk` 26 (Android 8.0)

---

## 📦 Instalação

### Pré-requisitos

- Android Studio com JDK 17+ (o `gradlew` precisa de `JAVA_HOME`, ex.: o JBR do Android Studio)
- Android SDK 37
- Aparelho ou emulador com Android 8.0+ e Google Play Services

### Clonar o Repositório

```bash
git clone https://github.com/ftapparo/nova-android.git
cd nova-android
```

### Compilar

```bash
./gradlew assembleDebug
```

---

## ⚙️ Configuração

- **URL da API**: `V3_BASE_URL` em `app/build.gradle.kts` (`buildConfigField`). Nunca fixar a URL no código das features
- **Login com Google**: `GOOGLE_WEB_CLIENT_ID` em `app/build.gradle.kts` (client **web**, não é segredo). Cada chave que assina o app (debug de cada máquina, upload e Play App Signing) precisa de um cliente OAuth **Android** com o SHA-1 dela no Google Cloud; o secret do client web nunca entra no app
- **Firebase (push)**: `app/google-services.json` do projeto `nova-residence-login` (não contém segredo)
- **Assinatura de release**: chave e `keystore.properties` ficam fora do repositório (`.gitignore`)

---

## 🚀 Execução e Testes

### Validação padrão antes de concluir uma mudança

```bash
./gradlew lint
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

### Testes de tela no aparelho

```bash
./gradlew connectedDebugAndroidTest
```

> ⚠️ Os testes de tela podem substituir a instalação de depuração e encerrar a sessão do aparelho.

### Aparelho físico (Galaxy J4)

Fluxo padronizado em [`skills/physical-j4/SKILL.md`](skills/physical-j4/SKILL.md) (instalar, logs, capturas).

> ⚠️ **Nunca aponte testes automáticos para a API de produção.** Testes com HTTP usam `MockWebServer`. Comandos ao exaustor acionam hardware real.

---

## 🔗 Backend

O app fala **somente** com a nova-api v3 ([ftapparo/nova-api](https://github.com/ftapparo/nova-api)):

- REST em `/v3/api/*`, com envelope `{ success, data, meta }` e erros RFC 7807 (o app decide pelo `error.type`, nunca pelo texto)
- WebSocket `/v3/ws` com ticket de uso único, só com o app aberto; eventos apenas disparam nova consulta ao REST
- Push nativo por `/notifications/*`

O contrato completo está em [AGENTS.md](AGENTS.md) (seção "Backend Contract"); a especificação OpenAPI é a fonte da verdade.

---

## 🌐 Idiomas

- Português (padrão), inglês e espanhol, com todos os textos em `res/values*/strings.xml`
- Na primeira abertura, o app adota o idioma do Android (português, inglês ou espanhol; qualquer outro vira inglês); depois vale só a escolha em Configurações → Idioma
- O app envia `Accept-Language` e a API responde mensagens de erro, rótulos da central e push no mesmo idioma
- Glossário e regras para novos textos em [docs/traducao.md](docs/traducao.md)

---

## 📂 Estrutura do Projeto

```
app/src/main/java/br/com/novaresidence/
├── feature/
│   ├── auth/           # login, cadastro, Google, senha
│   ├── residence/      # home, unidades, configurações, idioma
│   ├── exhaust/        # exaustores e localização
│   ├── fire/           # central de incêndio (somente leitura)
│   ├── message/        # comunicados
│   ├── notification/   # preferências de notificação
│   └── legal/          # Termo de Uso e Política de Privacidade
├── core/
│   ├── network/        # cliente HTTP, envelope e erros
│   ├── push/           # FCM, canais, registro do aparelho, permissão
│   ├── realtime/       # WebSocket /v3/ws
│   ├── session/        # tokens criptografados e renovação
│   └── ui/             # design system, datas, idioma, UiText
├── NovaApplication.kt  # grafo do processo
├── MainActivity.kt
└── App.kt              # navegação entre entrada e área logada
```

---

## 🔐 Segurança

- ✅ Somente HTTPS em release
- ✅ Tokens criptografados com chave no Android Keystore; nunca em `SharedPreferences` simples nem em log
- ✅ Refresh token de uso único, com renovação serializada
- ✅ Token FCM nunca guardado em claro nem registrado em log
- ✅ Localização usada só no comando do exaustor, sem segundo plano e sem armazenamento
- ✅ Credenciais, chaves de assinatura e secrets OAuth fora do repositório

---

## 📚 Documentação

- [AGENTS.md](AGENTS.md) - Regras de engenharia e contrato da API
- [DESIGN_GUIDE.md](DESIGN_GUIDE.md) - Tipografia, espaçamento, componentes e Figma
- [STATUS-PROJETO.md](STATUS-PROJETO.md) - Situação por tela e pendências em ordem
- [CHECKLIST-PUBLICACAO.md](CHECKLIST-PUBLICACAO.md) - Publicação na Play
- [docs/](docs/) - Revisão Figma, segurança dos dados e tradução

---

## 📄 Changelog

Para ver o histórico completo de alterações, consulte o [CHANGELOG.md](CHANGELOG.md).

---

## 📝 Licença

Software proprietário: **todos os direitos reservados**. Uso exclusivo do Condomínio Nova Residence; cópia, modificação, distribuição ou publicação dependem de autorização por escrito do autor. Veja o arquivo [LICENSE](LICENSE).

---

## 👤 Autor

**Flavio Eduardo Tapparo**

- GitHub: [@ftapparo](https://github.com/ftapparo)
- Projeto: [nova-android](https://github.com/ftapparo/nova-android)

---

## 🏢 Contexto

App desenvolvido para o **Condomínio Nova Residence**, dando aos moradores e à equipe acesso direto aos serviços e à segurança do condomínio.

---

<div align="center">

**Desenvolvido com ❤️ para automação inteligente**

[⬆ Voltar ao topo](#-nova-android---app-do-condomínio-nova-residence)

</div>
