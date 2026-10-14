# Histórico de alterações

## [Unreleased]

### Adicionado

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
