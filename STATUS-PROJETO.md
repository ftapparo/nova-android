# Status do projeto — Nova Residence (app Android)

Atualizado em **14/10/2026**.

Legenda: ✅ feito · 🟡 parcial · ⏳ depende de terceiros ou de outra etapa · ❌ não feito · ❓ aguarda decisão

> **Situação em 13/10/2026:** API em produção com comunicados e carrossel (migrações 003 a 005), Termo e Política 1.1, exclusão de conta e páginas legais sem cache. No app, o carrossel e a versão do Termo sempre atual estão commitados; o push nativo está em desenvolvimento. O repositório do app não tem remoto configurado.

Referências: [AGENTS.md](AGENTS.md) (regras e contrato da API), [DESIGN_GUIDE.md](DESIGN_GUIDE.md), [CHECKLIST-PUBLICACAO.md](CHECKLIST-PUBLICACAO.md), [docs/revisao-figma-e-integracao.md](docs/revisao-figma-e-integracao.md).

## 1. Autenticação (telas 01–07)

| | Item | Data |
|---|---|---|
| ✅ | Splash nativo e Compose com a marca oficial (#2B0F44) | 30/09 · cor 12/10 |
| ✅ | Boas-vindas com "ou continue com" e botão Google | 12/10 |
| ✅ | Revisão bidirecional Figma/app: menu inferior com intervalo de 4 dp; carrossel com cartões de 356 × 208 dp e respiro vertical de 12 dp; 17 estados novos desenhados em frames editáveis (detalhes no documento de revisão) | 14/10 |
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
| ✅ | Validado em produção com 3 comunicados de teste (ids 2–4, inseridos no banco, vencem em 16/10): carrossel, contador do sino (3 → 2), lista agrupada em Hoje/Ontem com ícone por categoria, filtro Não lidas, detalhe e leitura gravada em `condo_message_read` | 13/10 |
| ✅ | Carrossel com permanência e prioridade (decidido em 13/10): TTL padrão de 24 h ajustável pelo síndico; TTL 0 sai ao ser lido; fixado fica até desafixar (sempre TTL 0); ordem por prioridade da categoria (urgente > aviso > manutenção > comunicado), fixados primeiro dentro dela. API: `GET /messages/carousel`, campos no `POST /messages`, `PATCH /messages/:id` e migração 005, validados no Postgres em transação desfeita. App: home usa a rota nova e reconsulta o carrossel depois de marcar como lido | 13/10 |
| ✅ | Carrossel validado em produção no J4 (migração 005 aplicada): fixado aparece pela prioridade da categoria mesmo depois de 24 h; TTL 0 lido sai; desafixado e lido sai; TTL 0 não lido aparece com o sino em 1 e sai do carrossel assim que é aberto, sem reabrir o app | 13/10 |
| ⏳ | Publicação de comunicados: fica no novo frontend web, desenvolvido à parte (fora do escopo do app); a API já aceita `POST /messages` | — |

## 3. Configurações (09–16)

| | Item | Data |
|---|---|---|
| ✅ | Configurações: conta, Código Freedom, unidades, idioma, privacidade, desconectar e versão | 12/10 |
| ✅ | Seleção de unidade com motivos de bloqueio | 30/09 · visual 12/10 |
| ✅ | Idioma: Português; inglês e espanhol desabilitados até haver tradução (tradução: seção 3.1) | 30/09 |
| ✅ | Menu de privacidade (12) | 12/10 |
| ✅ | Termo de Uso (13) e Política de Privacidade (14) v1.0: página vigente publicada pela API, em WebView restrito, com o texto na escala do corpo do app (14 sp) | 13/10 |
| ✅ | App pronto para versões novas do Termo: a versão nunca é fixa no código e é consultada em `GET /legal` na hora do cadastro, do Google e da tela de aceite | 13/10 |
| ✅ | Termo e Política **1.1** em produção: conta que aceitou a 1.0 viu a tela de aceite da 1.1 no J4 e o aceite foi gravado pelo canal `app` | 13/10 |
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
| ✅ | API em produção com `Cache-Control: no-cache` nas páginas e no PDF sem versão; PDFs com versão (`/1.0/pdf`, `/1.1/pdf`) imutáveis | 13/10 |
| ✅ | Tela de aceite com título neutro "Aceite do Termo de Uso" (antes "Termo de Uso atualizado", errado para quem nunca aceitou) | 13/10 |
| ✅ | Figma: avisos do Termo nas boas-vindas e cadastro, tela de aceite, documentos v1.1, dados pessoais e exclusão (incluindo equipe) sincronizados com o app | 13–14/10 |

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
| ✅ | Figma 26 com push pronto e 26b com notificações desativadas/ação "Ativar notificações", refletindo a condição real do app | 14/10 |
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
| 🟡 | Push nativo (FCM): central, exaustor e comunicados — implementado e validado no J4 (teste, comunicado e exaustor desligado); falta o alarme real (seção 7.1) | 13/10 · 14/10 |
| 🟡 | WebSocket `/v3/ws` em tempo real com o app aberto: eventos da central e do exaustor disparam a reconsulta na hora; consulta periódica vira rede de segurança (60 s / 30 s). Reconexão com ticket novo, 4001 e backoff; 8 testes unitários. Validado no J4 (14/10): com a tela do exaustor aberta, o desligamento pelo painel v2 apareceu como "Desligando" e depois "Desligado" sem esperar a consulta de 30 s; ligar pelo app passou de "Ligando" a "Ligado" em menos de 1 s. Falta medir o volume do `cie.status.updated` | 14/10 |

## 7. Dependências na API (`src/v3`)

| | Item |
|---|---|
| ✅ | Autenticação, residência, exaustor (com `command` e `maintenance`) e central interpretada, em produção |
| ✅ | **`DELETE /auth/me`**, obrigatório para a Play — em produção desde 13/10 |
| ✅ | Página web pública de exclusão de conta (`/excluir-conta`) — em produção desde 13/10 |
| ✅ | Termo de Uso e Política de Privacidade v1.0, catálogo `GET /legal` e aceite (migração 004) — em produção desde 13/10 |
| ✅ | Rota de mensagens/comunicados (08d, 17, 18) — em produção desde 13/10 (migração 003) |
| ✅ | Push nativo (FCM): rotas `/notifications/*` (Android; iOS reservado), migração 006 e avisos da central, do exaustor e de comunicados — deploy feito em 13/10 (seção 7.1) |
| ✅ | CPF mascarado em `/residence/me` para a tela 15 (13/10); outros dados complementares não previstos |

## 7.1 Push nativo (FCM) — central, exaustor e comunicados (13/10)

Detalhe completo no [AGENTS.md](AGENTS.md), seção "Push notifications". Mecanismo novo, separado do Web Push do painel web.

| | Item |
|---|---|
| ✅ | **API** (`7b67532`, deploy em 13/10): registro do aparelho `/notifications/devices/android` (rotas `/ios` reservadas, respondem vazio), preferências, teste, migração 006 e envio pelo FCM só de dados |
| ✅ | **Central:** alarme para equipe e morador com unidade liberada (alta prioridade, um por episódio); falha, supervisão, reinício, bloqueio, sem comunicação, alarme encerrado e normalização só para a equipe |
| ✅ | **Exaustor:** "faltam 15 minutos" (fixo no servidor) e "desligado" só para quem ligou ou reiniciou por último pelo app; tempo mudado pelo painel v2 não avisa ninguém |
| ✅ | **Comunicados:** push quando um comunicado com `notify` fica visível, inclusive o agendado |
| ✅ | **App:** `NovaApplication` com o grafo do processo, serviço do FCM, canais (alarme com som de alarme), registro só quando algo muda, remoção no logout, toque abrindo a tela certa, aviso sem sessão descartado |
| ✅ | **Permissão e telas:** cartão "Ative as notificações" na home, faixa da tela 26 condicional com "Ativar notificações" e tela Configurações → Notificações (preferências e teste) |
| ✅ | 10 testes unitários novos (registro, payload, logout, preferências); lint, testes e build aprovados |
| ✅ | **Firebase** no projeto `nova-residence-login`: app Android registrado, `google-services.json` em `app/`, conta de serviço `nova-api-fcm` (só envio) no Portainer; migração 006 aplicada (14/10) |
| ✅ | **J4 (14/10):** aparelho registrado em `notification_device`, preferências gravadas na API, notificação de teste recebida, toque na notificação abrindo a aba Alarme, tela 26 com "Você será avisado se o estado da central mudar." |
| ✅ | **J4 (14/10):** comunicado de teste (id 7, inserido no banco e arquivado depois) chegou 8 s após a publicação com o app em segundo plano e o toque abriu o detalhe, marcando como lido; "Exaustor desligado" chegou a quem ligou pelo app quando o exaustor foi desligado pelo painel v2 |
| ✅ | Corrigido na API: alerta falso "Central de incêndio sem comunicação" 5 min após cada deploy (`c625664`) e texto do exaustor "Torre A, final 4" no lugar de "A4", que se confundia com o apartamento A-4 (`138f310`) |
| ✅ | Corrigido na API (`cf58d48`): ligar e desligar o exaustor em poucos segundos não gerava o "Exaustor desligado" (o laço de 20 s não via o exaustor ligado); agora o aviso sai ~1 s depois do desligamento — **falta redeploy** |
| ✅ | **Notificação por tela aberta (14/10):** o aviso não aparece quando a tela dele está aberta (exaustor, central, mensagens, início) e a tela se atualiza na hora; em outra tela ou com o app fechado, aparece. O alarme de incêndio aparece sempre. Validado no J4 com comunicados 9 (Mensagens aberta: entrou na lista sem notificação) e 10 (Início aberta: notificação) e com o exaustor (tela do exaustor aberta: sem notificação; outra tela: "O exaustor da Torre A, final 4, foi desligado.") |
| ✅ | **J4 (14/10):** "faltam 15 minutos" chegou a quem ligou; com o app fechado, o "desligado" pelo painel v2 chegou e o toque abriu o exaustor já atualizado |
| ❌ | **Validar no J4:** alarme com o app fechado e tela bloqueada (com a equipe) e cartão de permissão num Android 13+ |
| ✅ | **Figma (14/10):** cartão da home (08e), tela Notificações para morador/equipe e estados de permissão, canal bloqueado e registro pendente (09a–09e); central com ação "Ativar notificações" (26b) |
| ❌ | **Publicação:** declarar o token do aparelho na Política de Privacidade e na "Segurança dos dados" (IDs do dispositivo) |
| ⚠️ | **Limite:** a entrega não é garantida em tempo real (economia de bateria, otimizações Samsung); nenhum texto promete aviso imediato |

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
2. ✅ Deploy da API (no-cache, carrossel com migração 005, Termo 1.1) feito e validado no J4 (13/10). App commitado.
3. ✅ Cadastro, tela de aceite, estado sem unidade e exclusão validados em produção com conta de teste (13/10). A conta para a revisão da Play ainda precisa ser criada e vinculada a uma unidade.
4. ✅ Avisos do Termo e tela de aceite desenhados no Figma pelo MCP (13/10).
5. ❌ Trocar a senha pessoal usada nos testes (também consta do checklist de publicação).
6. ✅ Faixa da tela 27 sincronizada no Figma com o texto implementado no app (13/10).
7. 🟡 Push nativo funcionando no J4 (seção 7.1): redeploy da correção `cf58d48` e validar o alarme com a equipe; telas do Figma atualizadas em 14/10.
8. ❓ Informar as coordenadas do condomínio e se a equipe fica isenta da verificação de localização (seção 7.2).
9. ✅ Cores ajustadas para contraste refletidas no Figma (13/10).
10. 🟡 Validar um alarme da central com a equipe.

**Próximo bloco sugerido:** ícone, R8, chave e `.aab` assinado → OAuth de release → teste interno na Play. O push de alertas da central (seção 7.1) entra antes da publicação se for decidido que é obrigatório na 1ª versão.
