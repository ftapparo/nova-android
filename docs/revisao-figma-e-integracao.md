# Revisão do Figma e integração — 29/09/2026

Arquivo de referência: [Nova Residence no Figma](https://www.figma.com/design/kixzSbydhXP4k7qTSKWJbK/Sem-t%C3%ADtulo?node-id=0-1).
Estrutura, propriedades, textos, componentes e assets foram consultados pelo MCP do Figma. As capturas locais ficam em `artifacts/`, fora do Git.

## Figma → Android — nova revisão de 15/10/2026

Revisão consultada pelo MCP após a tradução do app. **O Figma é referência visual; os textos do app são os de `strings.xml`** (decidido em 15/10/2026): a redação do desenho não substitui a dos recursos. O Figma continua somente em português; os recursos `values-en` e `values-es`, a seleção local de idioma e o contrato traduzido da API foram preservados. As seções anteriores deste documento são registros históricos e usam a numeração anterior.

| Numeração atual | Nós de referência | Aplicação |
|---|---|---|
| 01–09 · Splash e autenticação | `56:1138`, `4:29391`, `323:1063`, `4:29479`, `309:1089`, `4:29564`, `4:29697`, `4:29743`, `4:29808` | Conferidos logo, margens, campos, ações e recuperação; textos do app mantidos. |
| 10a–10j · Início | `214:857`, `214:961`, `214:1066`, `90:2382`, `321:1243`, `323:1123`, `323:1216`, `323:1309`, `323:1400`, `330:1363` | Seção de comunicados e estado sem unidade conferidos (textos do app mantidos); carrossel de 356 × 208 dp e navegação preservados. |
| 11a–11b · Mensagens | `217:856`, `217:877` | Filtros, linhas, resumo e detalhe conferidos; conteúdo continua vindo da API. |
| 12–14 · Configurações, unidade e idioma | `231:809`, `231:879`, `231:954` | Instrução com mínimo de 40 dp e intervalo de 28 dp antes das opções; informação dos vínculos no cartão informativo padrão (`NovaInfoStrip`, como exaustor e central), também aplicado à validação de conta (07), notificações e documentos legais. Três idiomas continuam habilitados. |
| 15a–15e · Notificações | `321:916`, `321:977`, `321:1045`, `321:1111`, `321:1177` | Fundo #F5F6FA, como todas as telas de 12 a 20 (o branco anterior era erro do Figma, corrigido em 15/10/2026); preferências e estados reais preservados. |
| 16–20b · Privacidade e conta | `233:816`, `233:897`, `233:954`, `233:1011`, `234:881`, `310:842` | Dados pessoais com intervalo de 12 dp entre elementos; aviso de exclusão com o texto do app. |
| 21a–21f · Central | `257:856`, `320:869`, `257:939`, `257:1024`, `323:1613`, `323:1660` | Três ilustrações novas, quadradas; título de estado 28/38, apoio 16/22, agrupamento de 4 dp, blocos de 16 dp e faixas com mínimo de 64 dp. Alarme com imagem 200 dp, títulos 28/28 e orientações roláveis. Selos sem ponto, centralizados, como na revisão. |
| 22a–22d · Histórico e detalhe | `265:979`, `269:829`, `269:916`, `269:1003` | Conferidos filtros, categorias, linhas e dados retornados. A regra vigente de histórico só para equipe na versão 1.0.0 permanece. |
| 23a–23h · Exaustor | `245:942`, `246:2841`, `246:2912`, `246:3212`, `246:3054`, `246:3298`, `323:1493`, `323:1553` | Quatro ilustrações novas de 250 dp, título 28/38, apoio/contador 16/22, agrupamento de 4 dp e faixa com mínimo de 64 dp. |
| 23i–23o · Localização | `341:1388`, `341:1434`, `341:1480`, `341:1526`, `341:1572`, `341:1618`, `341:1664` | Ilustração própria com marcador de localização (a mesma nos sete frames), composição do exaustor e textos do app; permissões, verificação e comandos não mudam. Esses frames ainda mostram o ponto no selo “Disponível” e 14 dp entre ação e faixa, restos da versão anterior: o app usa o selo sem ponto e 16 dp em todos os estados, como 23a–23h (corrigir no Figma). |

Os oito PNGs originais foram obtidos dos assets retornados por `get_design_context`, sem rasterizar telas. São imagens com transparência de 1254 × 1254 px (desligado: 1242 × 1238). Gerados 40 WebP RGBA com Pillow/libwebp, qualidade 85, método 6, alpha 100 e redimensionamento Lanczos com recorte central quadrado: exaustor 250/375/500/750/1000 px e central 200/300/400/600/800 px, de mdpi a xxxhdpi. Total: 1.043.064 bytes. A decodificação assíncrona e o cache continuam em `NovaIllustration`.

Mantidas diferenças intencionais: botões padronizados em 48 dp e 14/20 sp Medium em todas as telas (o Figma mistura 48/52 dp, 14 Medium/15 SemiBold e cantos de 24 dp/pílula no exaustor e na central); textos funcionais em fonte Android; conteúdo cresce em EN/ES e com fonte ampliada; margem lateral de 28 dp conforme o guia (alguns frames da home ainda usam 20); documentos legais e versão são os vigentes da API, sem fixar a versão 1.1 do desenho; cadastro não promete prazo de análise de 24 horas, que a API não garante; exclusão continua imediata após confirmação, com entrada “Excluir conta”; erros gramaticais “da evento” do Figma continuam corrigidos. Nenhum arquivo ou texto foi escrito no Figma nesta rodada.

Validação concluída: `lint` sem erros (25 avisos), `testDebugUnitTest` com 73 testes, `assembleDebug`, `assembleDebugAndroidTest` e 13 testes `ScreenTest` no Galaxy J4 (Android 8.1), incluindo os três idiomas a 100%/130% de fonte com insets e ações roláveis. O APK final foi instalado preservando os dados do app. Testes usam dados fictícios e callbacks locais, sem comandos ao hardware ou testes contra a API de produção. Chaves traduzíveis e parâmetros de formatação conferidos nos três recursos.

Evidências em `artifacts/figma-sync-20261015/`: nove comparações Figma/aparelho lado a lado, normalizadas para largura de 412 dp com a barra de status recortada, e oito painéis PT/EN/ES (100%/130%). Conferidos imagens, hierarquia, selos, margens, quebras e acesso às ações. A ilustração menor nas capturas com navegação inferior acompanha a altura útil do J4; diferenças das métricas da fonte nativa e das barras do Android são mantidas, sem compensações por aparelho. Capturas com fonte ampliada mostram posições de rolagem para alcançar ações e avisos. Não houve inspeção em tablet/janela ≥600 dp ou Android 13+ nesta rodada.

## Sincronização app → Figma — 13/10/2026

Revisão executada pelo MCP a partir das pendências do `STATUS-PROJETO.md` e dos textos e tokens implementados no app:

| Tela | Nó Figma | Atualização |
|---|---|---|
| 02 — Boas-vindas | `4:29391` | Aviso de concordância com Termo e Política, com links; margem de 28 dp. |
| 04 — Cadastro | `4:29564` | Mesmo aviso antes de Continuar; alinhamento do formulário à margem de 28 dp. |
| 04b — Aceite do Termo | `309:1089` | Nova tela com orientação, documento rolável, links, Aceitar e continuar e Sair; sem voltar. |
| 13 — Termo de Uso | `233:897` | Conteúdo oficial v1.0 do repositório da API, data de vigência e link para PDF; removido texto provisório. |
| 14 — Política de Privacidade | `233:954` | Conteúdo oficial v1.0, data e link para PDF; removido texto provisório. |
| 15 — Dados pessoais | `233:1011` | Nome, CPF mascarado de exemplo, e-mail e unidade ativa, na ordem do app; removidos dados complementares. |
| 16 — Excluir conta | `234:881` | Texto sobre exclusão imediata e necessidade de nova conta e vínculo. |
| 16b — Excluir conta, equipe | `310:842` | Novo estado com botão desabilitado e explicação de remoção pela administração. |
| 26 — Central normal | `257:856` | Texto de atualização enquanto o app está aberto, sem promessa de push ainda não implementado. |
| 27 — Central em atenção | `257:939` | Faixa sobre ocorrência registrada e verificação necessária; correções de “um aviso” e “Local do aviso”. |

Avisos legais usam instâncias do componente `Nova / LegalLinksText` (`307:825`). Textos e documentos são editáveis, com Auto Layout e fonte Roboto; documentos longos têm viewport com rolagem vertical. As cores de estado verde, amarelo e offline foram sincronizadas com `NovaTheme.kt` (#237A4B, #8A6A00 e #5B6473), junto dos textos secundários em superfícies tingidas identificadas na revisão. O token `amber/700` também foi atualizado.

Validação: leitura da estrutura, fontes e imagens pelo MCP, seguida de capturas dos frames a 1×; corrigidos reflow dos parágrafos e padding duplicado do cadastro. Imagens foram preservadas apenas para marca, ícone Google e ilustrações existentes, sem rasterizar interfaces. Esta revisão não inclui nova captura do aparelho nem comparação lado a lado com o app em execução; as capturas locais disponíveis são anteriores às mudanças de 13/10. Lint, testes e build não foram executados, pois não houve alteração de código Android. Estados futuros de localização, push e operação dos 24 exaustores permanecem pendentes.

## Telas revisadas

### Sincronização bidirecional — 14/10/2026

No Android, aplicados do Figma o intervalo de 4 dp entre ícone e rótulo do menu inferior e cartões do carrossel com 356 × 208 dp e padding vertical de 12 dp. O viewport chega à borda direita da folha para continuar mostrando parte do próximo cartão. O cartão de ativação das notificações também recebeu `NovaOnTint` no texto de apoio e na ação secundária para manter contraste sobre a superfície tingida. As alterações de push já presentes no workspace foram preservadas.

No Figma, atualizados Configurações com acesso a Notificações, recuperação com nova senha e validade de 15 minutos, opções de idioma desabilitadas, margens de 28 dp e navegação de 80 dp nas telas de serviço, cores de superfície/estado e ícone vetorial de ligar conforme `nova_power.xml`. Os cartões seguintes do carrossel foram unificados com a edição do principal. Termo, Política e exemplo de aceite agora mostram o conteúdo oficial 1.1; a versão continua dinâmica no app.

| Frames novos | Nós |
|---|---|
| 03b — troca obrigatória de senha | `323:1063` |
| 08e — home com ativação de notificações | `321:1243` |
| 08f–08j — cadastro pendente, sem unidade, unidade bloqueada, erro e conta bloqueada | `323:1123`, `323:1216`, `323:1309`, `323:1400`, `330:1363` |
| 09a–09e — notificações: morador, equipe, permissão negada, canal bloqueado e registro pendente | `321:916`, `321:977`, `321:1045`, `321:1111`, `321:1177` |
| 25a–25b — resultado inconclusivo e exaustor indisponível | `323:1493`, `323:1553` |
| 26b–26d — notificações desativadas, central sem comunicação e estado indisponível | `320:869`, `323:1613`, `323:1660` |

São 17 frames novos, editáveis, com Roboto e reaproveitamento de componentes. As telas longas de notificações e dos documentos têm viewport com rolagem vertical. Os exemplos representam estados implementados; localização e operação administrativa de exaustores continuam fora desta revisão porque ainda não foram implementadas.

### Localização para ligar o exaustor — 14/10/2026

Sete frames clonados da 25a (mesma ilustração, título, descrição, botão de 52 dp e faixa de informação), com rótulo do botão em largura automática. O app usa os mesmos textos (`ExhaustScreen.kt`, `locationCopy`); o estado "Verificando localização..." é só o rótulo do botão desabilitado e não tem frame.

| Frames novos | Nós |
|---|---|
| 25c — localização necessária (antes do pedido do sistema) | `341:1388` |
| 25d — localização não permitida (abre as configurações) | `341:1434` |
| 25e — só localização aproximada | `341:1480` |
| 25f — localização do aparelho desligada | `341:1526` |
| 25g — fora do condomínio (403 da API) | `341:1572` |
| 25h — localização imprecisa ou sem leitura | `341:1618` |
| 25i — localização simulada | `341:1664` |

Validação: `lint`, `testDebugUnitTest` (57 testes) e `assembleDebug` aprovados; 11 testes de tela aprovados no Galaxy J4, incluindo preferências de notificações e condição da faixa da central. Capturas usam dados simulados, sem consultas de teste à API de produção ou comandos ao hardware. Comparações lado a lado, com largura normalizada para 412 dp e barras de status recortadas, ficam em `artifacts/figma-sync-20261014/compare_*.png`. A ilustração menor e a altura útil diferente no J4 seguem a adaptação prevista no guia; a imagem do exaustor foi conferida contra o original do MCP e mantida. Há diferenças de métricas da fonte Android do J4 em relação ao Roboto do Figma, com quebras de linha e alturas de conteúdo variáveis; os tokens em dp/sp foram preservados, sem compensação manual por aparelho. Renderizações do Figma foram verificadas para reflow, fontes, hierarquia e ausência de interfaces rasterizadas. A janela de teste mantém a tela acesa e aguarda a composição antes das capturas para evitar falhas quando o aparelho entra em repouso.

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

## Revisão completa contra o Figma — 01/10/2026

Após o teste no J410G, várias telas ainda reproduziam a estrutura anterior. Todas as telas foram refeitas com os valores do design context do MCP e conferidas lado a lado (PNG do nó × captura do aparelho, mesma escala).

- **Densidade proporcional:** o J410G tem 360 dp; o Figma, 412 dp. Por decisão do produto, telas mais estreitas recebem densidade proporcional (`NovaResidenceTheme`), e margens, quebras e proporções ficam iguais às do Figma. A escala de fonte do sistema continua aplicada. `DESIGN_GUIDE.md` foi atualizado.
- **Ícones:** exportados do Figma como SVG pela API de plugin (somente leitura) e convertidos em vetores: chevron de voltar, casa, chama, ventilador, usuário, prédio, histórico, sirene, círculo-x, rota, microfone desligado, rádio e chevrons.
- **Barra superior:** chevron, título 18/24 Regular e fundo da tela.
- **Autenticação (02–07):** margens de 28 dp; espaçamentos de 16 dp descontando os 8 dp internos do campo Material; Google e cadastro na tela de boas-vindas; "Esqueci minha senha" como link; placeholder e legenda #49454F; tela de erro sem referência técnica.
- **Home (08a/08b/08c):** cabeçalho com margem de 28 dp e primeiro nome formatado, linhas de serviço de 12 sp com chevron Material, status real do exaustor e da central, faixa "Nenhuma mensagem nova" (estado vazio; não há rota de comunicados).
- **Central de incêndio (09–13):** implementada em `feature/fire`, somente leitura, com dados reais da API.
- **Exaustor (14–20):** componentes compartilhados, margem de 24 dp, quebras de linha do Figma e botão primário desabilitado em roxo translúcido.
- **Configurações e seleção (21–23):** rótulos de seção, linhas, rádio de 20 dp e nome em caixa de título.
- **Navegação inferior:** "Início · Alarme · Exaustores · Perfil", sem borda superior e com sombra projetada para cima, oculta em 12, 13, 22 e 23.
- **Splash:** o splash nativo usava 244 × 98 dp e o Compose centralizava descontando as barras, o que fazia o logo "encolher". Os dois agora usam o mesmo `@dimen/nova_splash_logo_*` (210 × 70 dp no Figma, proporcional no J4) e o mesmo centro. Medido no aparelho: logo com 39% da largura em todas as capturas, como no Figma.

### Divergências mantidas ou a corrigir no Figma

1. A tela 09/10 diz "Você será avisado se o estado da central mudar"; sem push nativo, o app diz que o estado é atualizado enquanto o aplicativo estiver aberto.
2. O menu do Figma alterna entre "Exaustor/Configurações" (14–21) e "Exaustores/Perfil" (08–11). O app segue 08a e a orientação do produto: "Exaustores" e "Perfil", sem borda.
3. O nó do menu ainda tem `border-t`; por orientação do produto, o app usa somente sombra.
4. Textos de contrato mantidos: código de 15 minutos com resposta genérica (05) e rótulo "E-mail" (06).

### Observação do aparelho

O relógio do J410G está 1 hora adiantado em relação ao horário do condomínio (eventos do painel às 15:51 aparecem como 16:51). A causa provável é uma regra antiga de horário de verão no sistema do aparelho; a API está correta.

### Validação

- `testDebugUnitTest` (incluindo 5 novos testes da central), `lint`, `assembleDebug` e `assembleDebugAndroidTest`: aprovados.
- `ScreenTest` no J410G pelo AndroidJUnitRunner: 9 testes aprovados (inclui central normal, atenção, alarme, histórico, detalhe e home com alarme, usando dados fictícios).
- No aparelho, com a API real: login, home, central (normal), histórico e detalhe reais, exaustor ligado e reiniciado (autorizado), configurações e seleção. Nenhum comando foi enviado à central de incêndio nem aos portões.

## Revisão do Figma de 12/10/2026

O arquivo foi reorganizado e renumerado. Todas as telas foram consultadas pelo MCP oficial (`get_metadata`, `get_screenshot`, `get_design_context` e `get_variable_defs`) e comparadas com capturas do J410G.

| Telas | Nós | Implementação |
|---|---|---|
| 01–07 Autenticação | `56:1138`, `4:29391`…`4:29808` | Novo texto de boas-vindas, separador "ou continue com", botão "Google". Cadastro com senha única, botão de mostrar senha e dica "Use pelo menos 8 caracteres." (a confirmação saiu do Figma; recuperação e troca obrigatória seguem o mesmo padrão). Nota de spam com ícone. |
| 08a–08c Home | `214:857`, `214:961`, `214:1066` | Cabeçalho #2B0F44 com pílula da unidade, sino (Mensagens) e perfil (Configurações); folha com cantos de 28 dp; cartões de serviço; "Informativos do condomínio" com estado vazio. |
| 08d, 17, 18 Comunicados | `90:2382`, `217:856`, `217:877` | Sem rota na API: Mensagens (17) mostra filtros e estado vazio; carrossel (08d) e detalhe (18) aguardam contrato. Nenhum dado demonstrativo. |
| 09–16 Configurações | `231:809`…`234:881` | Configurações, unidades, idioma, privacidade, termos, política, dados pessoais e exclusão de conta. |
| 19–24 Exaustor | `245:942`, `246:2841`…`246:3298` | Aba sem voltar, cartão "Unidade ativa · Equipamento", ilustrações por estado, contador HH:MM:SS, botão com ícone de ligar. |
| 26–30c Central | `257:856`…`269:1003` | Ilustrações por estado, alarme com orientações de segurança, histórico agrupado por dia com filtros e detalhes em seções com "Dados técnicos". |
| Menu inferior | `157:1183` | Três abas: Início · Alarme · Exaustor. |

### Decisões e divergências

1. **Cor primária** passou a #4C1D95 (`color/primary`); cabeçalho e splash, #2B0F44. Verde e cinza dos selos foram escurecidos para 4,5:1.
2. **Ilustrações** foram exportadas do Figma e geradas por densidade em WebP (ver "Ilustrações por densidade em WebP" abaixo). Em telas mais baixas que o frame (J4 com botões), encolhem para manter ação e aviso visíveis sem rolagem (DESIGN_GUIDE §6). O estado "sem comunicação" da central usa a ilustração normal em tons de cinza; o Figma não tem esse estado.
3. **Central**: os textos do Figma "Existe uma aviso", "Local da aviso" e "Detalhes da evento" foram corrigidos. "A equipe responsável está verificando a ocorrência" virou "A ocorrência foi registrada pela central e deve ser verificada pela equipe responsável", pois a API não confirma verificação em andamento. O aviso de "você será avisado" continua substituído (sem push nativo).
4. **Central — dados**: o app usa os campos interpretados `event`/`display` e `activeAlarms` da API v3; servidores que ainda não os enviam caem no tratamento anterior. "Local do aviso" vem de `/cie/events/active`, consultado só quando há falha ou supervisão.
5. **Recuperação (06)**: o rótulo é "E-mail", como a API exige (o Figma pedia "E-mail, celular ou CPF" e foi corrigido em 13/10/2026). O código (05) expira em 15 minutos pelo contrato, não 10.
6. **Dados pessoais (15)**: o CPF aparece mascarado ("•••.456.789-••", `person.cpfMasked`); "Dados complementares" não existem no contrato e não são exibidos.
7. **Excluir conta (16)**: funcional desde 13/10/2026 (`DELETE /auth/me`); a própria tela é a confirmação. Para a equipe, o botão fica desabilitado.
8. **Termos e política (13, 14)**: exibem a página vigente publicada pela API (Termo de Uso e Política de Privacidade v1.0), no lugar do texto provisório do Figma. Boas-vindas e cadastro ganharam o aviso "Ao continuar, você concorda com o Termo de Uso e a Política de Privacidade", e há uma tela de aceite para conta antiga ou versão nova do Termo — nenhum dos dois existe no Figma.
9. **Exaustor — erro**: o Figma não tem mais tela de erro; os estados "Falha na ativação" e "Não foi possível confirmar a ativação" usam o novo layout com os textos aprovados.

### Pendências na API (`src/v3`)

- Concluídas em 13/10/2026 (aguardando deploy): `/messages` (08d, 17 e 18), Termo/Política com aceite, `DELETE /auth/me`, página `/excluir-conta` e `person.cpfMasked` em `/residence/me`.

### Validação (12/10/2026)

- `lint`, `testDebugUnitTest` (39 testes), `assembleDebug`, `assembleDebugAndroidTest` e `installDebug`: aprovados. Os avisos restantes do lint são anteriores a esta revisão (manifesto, ícone do launcher, `ResidencePreferences`, versões de dependências).
- `ScreenTest` no J410G pelo AndroidJUnitRunner: 9 testes aprovados, com capturas de 01–30a usando dados fictícios.
- No aparelho, com a API real e a sessão já existente: home, central (normal), histórico e detalhe de evento, e exaustor (desligado). A produção já envia `event`/`display`; isso revelou que operações sem `place` repetiam o nome da zona ("Torre B · Torre B"), corrigido com teste. Nenhum comando foi enviado ao exaustor, à central ou aos portões.

### Ilustrações por densidade em WebP (12/10/2026)

As 7 ilustrações deixaram de ser PNG únicos em `drawable-nodpi` (990 px e 600 px, ~3,6 MB) e passaram a ter uma versão por densidade em WebP, geradas dos originais do Figma (1448 px e 1254 px) com `cwebp` 1.6.0:

```
cwebp -q 85 -alpha_q 100 -m 6 -sharp_yuv -resize <largura> 0 original.png -o drawable-<densidade>/nova_….webp
```

| Densidade | Exaustor (329 dp) | Central (200 dp) | Total das 7 |
|---|---|---|---|
| mdpi | 329 px | 200 px | 71 KB |
| hdpi | 494 px | 300 px | 117 KB |
| xhdpi (J4) | 658 px | 400 px | 168 KB |
| xxhdpi | 987 px | 600 px | 280 KB |
| xxxhdpi | 1316 px | 800 px | 402 KB |

O Android escolhe a versão da densidade do aparelho; no App Bundle, a Play entrega só essa versão. Para regenerar, usar os mesmos parâmetros a partir dos PNGs exportados do Figma.

## Revisão do Figma de 13/10/2026

Pente fino em todas as telas: estrutura comparada com a revisão anterior (`get_metadata`) e capturas de todos os frames comparadas pixel a pixel. Telas 01–06, 09–16, 18, 30a e 30c ficaram idênticas.

| Tela | Mudança no Figma | No app |
|---|---|---|
| Menu inferior (157:1183 e "… Menu Button") | Ícones react-icons (prédio, chama, ventilador). Selecionado = ícone preenchido roxo de 32 dp, sem fundo e sem rótulo | Implementado (`nova_home*`, `nova_alarm_*`, `nova_exhaust_*`) |
| 08a–08d Home | Cabeçalho 16 dp mais baixo; ícones prédio, sino (BiBell 22), usuário (AiOutlineUser 22); serviços com chama e ventilador de 24 dp | Implementado |
| 07 Erro | Aviso com "i" contornado de 22 dp | `NovaNotice` usa `NovaInfoMark` |
| 19–24 Exaustor | Prédio de 28 dp no cartão; faixa informativa #EEECF6 | Implementado; texto da faixa em #5F6675 para 4,5:1 |
| 27 Atenção, 30b Falha | Amarelo #BD9700 sobre #F8F4E3 | Fundo #F8F4E3; texto e ícones em #8A6A00 (o #BD9700 tem 2,8:1) |
| 29 Histórico | Ícones de 24 dp sem círculo: sirene, alerta, chave inglesa; linhas de 88 dp | Implementado (`nova_event_*`) |
| 17 Mensagens | Ícones das linhas | Sem efeito: a lista não tem dados (sem rota na API) |

Validação: lint, 39 testes unitários, `assembleDebug` e 9 testes de tela no J410G aprovados; capturas conferidas com os PNGs do Figma.
