# Revisão do Figma e integração — 29/09/2026

Arquivo de referência: [Nova Residence no Figma](https://www.figma.com/design/kixzSbydhXP4k7qTSKWJbK/Sem-t%C3%ADtulo?node-id=0-1).
Estrutura, propriedades, textos, componentes e assets foram consultados pelo MCP do Figma. As capturas locais ficam em `artifacts/`, fora do Git.

## Telas revisadas

| Tela | Nó Figma | Implementação |
|---|---|---|
| 01 — Splash | `56:1138` | Marca branca oficial, fundo roxo e inicialização nativa/Compose sem atraso artificial. |
| 02 — Boas-vindas | `4:29391` | Logo, título, descrição e dois botões atualizados. |
| 03 — Login | `4:29479` | Google integrado ao formulário; textos e espaçamentos atualizados. |
| 04 — Cadastro | `4:29564` | Confirmação local da senha e botão inferior; API recebe apenas e-mail/senha. |
| 05 — Validação | `4:29697` | Código de seis dígitos, foco, reenvio e orientação de spam; adaptado à recuperação real. |
| 06 — Recuperação | `4:29743` | Formulário por e-mail e aviso de carregamento durante o envio. |
| 07 — Erro | `4:29808` | Ícone, faixa informativa e ações conforme o modelo. |
| 08 — Home | `6:3353` | Cabeçalho, contexto de unidade e novos ícones; dados reais, sem comunicado demonstrativo. |
| 14 — Exaustor desligado | `4:30163` | Consulta do equipamento autorizado da unidade ativa e comando de ligar. |
| 15 — Ativando | `4:30223` | Bloqueio de comando duplicado e progresso indeterminado. |
| 16 — Funcionamento | `4:30301` | Estado real e contador derivado de `expiresAt`; nenhum prazo inventado. |
| 17 — Finalizando | `4:30362` | Prazo encerrado enquanto o servidor ainda informa ligado; aguarda atualização. |
| 18 — Offline | `4:30437` | `moduleOnline: false`, aviso e acionamento desabilitado. |
| 19 — Erro | `4:30502` | Erro real; atualização antes de nova tentativa quando o resultado é inconclusivo. |
| 20 — Configurações | `4:30567` | Pessoa/unidade reais, minhas unidades, idioma, desconectar e versão do build. |
| 21 — Minhas unidades | `4:30653` | Vínculos reais, motivos de bloqueio, unidade atual e padrão. |
| 22 — Unidade padrão | `4:30817` | Seleção com confirmação; preferência local separada por conta. |
| 23 — Idioma e região | `4:30920` | Navegação para o idioma local do aplicativo. |
| 24 — Seleção de idioma | `4:30970` | Português (Brasil) disponível; inglês/espanhol desabilitados enquanto não há traduções completas. |

## Comportamentos preservados e adaptações

- Recuperação usa e-mail, resposta genérica e validade de 15 minutos, conforme contrato aprovado. O cadastro não exige código porque `/auth/signup` já retorna sessão.
- Senhas e códigos ficam somente em memória da composição. A confirmação de senha não é enviada ao cadastro.
- O indicador circular de recuperação usa a versão estável do Material 3 instalada; o indicador Expressive do Code Connect ainda não está disponível nessa versão. Nenhuma dependência foi atualizada apenas para essa animação.
- Barras de sistema, teclado, rolagem, alvos de toque e escala de fonte seguem Android. Não são desenhadas barras de status fictícias do Figma.
- A home continua sem comunicados ou estados demonstrativos. Exaustores agora abrem a funcionalidade; a central de incêndio permanece indisponível nesta entrega, pois as telas 09–13 não fazem parte deste pedido.
- O comando do exaustor envia `minutes: 180` em `POST /v3/api/exhausts/{id}/on`, conforme duração confirmada. A API controla a contagem e o desligamento; o app não chama `/off` automaticamente.
- Consulta somente enquanto a tela está em primeiro plano: 5 segundos normalmente, 2 segundos durante transições. As falhas não repetem comandos. `Retry-After` bloqueia consultas/comandos até o prazo.
- Encerrar a tela, mudar a unidade ou sair da conta invalida respostas pendentes na interface. No retorno, o estado é consultado novamente.
- A unidade padrão não troca a unidade ativa imediatamente. No próximo acesso, é usada se ainda vinculada e liberada; caso contrário, aplica-se a seleção inicial já existente.
- Os dados dos testes e capturas são simulados. O fluxo normal do aplicativo consulta a API e não substitui silenciosamente falhas por dados de demonstração.

## Pontos para conferir/corrigir na API

1. **Contrato do ciclo automático:** o app solicita 180 minutos à API, que controla o desligamento. O OpenAPI deve manter essa responsabilidade e o retorno de `expiresAt` documentados.
2. **Contador:** quando houver ciclo temporizado, retornar `expiresAt` em epoch de milissegundos também nas consultas subsequentes. Se vier `null`, o app mostra funcionamento sem inventar contagem regressiva.
3. **Finalização explícita:** o contrato só possui `iniciando`, `executado`, `erro` e `null`, sem fase `finalizando` nem direção do último comando. Hoje a tela 17 é apresentada quando o prazo retornado terminou e o equipamento continua ligado. Um campo de fase/direção permitiria distinguir com precisão preparação, operação e desligamento, inclusive em outro dispositivo.
4. **Garantias após erro:** a API não informa que o equipamento ficou protegido ou que houve encerramento seguro. Os textos foram adaptados com autorização. Para reproduzir essas afirmações literalmente, o backend precisa retornar uma confirmação específica, não apenas um erro genérico.

Os endpoints de consulta e ativação já existem e foram integrados. Não foi necessário inventar endpoint nem trocar essa integração por mockup. As seis variações foram exercitadas com respostas simuladas nos testes, sem acionar equipamento real.

## Pendências que não são da API

- Inglês e espanhol dependem de tradução completa do aplicativo; a interface e os commits permanecem em português brasileiro.
- Unidade padrão e idioma são locais. Sincronização dessas preferências entre dispositivos exigiria um contrato adicional, caso esse comportamento seja desejado.
- O fluxo de exaustor do Figma é orientado à unidade residencial ativa. Uma operação administrativa dos 24 equipamentos, sem unidade residencial vinculada, precisa de um fluxo próprio de seleção; não foi inventada uma tela administrativa.
- Os testes automatizados usam exclusivamente MockWebServer ou estado de UI simulado; o acionamento real autorizado pelo usuário foi validado em sessão anterior.
- O login Google real ainda depende da configuração OAuth registrada em `validacao-inicial.md`.

## Revisão de densidade — 30/09/2026

- As telas 01–08 e 14–24 foram consultadas novamente pelo MCP após a revisão do Figma. A paleta permaneceu igual; mudaram sobretudo tipografia e espaçamentos.
- A tipografia compartilhada de título passou para 22/28 sp. Autenticação usa margens horizontais de 20 dp e fluxo vertical compacto. A home usa cabeçalho e linhas de serviço menores. No exaustor, o ícone principal mede 48 dp e o contador 36/44 sp. A navegação inferior mede 80 dp.
- As capturas do J410G (360 dp de largura, fonte em 100%) estão em `artifacts/figma-density-j4/screenshots/`. Foram comparadas aos nós Figma acima. A largura de referência do Figma é 412 dp, por isso textos longos quebram mais cedo no aparelho sem perder a escala de fonte ou os alvos de toque.
- `lint`, `testDebugUnitTest` (27 casos), `assembleDebug` e `installDebug` passaram no J410G. Os nove testes instrumentados concluíram sem falhas pela execução direta do AndroidJUnitRunner no aparelho.

## Revisão conforme DESIGN_GUIDE.md e Figma atualizado

- O guia de produto passou a orientar as decisões de hierarquia e adaptação; o Figma continua como referência visual. Foram conferidos novamente os nós 01–08 e 14–24 pelo MCP.
- Os papéis compartilhados agora usam barra 18/24 sp, título principal 20/26 sp, linha 16/22 sp, corpo 14/20 sp e botão 14/20 sp, com fonte padrão Android. A cor suave do primário foi unificada em um token.
- O J410G usa margens de 16 dp; larguras maiores usam 20 dp. Formulários ficam limitados a 480 dp e o conteúdo de residência e exaustores a 600 dp, centralizados em janelas largas.
- O link de recuperação tornou-se `TextButton`, campos usam canto de 10 dp, e o ícone de voltar tem 24 dp dentro do alvo nativo de 48 dp. A navegação inferior conserva alvo mínimo de 80 dp sem consumir novamente o inset do sistema.
- O exaustor agora apresenta estado, aviso e ação em fluxo vertical rolável, evitando afastá-los em telas altas. O contador foi ajustado a 20/26 sp conforme o novo Figma; a API continua responsável pelo desligamento.
- Na home, uma atualização de dados existentes não insere mais um indicador entre o cabeçalho e os serviços, evitando deslocamento visual temporário.
- Validação final: `lint`, 27 testes unitários, `assembleDebug` e `installDebug` passaram. O AndroidJUnitRunner concluiu nove testes no J410G físico e nove com largura virtual de 600 dp. O formulário passou também com fonte em 130%, largura de 412 dp e janela curta de 740 × 360 dp; nesta última, ações e erros foram alcançados por rolagem.
- O teclado Samsung aberto no login foi inspecionado no aparelho: campo e botão principal permaneceram visíveis, e o conteúdo inferior continuou rolável. A resolução física de 720 × 1480 e a escala de fonte 1,0 foram restauradas após os cenários temporários.
- A tarefa `Instrumented` do fluxo físico foi corrigida: compila os APKs, instala com `adb install -r` e exige `OK (N tests)` do AndroidJUnitRunner. O novo caminho executou nove casos aprovados no J410G.

## Validação

- `lint`: aprovado, 0 erros e 17 avisos (recomendações de atualização de dependências, recursos do template e uso de KTX; a gravação das preferências verifica o resultado de `commit()`).
- `testDebugUnitTest`: 27 testes aprovados, incluindo 7 novos testes de exaustor e a regra de preferência por unidade padrão.
- `assembleDebug` e `assembleDebugAndroidTest`: aprovados.
- `ScreenTest`: 8 testes aprovados em cada configuração: 412 × 892 dp/fonte 100% e 360 × 640 dp/fonte 130%, no emulador Android 9/API 28.
- Capturas finais: `artifacts/revisao-14-24/412-final/` e `artifacts/revisao-14-24/360-fonte130/`. Incluem autenticação, splash, home, seis estados de exaustor, perfil, unidades, diálogo e idioma. A captura do diálogo usa a janela Android, pois há duas raízes Compose.
- As capturas foram inspecionadas e comparadas às referências do MCP: cores, hierarquia, ícones, campos, ações, cartões, estados desabilitados e rolagem em tela pequena.
- A splash Compose foi verificada no ambiente disponível. Os recursos de inicialização nativa, incluindo Android 12+, foram compilados; a transição de partida a frio ainda precisa de conferência manual nas versões de Android utilizadas pelo condomínio.
- Nenhum teste acionou a API de produção ou hardware do condomínio. Os dados usados nas capturas são fixtures, identificados no código de teste.

## Figma concluído da 1ª etapa — 30/09/2026

O arquivo foi renumerado. Referências novas: 08a–08e (variações da home), 14 `110:1166`, 20 `132:1203` (manutenção), 21 `135:1307` (configurações), 22 `135:1367` (selecionar unidade) e 23 `135:1543` (selecionar idioma). As antigas telas "Unidade padrão" e "Idioma e região" deixaram de existir.

| Tela | Situação |
|---|---|
| 01 Splash | Sem mudança |
| 02–07 Autenticação | Atualizadas: Google e "Criar conta" movidos para as boas-vindas; login só com e-mail/senha; nota do cadastro; "Validar conta" com reenvio em botão; erro sem ícone e com nova descrição |
| 08a/08b Home | Atualizadas: saudação com primeiro nome, "Apto. 124 - Torre A", linhas de serviço com estado real do exaustor |
| 08c/08d/08e Home | Pendentes: dependem da CIE e de uma rota de mensagens que não existe |
| 09–13 Central de incêndio | Pendentes (rotas de leitura já existem na API) |
| 14–19 Exaustor | Redesenhadas: identidade do equipamento, estado de operação, faixa de tempo restante e "Reiniciar tempo" |
| 20 Exaustor em manutenção | Implementada, com o campo `maintenance` criado na API |
| 21–23 Configurações, unidade e idioma | Atualizadas; unidade padrão removida |

### Mudanças na API (`src/v3`)

- `Exhaust` ganhou `command` e `maintenance`; nova rota `PUT /exhausts/{id}/maintenance` (síndico/admin); `/on` responde 409 em manutenção; reinício pelo `/on` documentado.

### Divergências a corrigir no Figma

1. Barra superior usa Regular no Figma; o guia define 18/24 sp **Medium** (mantido).
2. Tela 05 diz "Enviamos um código… expira em 10 minutos"; o contrato é resposta genérica e 15 minutos (mantido o texto do contrato).
3. Tela 06 usa o rótulo "E-mail, celular ou CPF"; a API aceita apenas e-mail (mantido "E-mail").
4. Tela 23 mostra inglês e espanhol habilitados; ficam desabilitados até existir tradução.
5. Tela 19 não tem variação para resultado inconclusivo; o app mantém "Não foi possível confirmar a ativação" / "Atualize o estado antes de tentar novamente" quando o servidor não confirma a falha.
6. Os ícones reutilizam os vetores já importados do Figma (chama, ventilador, casa, perfil, prédio), com cor aplicada por tint; conferir no aparelho.

### Pendências

- API: rota de mensagens/comunicados para 08d/08e; o painel v2 não respeita a manutenção.
- App: nada foi executado nesta entrega (sem build, lint, testes nem capturas); validação a fazer em conjunto.
