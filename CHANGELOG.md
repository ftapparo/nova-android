# Histórico de alterações

## [Unreleased]

### Adicionado

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

- Sessão mantida quando a renovação do token falha por falta de rede (comum ao acordar o aparelho): só uma recusa definitiva (401 ou 400) encerra a sessão. A consulta da residência tenta de novo sozinha, com espera de 2 s a 30 s, e mantém a última informação na tela, para a central de incêndio não sumir enquanto a conexão volta.
- A tarefa instrumentada do J4 agora instala os APKs com ADB e exige casos aprovados no AndroidJUnitRunner, evitando sucesso falso do executor Gradle.
- Ajustado o convite de cadastro em fonte ampliada e removido o indicador de atualização que deslocava os serviços da home.
- Requisição de logout sem corpo JSON vazio; a sessão local é encerrada mesmo quando a revogação falha.

### Removido

- Unidade padrão (tela, diálogo, selo e preferência local) e a tela intermediária "Idioma e região", removidas do Figma.
- Drawables sem uso: `nova_account`, `nova_info_status`, `nova_languages`, `nova_mic_off`, `nova_refresh`.
