# Status do projeto — Nova Residence (app Android)

Atualizado em **13/10/2026**.

Legenda: ✅ feito · 🟡 parcial · ⏳ depende de terceiros ou de outra etapa · ❌ não feito · ❓ aguarda decisão

> **Atenção:** o último commit é de **01/10/2026**. O trabalho de 12 e 13/10 (58 arquivos) está **sem commit**, junto com o ajuste de resiliência da sessão que já estava no repositório antes de 12/10.

Referências: [AGENTS.md](AGENTS.md) (regras e contrato da API), [DESIGN_GUIDE.md](DESIGN_GUIDE.md), [CHECKLIST-PUBLICACAO.md](CHECKLIST-PUBLICACAO.md), [docs/revisao-figma-e-integracao.md](docs/revisao-figma-e-integracao.md).

## 1. Autenticação (telas 01–07)

| | Item | Data |
|---|---|---|
| ✅ | Splash nativo e Compose com a marca oficial (#2B0F44) | 30/09 · cor 12/10 |
| ✅ | Boas-vindas com "ou continue com" e botão Google | 12/10 |
| ✅ | Login por e-mail e senha | 30/09 |
| ✅ | Login com Google (Credential Manager) — validado só no build de depuração | 30/09 |
| ✅ | Cadastro com senha única e dica "Use pelo menos 8 caracteres" | 12/10 |
| ✅ | Botão de mostrar/ocultar senha em todos os campos de senha | 12/10 |
| ✅ | Recuperação por código de 6 dígitos e reenvio com contagem | 30/09 · visual 12/10 |
| ✅ | Troca obrigatória de senha temporária | 30/09 |
| ✅ | Tela de erro de login com o "i" contornado | 12/10 · 13/10 |
| ✅ | Recuperação de acesso por e-mail (Figma corrigido para "E-mail" em 13/10, igual ao app e à API) | 30/09 · 13/10 |

## 2. Home e mensagens (08a–08d, 17, 18)

| | Item | Data |
|---|---|---|
| ✅ | Cabeçalho com pílula da unidade, sino, perfil e "Olá, Nome!" | 12/10 · ícones 13/10 |
| ✅ | Cartões de serviço com estado real (normal, exaustor ligado, alarme) | 12/10 |
| ✅ | Avisos de cadastro pendente, sem unidade, bloqueio e erro | 30/09 · visual 12/10 |
| ✅ | Carrossel de comunicados na home (08d), com estado vazio quando não há | 13/10 |
| ✅ | Contador de não lidas no sino | 13/10 |
| ✅ | Mensagens (17): filtro Todas/Não lidas, agrupadas por dia, ícone por categoria | 13/10 |
| ✅ | Detalhe da mensagem (18), marcando como lida | 13/10 |
| ✅ | API: rotas `/messages` em `src/v3` (listar, detalhe, marcar lida, publicar, arquivar) + migração 003, validadas no Postgres existente dentro de transação desfeita | 13/10 |
| ⏳ | **Deploy da API e aplicação da migração `003_condo_messages.sql`** — até lá a home mostra "Não foi possível carregar os informativos." | — |
| ❌ | Tela de publicação no painel web (FRONT); a API já aceita `POST /messages` | — |

## 3. Configurações (09–16)

| | Item | Data |
|---|---|---|
| ✅ | Configurações: conta, Código Freedom, unidades, idioma, privacidade, desconectar e versão | 12/10 |
| ✅ | Seleção de unidade com motivos de bloqueio | 30/09 · visual 12/10 |
| ✅ | Idioma: Português; inglês e espanhol desabilitados até haver tradução (tradução: seção 3.1) | 30/09 |
| ✅ | Menu de privacidade (12) | 12/10 |
| ✅ | Termo de Uso (13) e Política de Privacidade (14) v1.0: página vigente publicada pela API, em WebView restrito | 13/10 |
| ✅ | Aviso "Ao continuar, você concorda com o Termo de Uso e a Política de Privacidade" nas boas-vindas e no cadastro; cadastro envia a versão aceita | 13/10 |
| ✅ | Tela de aceite do Termo para conta antiga ou versão nova (`mustAcceptTerms`) — sem tela no Figma | 13/10 |
| ✅ | Dados pessoais (15): nome, CPF mascarado, e-mail e unidade ativa; "dados complementares" omitidos (não estão na API) | 12/10 · CPF 13/10 |
| ✅ | Excluir conta (16) funcional; equipe vê o botão desabilitado (removida pela administração) | 13/10 |
| ✅ | API: `DELETE /auth/me`, página pública `/excluir-conta` e `person.cpfMasked` em `src/v3`, validados no Postgres em transação desfeita | 13/10 |
| ⏳ | **Deploy da API** com Termo/Política (migração `004_legal_acceptance.sql`) e exclusão de conta — até lá, o cadastro falha (o app exige a versão do Termo) e as telas 13/14 mostram erro de carregamento | — |
| ❓ | Telas novas sem Figma (aviso nas boas-vindas e no cadastro, aceite do Termo): desenhar ou aceitar como estão | — |

### 3.1 Tradução (inglês e espanhol) — a planejar

| | Item |
|---|---|
| ❌ | Planejar: extrair os textos para `strings.xml`, traduzir, decidir o idioma dos textos que vêm da API (`detail`, comunicados, central) e dos documentos jurídicos |
| ❌ | Habilitar inglês e espanhol na tela 11 só com a tradução completa |

## 4. Exaustor (19–24)

| | Item | Data |
|---|---|---|
| ✅ | Estados desligado, ligando, ligado, desligando, offline, manutenção e erro com ilustrações | 30/09 · redesenho 12/10 |
| ✅ | Ligar por 180 min, reiniciar tempo, contador HH:MM:SS vindo da API | 30/09 · 12/10 |
| ✅ | Proteções: sem comando duplicado, `Retry-After` respeitado, 409 em manutenção sem repetir | 30/09 |
| ✅ | Cartão unidade/equipamento (prédio de 28 dp, faixa #EEECF6) | 12/10 · 13/10 |
| ✅ | Validado com hardware real (autorizado) | 01/10 |
| ❌ | Operação dos 24 exaustores pela equipe: não há tela no Figma | — |

## 5. Central de Incêndio (08c, 26–30c)

| | Item | Data |
|---|---|---|
| ✅ | Estados normal, atenção (com local do aviso), sem comunicação e alarme com orientações de segurança | 01/10 · redesenho 12/10 |
| ✅ | Uso dos campos interpretados pela API (`event`, `display`, `activeAlarms`), com alternativa para servidores antigos | 12/10 |
| ✅ | Histórico de 30 dias agrupado por dia, com filtros e ícones | 12/10 · 13/10 |
| ✅ | Detalhe do evento com "Dados técnicos" | 12/10 |
| ✅ | Correção "Torre B · Torre B" encontrada com dados reais | 12/10 |
| ✅ | Validado com a API real (normal, histórico, detalhe) | 12/10 |
| 🟡 | Alarme real ou simulado com a equipe ainda não validado | — |
| ⏳ | Texto da tela 26 ("Você será avisado…") depende do push; regra registrada no AGENTS.md | 13/10 |
| ❓ | Texto da faixa da tela 27: manter, usar o do Figma ou ajustar | — |

## 6. Núcleo, desempenho e qualidade

| | Item | Data |
|---|---|---|
| ✅ | Sessão criptografada (Keystore), renovação de token serializada | 30/09 |
| 🟡 | Sessão resiliente a falta de rede ao acordar o aparelho: presente, sem commit | antes de 12/10 |
| ✅ | Menu inferior novo (ícone preenchido roxo no item selecionado) | 12/10 · 13/10 |
| ✅ | Margem de 28 dp em todas as telas | 12/10 |
| ✅ | Desempenho: imagens e JSON processados fora da thread da interface, histórico montando só os itens visíveis | 12/10 |
| ✅ | Ilustrações em WebP por densidade (~3,6 MB → ~1 MB; ~170 KB no J4) | 12/10 |
| ✅ | 46 testes unitários e 9 testes de tela aprovados no J4 | 13/10 |
| 🟡 | Contraste: amarelo, verde e cinzas mais escuros que o Figma; precisa de aceite no Figma | 12–13/10 |
| ❌ | Otimização R8 e Baseline Profile (velocidade na primeira abertura de cada tela) | — |
| ❌ | Push nativo (FCM) para alertas da central — ver seção 7.1 | — |
| ❌ | WebSocket `/v3/ws` em tempo real (hoje: consulta periódica a cada 5–10 s com o app aberto) | — |

## 7. Dependências na API (`src/v3`)

| | Item |
|---|---|
| ✅ | Autenticação, residência, exaustor (com `command` e `maintenance`) e central interpretada, em produção |
| ✅ | **`DELETE /auth/me`**, obrigatório para a Play — implementado em 13/10, aguardando deploy |
| ✅ | Página web pública de exclusão de conta (`/excluir-conta`) — implementada em 13/10, aguardando deploy |
| ✅ | Termo de Uso e Política de Privacidade v1.0, catálogo `GET /legal` e aceite (migração 004) — commit 0555c2a, aguardando deploy |
| ✅ | Rota de mensagens/comunicados (08d, 17, 18) — implementada em 13/10, aguardando deploy e migração 003 |
| ❌ | Push nativo (FCM) para alarme com o app fechado — ver seção 7.1 |
| ✅ | CPF mascarado em `/residence/me` para a tela 15 (13/10); outros dados complementares não previstos |

### 7.1 Push de alertas da central — requisitos combinados (13/10)

Detalhe completo no [AGENTS.md](AGENTS.md), seção da Central de Incêndio.

| | Requisito |
|---|---|
| ❌ | **API:** FCM no backend (hoje só existe Web Push/VAPID para o painel web), registro do token do aparelho e envio quando o estado da central muda |
| ❌ | **Prioridade alta:** alertas de incêndio enviados como mensagem FCM de prioridade alta |
| ✅ | **Regra decidida (13/10):** prioridade pela gravidade do evento, não pela localização. **Morador recebe apenas alarme de incêndio** (alta prioridade). **Equipe** (porteiro, síndico, admin) recebe alarme (alta prioridade), falha/supervisão (push convencional) e normalização (silencioso, opcional) |
| ❌ | **Canal próprio com som:** canal de notificação exclusivo para alertas da central, separado de outros avisos |
| ❌ | **Permissão:** pedir `POST_NOTIFICATIONS` no Android 13+ e conferir `areNotificationsEnabled()` e o canal a cada retorno à tela |
| ❌ | **Tela 26 condicional:** "Você será avisado se o estado da central mudar." só com token registrado e notificações permitidas; senão, o texto atual + ação "Ativar notificações" |
| ⚠️ | **Limite:** a entrega não é garantida em tempo real (economia de bateria, otimizações Samsung); nenhum texto promete aviso imediato |
| ❓ | **Prioridade no plano:** o checklist trata o push como opcional na 1ª versão; por ser alerta de incêndio, decidir se entra antes da publicação |

## 7.2 Localização para ligar o exaustor — decidido em 13/10, não implementado

Detalhe completo no [AGENTS.md](AGENTS.md), seção do exaustor.

| | Item |
|---|---|
| ✅ | **Decisão:** morador só liga o exaustor dentro da área do condomínio; validação feita **na API**, com a localização enviada no comando |
| ✅ | **Decisão:** desligar nunca exige localização; reiniciar tempo exige |
| ✅ | **Decisão:** localização só "durante o uso", sem segundo plano; não é armazenada |
| ❓ | Coordenadas ou polígono do terreno do condomínio |
| ❓ | Equipe (porteiro, síndico, admin) isenta da verificação? |
| ❌ | **API:** área do condomínio no servidor, validação no `POST /exhausts/{id}/on` e erro específico para "fora da área" |
| ❌ | **App:** permissão, leitura da localização (`play-services-location`), detecção de localização simulada e envio no comando |
| ❌ | **Figma:** estados de permissão negada, localização desligada, fora da área e precisão insuficiente |
| ❌ | **Publicação:** declarar localização na política de privacidade e na "Segurança dos dados" |

## 8. Build de release e Play Console

| | Item |
|---|---|
| ✅ | Somente HTTPS |
| ✅ | `.aab` gerado (`bundleRelease`) com divisão por densidade (12/10) — ainda sem assinatura |
| 🟡 | `versionCode`/`versionName` (hoje 1 / "1.0") e `targetSdk` a conferir |
| ❌ | Chave de upload e assinatura no Gradle |
| ❌ | Clientes OAuth de release (SHA-1 da chave de upload e da Play App Signing); sem eles o login Google falha no app da loja |
| ❌ | Ícone do app com a marca (hoje é o padrão do template) |
| ❌ | R8 ativado e release testado no J4 |
| ❌ | D-U-N-S, conta de organização, política de privacidade em URL pública, "Segurança dos dados", classificação, ficha da loja, conta de teste para a revisão, teste interno e produção |

## 9. Pendências imediatas (sem depender de terceiros)

1. ❌ Fazer o commit do trabalho de 12–13/10 e atualizar o `CHANGELOG.md` (ainda descreve a numeração antiga das telas).
2. ❌ Trocar a senha pessoal usada nos testes (também consta do checklist de publicação).
3. ❓ Decidir o texto da faixa da tela 27.
4. ❓ Decidir se o push de alertas da central (seção 7.1) é obrigatório na 1ª versão.
5. ❓ Informar as coordenadas do condomínio e se a equipe fica isenta da verificação de localização (seção 7.2).
6. ❓ Aceitar no Figma as cores ajustadas para contraste, ou voltar às cores exatas do Figma.
7. 🟡 Validar um alarme da central com a equipe.

**Próximo bloco sugerido:** deploy da API (migrações 003 e 004, Termo, exclusão de conta) e teste da exclusão com uma conta de teste → ícone, R8, chave e `.aab` assinado → OAuth de release → teste interno na Play. O push de alertas da central (seção 7.1) entra antes da publicação se for decidido que é obrigatório na 1ª versão.
