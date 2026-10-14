# Histórico de alterações

## [Unreleased]

### Adicionado

- Localização para ligar o exaustor (decisão de 13/10/2026): no toque em "Ligar exaustor" ou "Reiniciar tempo", o app do morador lê a localização uma vez (`play-services-location`, só durante o uso) e a envia no `POST /exhausts/{id}/on`; a API confere se está na área do condomínio. A equipe é isenta e não vê pedido de permissão. Sem permissão, com só a localização aproximada ou com a localização desligada, nada é enviado e a tela mostra o aviso com a ação (pedido do sistema, configurações do app ou diálogo para ligar a localização); depois, o comando só sai com novo toque. Recusas da API (fora do condomínio, imprecisa, simulada) aparecem pelo `title` estável do 403, sem tratar como perda de acesso nem como comando incerto. Telas 25c–25i no Figma; desligar nunca exige localização; as coordenadas não são guardadas nem registradas.
- Tempo real pelo WebSocket `/v3/ws` com o app aberto (`core/realtime/RealtimeClient.kt`): ticket de uso único, reconexão com ticket novo (na hora quando o token expira, código 4001; com espera de 1 a 30 s nos demais casos; sem nova tentativa quando a conta não tem acesso). Os eventos só disparam a reconsulta do REST: a central quando muda conexão, reinício ou contadores (o estado chega a cada ~1 s; no máximo uma reconsulta a cada 2 s) e o exaustor quando muda o da unidade ativa. Com o socket conectado, a consulta periódica vira rede de segurança (central 60 s, exaustor 30 s); se ele cair, volta na hora a 10 s e 5 s.
- Push nativo (FCM) com a API `/notifications/*`: alarme de incêndio (canal próprio com som de alarme), alertas da central para a equipe, aviso antes de desligar o exaustor e de exaustor desligado (só para quem ligou ou reiniciou por último) e comunicados novos. `NovaApplication` passa a manter sessão, cliente da API e registro do push no processo, porque o serviço do FCM roda com o app fechado. O aparelho é registrado só quando algo muda (conta, token, permissão, canal do alarme, versão) e removido no logout; push sem sessão é descartado. O toque abre a central, o exaustor ou o detalhe do comunicado, se a conta ainda tiver acesso. A notificação não aparece quando a tela do aviso está aberta (a tela se atualiza na hora); o alarme aparece sempre. Telas: cartão "Ative as notificações" na home (Android 13+), faixa da tela 26 condicional com "Ativar notificações" e Configurações → Notificações (preferências e teste). Validado no Galaxy J4 com o projeto Firebase `nova-residence-login`.
- Carrossel de comunicados da home vindo pronto de `GET /messages/carousel`: a API decide permanência (TTL padrão de 24 h, TTL 0 sai ao ser lido, fixados) e ordem por prioridade da categoria. Depois de marcar um comunicado como lido, o app reconsulta o carrossel, para o de TTL 0 sair na hora.
- Termo de Uso (13) e Política de Privacidade (14) v1.0: página vigente publicada pela API (`GET /legal`), em WebView restrito (sem JavaScript; links e PDF abrem fora do app) e com o texto na escala do corpo do app. Aviso "Ao continuar, você concorda com o Termo de Uso e a Política de Privacidade" nas boas-vindas e no cadastro; o cadastro envia a versão aceita e o Google a envia quando o catálogo carrega. Tela de aceite para conta antiga ou versão nova do Termo (`mustAcceptTerms`).
- Exclusão da própria conta (16) com `DELETE /auth/me`: a sessão só é apagada depois da confirmação da API; em falha, a pessoa continua conectada e vê o erro. Equipe vê o botão desabilitado (contas removidas pela administração).
- CPF mascarado nos dados pessoais (15), vindo de `person.cpfMasked`; o CPF completo não chega ao app.
- Comunicados do condomínio (telas 08d, 17 e 18) em `feature/message`, com `/messages` da API v3: carrossel dos 5 mais recentes na home, contador de não lidas no sino, lista com filtro Todas/Não lidas agrupada por dia e detalhe que marca como lido.
- Central de incêndio redesenhada (08c e 26–30c): normal, atenção com o local do aviso, sem comunicação e alarme com orientações de segurança; histórico de 30 dias com filtros e ícones por tipo; detalhe com "Dados técnicos". Usa os campos interpretados `event`, `display` e `activeAlarms` da API, com alternativa para servidores antigos.
- Botão de mostrar/ocultar senha em todos os campos de senha.
- Central de incêndio (telas 08c e 09–13) em `feature/fire`, somente leitura: estado normal, atenção, sem comunicação e alarme ativo, histórico de 30 dias com paginação por cursor e detalhe do evento. Consulta a cada 10 s enquanto a área logada está visível.
- Ícones exportados do Figma e densidade proporcional ao frame de 412 dp em telas mais estreitas.
- Figma concluído da 1ª etapa: tela 20 (exaustor em manutenção), botão "Reiniciar tempo" (tela 16, reenvio do `/on` com 180 minutos) e estado real do exaustor na home (08b).
- Suporte aos campos `command` e `maintenance` do exaustor, criados na API v3 nesta entrega.
- Fluxo de commits com revisão, mensagens completas em português brasileiro, changelog e aprovação explícita.
- Boas-vindas, login por e-mail e Google, cadastro, recuperação por código e troca obrigatória de senha.
- Home com vínculo residencial, nome, unidades, bloqueios e permissões retornados pela API v3.
- Sessão criptografada com Android Keystore, refresh serializado e proteção contra respostas pendentes após logout.
- Testes com MockWebServer e verificações de interface e armazenamento criptografado no emulador.
- Splash com a marca oficial e revisão das telas 01 a 08 conforme o Figma atualizado.
- Telas 14 a 19 de exaustores, com consulta e acionamento pela API v3, atualização periódica, tempo retornado pelo servidor e proteção contra comandos duplicados.
- Telas 20 a 24 de perfil, unidades, confirmação de unidade padrão e idioma, com preferências locais por conta/dispositivo.

### Alterado

- Respostas do formulário "Segurança dos dados" da Play em `docs/seguranca-dos-dados.md` (e-mail, nome, IDs de usuário, outras ações, IDs do dispositivo e diagnóstico; nada compartilhado), alinhadas à Política de Privacidade 1.2. Volume do tempo real medido: ~0,9 MB por hora de app aberto, sem necessidade de filtro na API.
- Sincronização bidirecional com o Figma (14/10/2026): menu inferior com 4 dp entre ícone e rótulo; carrossel de comunicados com cartões de 356 × 208 dp, respiro vertical de 12 dp e o próximo cartão aparecendo até a borda da folha. No Figma, 17 frames novos (push, estados da home, do exaustor e da central). Testes de tela para as preferências de notificação, o cartão de permissão e a faixa da central, com a tela mantida acesa durante a suíte. Registro em `docs/revisao-figma-e-integracao.md`.
- Figma sincronizado com o app (13/10/2026): avisos do Termo nas boas-vindas e no cadastro, nova tela de aceite (04b), Termo e Política v1.0 (13, 14), dados pessoais com CPF mascarado (15), exclusão de conta incluindo o estado da equipe (16, 16b), textos das faixas da central (26, 27) e cores de contraste dos tokens do app. Registro em `docs/revisao-figma-e-integracao.md`.
- Revisão das telas contra o Figma de 12–13/10/2026 no Galaxy J4: menu inferior Início · Alarme · Exaustor com ícone preenchido no item selecionado; configurações pelo ícone de perfil e mensagens pelo sino; cabeçalho com a pílula da unidade; margem de 28 dp; cadastro com senha única; exaustor com cartão unidade/equipamento e ilustração por estado; cor primária #4C1D95 e splash #2B0F44.
- Desempenho em aparelhos lentos: ilustrações em WebP por densidade (~3,6 MB → ~1 MB) e pré-carregadas, JSON decodificado fora da thread da interface e histórico da central montando só os itens visíveis.
- Menu inferior refeito a partir do novo componente do Figma: item selecionado em quadrado roxo com ícone branco, barra de 72 dp com borda e sombra superiores.
- Barra dos botões do Android preta com ícones claros do splash até as telas, em vez do cinza-claro padrão que aparecia em aparelhos como o Galaxy J4.
- Revisão completa contra o Figma no J410G: autenticação, home, exaustor, configurações, barra superior com chevron, navegação inferior ("Início · Alarme · Exaustores · Perfil", sem borda e com sombra) e splash com o mesmo tamanho de logo no nativo e no Compose.
- Telas 02–08, 14–19 e 21–23 atualizadas para o Figma renumerado: Google e cadastro nas boas-vindas; login só com e-mail e senha; validação com reenvio em botão; home com "Apto. 124 - Torre A" e linhas de serviço com estado; exaustor com identidade do equipamento e estado de operação; configurações com e-mail e Código Freedom; seleção de unidade por rádio; navegação "Exaustor" e "Configurações".
- "Desligando" passa a ser identificado pela direção do comando retornada pela API, e não mais exibido como "Ligando".
- Hierarquia tipográfica, espaçamentos, margens adaptativas, formulários, navegação e estados do exaustor conforme `DESIGN_GUIDE.md` e o Figma revisado, com comparação no J410G.
- Tema e componentes compartilhados em Compose e Material 3 conforme o Figma, com assets locais.
- Contrato documentado para aceitar também o papel `morador`, preservando a configuração OAuth confirmada.
- Configuração de rede HTTPS, autenticação Bearer, erros tipados e bloqueio temporário por `Retry-After`.
- Google integrado à tela de login, confirmação de senha no cadastro e navegação inferior com os novos ícones do Figma.
- Duração e desligamento do exaustor delegados integralmente à API; respostas inconclusivas exigem atualização do estado antes de novo acionamento.

### Corrigido

- Versão do Termo de Uso consultada na hora do cadastro, do login com Google e da tela de aceite: o catálogo em memória podia estar desatualizado quando uma versão nova é publicada com o app aberto, e o cadastro mandaria a anterior (recusada pela API).
- Termo de Uso e Política de Privacidade abertos sem cache: o endereço da página não muda entre versões, e a tela de aceite podia mostrar por até 1 h o texto anterior enquanto registrava a versão vigente.
- Tela de aceite com o título "Aceite do Termo de Uso", que vale para quem nunca aceitou e para versão nova (antes "Termo de Uso atualizado").
- "Informativos do condomínio" só aparece para quem pode ler comunicados (equipe ou morador com unidade liberada); antes a conta pendente via um cartão vazio.
- Sessão mantida quando a renovação do token falha por falta de rede (comum ao acordar o aparelho): só uma recusa definitiva (401 ou 400) encerra a sessão. A consulta da residência tenta de novo sozinha, com espera de 2 s a 30 s, e mantém a última informação na tela, para a central de incêndio não sumir enquanto a conexão volta.
- A tarefa instrumentada do J4 agora instala os APKs com ADB e exige casos aprovados no AndroidJUnitRunner, evitando sucesso falso do executor Gradle.
- Ajustado o convite de cadastro em fonte ampliada e removido o indicador de atualização que deslocava os serviços da home.
- Requisição de logout sem corpo JSON vazio; a sessão local é encerrada mesmo quando a revogação falha.

### Removido

- Confirmação de senha no cadastro e ícones que deixaram de ser usados na revisão do Figma.
- Unidade padrão (tela, diálogo, selo e preferência local) e a tela intermediária "Idioma e região", removidas do Figma.
- Drawables sem uso: `nova_account`, `nova_info_status`, `nova_languages`, `nova_mic_off`, `nova_refresh`.
