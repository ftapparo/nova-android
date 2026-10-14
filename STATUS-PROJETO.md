# Status do projeto — Nova Residence (app Android)

Atualizado em **13/10/2026**.

Legenda: ✅ feito · 🟡 parcial · ⏳ depende de terceiros ou de outra etapa · ❌ não feito · ❓ aguarda decisão

> **Situação em 13/10/2026:** todo o trabalho do app está commitado (`3b61c5c` sessão, `6a24a74` revisão das telas e comunicados, `96ffafb` Termo, Política e exclusão de conta). A API com comunicados, Termo/Política, exclusão de conta e o ajuste visual das páginas legais (`f858bb1`) está em produção (migrações 003 e 004 aplicadas). Falta o deploy do `Cache-Control: no-cache` das páginas legais. O repositório do app não tem remoto configurado.

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
| ✅ | Deploy da API e migração `003_condo_messages.sql` aplicada; a home carrega os informativos no J4 ("Nenhuma mensagem nova") | 13/10 |
| ✅ | Seção "Informativos do condomínio" só para quem lê comunicados (equipe ou morador com unidade liberada); some na conta pendente, sem unidade ou bloqueada | 13/10 |
| ❌ | Validar com um comunicado real publicado (carrossel, sino, detalhe e marcação de lido) | — |
| ❌ | Tela de publicação no painel web (FRONT); a API já aceita `POST /messages` | — |

## 3. Configurações (09–16)

| | Item | Data |
|---|---|---|
| ✅ | Configurações: conta, Código Freedom, unidades, idioma, privacidade, desconectar e versão | 12/10 |
| ✅ | Seleção de unidade com motivos de bloqueio | 30/09 · visual 12/10 |
| ✅ | Idioma: Português; inglês e espanhol desabilitados até haver tradução (tradução: seção 3.1) | 30/09 |
| ✅ | Menu de privacidade (12) | 12/10 |
| ✅ | Termo de Uso (13) e Política de Privacidade (14) v1.0: página vigente publicada pela API, em WebView restrito, com o texto na escala do corpo do app (14 sp) | 13/10 |
| ✅ | Validado em produção: `GET /legal`, páginas `/termos-de-uso`, `/politica-de-privacidade` e `/excluir-conta` e PDF com o hash do catálogo; Termo aberto no J4 | 13/10 |
| ✅ | Aviso "Ao continuar, você concorda com o Termo de Uso e a Política de Privacidade" nas boas-vindas e no cadastro; cadastro envia a versão aceita | 13/10 |
| ✅ | Tela de aceite do Termo para conta antiga ou versão nova (`mustAcceptTerms`) — Figma atualizado, tela 04b (`309:1089`) | 13/10 |
| ✅ | Aceite gravado em produção: login com Google após o deploy registrou o Termo 1.0 em `legal_acceptance` (canal `google`) | 13/10 |
| ✅ | Tela de aceite validada em produção com conta de teste: aparece sem aceite registrado e "Aceitar e continuar" grava o aceite pelo canal `app` | 13/10 |
| ✅ | Dados pessoais (15): nome, CPF mascarado, e-mail e unidade ativa; "dados complementares" omitidos (não estão na API) | 12/10 · CPF 13/10 |
| ✅ | Excluir conta (16) funcional; equipe vê o botão desabilitado (removida pela administração) | 13/10 |
| ✅ | API: `DELETE /auth/me`, página pública `/excluir-conta` e `person.cpfMasked` em `src/v3`, validados no Postgres em transação desfeita | 13/10 |
| ✅ | Deploy da API com Termo/Política (migração `004_legal_acceptance.sql` aplicada) e exclusão de conta | 13/10 |
| ✅ | Validado em produção com conta de teste: cadastro (aceite pelo canal `signup`), estado pendente sem unidade (home, configurações e dados pessoais), exclusão com "Sua conta foi excluída." e login recusado depois; vínculo apagado e aceite mantido, como prevê a Política | 13/10 |
| ✅ | Páginas legais sem linha dupla no início e sem quebras no meio da frase (`f858bb1`), em produção e conferidas no J4 | 13/10 |
| ✅ | Leitor do Termo e da Política sem cache no app: o endereço não muda entre versões, e a tela de aceite poderia mostrar o texto anterior por até 1 h | 13/10 |
| ⏳ | API: `Cache-Control: no-cache` nas páginas e no PDF sem versão (o PDF com versão fica imutável) — falta deploy | 13/10 |
| ✅ | Tela de aceite com título neutro "Aceite do Termo de Uso" (antes "Termo de Uso atualizado", errado para quem nunca aceitou) | 13/10 |
| ✅ | Figma: avisos do Termo nas boas-vindas e cadastro, tela de aceite, documentos v1.0, dados pessoais e exclusão (incluindo equipe) sincronizados com o app | 13/10 |

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
| ✅ | Figma 26 sincronizado com o texto atual: "O estado é atualizado enquanto o aplicativo estiver aberto."; texto de notificação continua dependendo do push | 13/10 |
| ✅ | Figma 27 sincronizado: "A ocorrência foi registrada pela central e deve ser verificada pela equipe responsável." | 13/10 |

## 6. Núcleo, desempenho e qualidade

| | Item | Data |
|---|---|---|
| ✅ | Sessão criptografada (Keystore), renovação de token serializada | 30/09 |
| ✅ | Sessão resiliente a falta de rede ao acordar o aparelho (commit `3b61c5c`) | antes de 12/10 · commit 13/10 |
| ✅ | Menu inferior novo (ícone preenchido roxo no item selecionado) | 12/10 · 13/10 |
| ✅ | Margem de 28 dp em todas as telas | 12/10 |
| ✅ | Desempenho: imagens e JSON processados fora da thread da interface, histórico montando só os itens visíveis | 12/10 |
| ✅ | Ilustrações em WebP por densidade (~3,6 MB → ~1 MB; ~170 KB no J4) | 12/10 |
| ✅ | 46 testes unitários e 9 testes de tela aprovados no J4 | 13/10 |
| ✅ | Contraste: amarelo, verde e cinzas escurecidos no Figma conforme os tokens do app e o DESIGN_GUIDE | 13/10 |
| ❌ | Otimização R8 e Baseline Profile (velocidade na primeira abertura de cada tela) | — |
| ❌ | Push nativo (FCM) para alertas da central — ver seção 7.1 | — |
| ❌ | WebSocket `/v3/ws` em tempo real (hoje: consulta periódica a cada 5–10 s com o app aberto) | — |

## 7. Dependências na API (`src/v3`)

| | Item |
|---|---|
| ✅ | Autenticação, residência, exaustor (com `command` e `maintenance`) e central interpretada, em produção |
| ✅ | **`DELETE /auth/me`**, obrigatório para a Play — em produção desde 13/10 |
| ✅ | Página web pública de exclusão de conta (`/excluir-conta`) — em produção desde 13/10 |
| ✅ | Termo de Uso e Política de Privacidade v1.0, catálogo `GET /legal` e aceite (migração 004) — em produção desde 13/10 |
| ✅ | Rota de mensagens/comunicados (08d, 17, 18) — em produção desde 13/10 (migração 003) |
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
| ✅ | URLs públicas para a Play: `https://novaresidence.com.br/politica-de-privacidade` e `https://novaresidence.com.br/excluir-conta` |
| ❌ | D-U-N-S, conta de organização, "Segurança dos dados", classificação, ficha da loja, conta de teste para a revisão, teste interno e produção |

## 9. Pendências imediatas (sem depender de terceiros)

1. 🟡 Commit do trabalho de 12–13/10 feito, com entradas novas no `CHANGELOG.md`; as entradas antigas ainda citam a numeração anterior das telas.
2. ⏳ Deploy do `Cache-Control: no-cache` das páginas legais na API (`f858bb1` já está em produção).
3. ✅ Cadastro, tela de aceite, estado sem unidade e exclusão validados em produção com conta de teste (13/10). A conta para a revisão da Play ainda precisa ser criada e vinculada a uma unidade.
4. ✅ Avisos do Termo e tela de aceite desenhados no Figma pelo MCP (13/10).
5. ❌ Trocar a senha pessoal usada nos testes (também consta do checklist de publicação).
6. ✅ Faixa da tela 27 sincronizada no Figma com o texto implementado no app (13/10).
7. ❓ Decidir se o push de alertas da central (seção 7.1) é obrigatório na 1ª versão.
8. ❓ Informar as coordenadas do condomínio e se a equipe fica isenta da verificação de localização (seção 7.2).
9. ✅ Cores ajustadas para contraste refletidas no Figma (13/10).
10. 🟡 Validar um alarme da central com a equipe.

**Próximo bloco sugerido:** ícone, R8, chave e `.aab` assinado → OAuth de release → teste interno na Play. O push de alertas da central (seção 7.1) entra antes da publicação se for decidido que é obrigatório na 1ª versão.
