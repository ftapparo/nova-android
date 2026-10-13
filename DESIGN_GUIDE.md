# DESIGN GUIDE — Nova Residence

Guia de produto para o app Android. Objetivo: interface profissional, minimalista, legível e consistente, baseada em Material 3. **O Figma é referência visual revisável, não fonte da verdade.** Se desenho, implementação e uso real divergirem, validar a solução no aparelho e atualizar o Figma.

## 1. Princípios

1. Uma ação primária por região. Ações secundárias não devem parecer igualmente importantes.
2. Hierarquia por papel: barra, título da página, seção, corpo e legenda têm estilos distintos.
3. Reduzir espaços, superfícies e ornamentos desnecessários antes de diminuir texto essencial ou alvo de toque.
4. Usar dp para layout e sp para texto, com os valores do Figma. Em telas mais estreitas que o frame de 412 dp, a interface recebe densidade proporcional (decisão de 01/10/2026, ver seção 6); a escala de fonte do sistema continua valendo.
5. Respeitar fontScale, texto longo, teclado, insets, rotação e janelas redimensionáveis.
6. Cores, tipografia e formas são tokens compartilhados; telas não inventam estilos locais sem justificativa.

## 2. Tipografia

Usar fonte padrão Android/Material 3 nos textos funcionais. Reservar a fonte de marca para logotipo e raros destaques. Normal no corpo; Medium para títulos e ações; SemiBold para estado ou valor de destaque. Evitar negrito em blocos inteiros.

Estes são **tokens de produto**, uma personalização permitida pelo Material 3. Desde 12/10/2026 o Figma publica os estilos "Nova / Typography" (Headline Home 22/28 SemiBold, Screen Title 20/26 Medium, Section Title 15/20 SemiBold, Item Title 15/20 Medium, Body 14/20, Supporting 13/18, Caption 12/16); no código eles ficam em `NovaType` (`core/ui/NovaTheme.kt`) e as telas os usam pelo papel:

| Papel | Tamanho / linha | Peso | Uso |
|---|---:|---|---|
| Barra superior | 18/24 sp | Regular | Nome curto da tela, com chevron de voltar |
| Título principal | 20/26 sp | Medium | Uma vez por tela |
| Seção, serviço, linha principal | 16/22 sp | Medium | Agrupamentos e listas |
| Corpo e apoio | 14/20 sp | Normal | Instruções e descrições |
| Dado de destaque pontual | 16/24 sp | Normal ou Medium | Estado, unidade, valor |
| Campo e placeholder | 16/24 sp | Normal | Entrada de dados |
| Rótulo de campo | 12/16 sp | Medium | Legenda persistente |
| Botão | 14/20 sp | Medium | Ação |
| Navegação inferior | 11–12/16 sp | Medium | Rótulo curto |
| Caption/metadado | 12/16 sp | Normal | Informação suplementar |

- Não usar dois títulos de 22 sp na mesma região, como ocorre no login atual. A barra deve ser visualmente mais discreta que o título da página.
- Não aplicar estilos pelo nome da API sem conferir o papel: o projeto hoje redefine headlineMedium como 22 sp, embora o padrão Material 3 para esse papel seja maior.
- Limitar cada região a cerca de três níveis tipográficos perceptíveis. Corpo informativo normalmente não deve ficar abaixo de 14 sp; 12 sp fica para rótulos e metadados.
- Evitar tracking excessivo em texto corrido. Permitir quebra de linhas, aumento de fonte do sistema e crescimento vertical. Não truncar erro, instrução ou estado operacional crítico.

## 3. Espaçamento e alinhamento

Escala base: **4, 8, 12, 16, 24 e 32 dp**. Usar 20 dp como possível margem lateral, não como intervalo universal repetido.

| Relação | Valor inicial |
|---|---:|
| Ícone ↔ texto | 8–12 dp |
| Título ↔ apoio | 4–8 dp |
| Campo ↔ erro | 4–8 dp |
| Campos relacionados | 12–16 dp |
| Blocos da mesma seção | 16 dp |
| Seções distintas | 24 dp |
| Margem lateral do conteúdo, em todas as telas | 28 dp (Figma, 12/10/2026). Cartões, botões, listas e divisores começam e terminam nela; o traço do chevron de voltar também (o vetor tem respiro interno, por isso a barra usa 8 dp + alvo de 48 dp) |

Alinhar título, campos, divisores e ações à mesma grade. Evitar texto longo centralizado. Usar espaço ou divisor simples em vez de vários cartões aninhados. Espaço vazio deve ter finalidade; eliminar cadeias de Spacer(20.dp) em formulários.

## 4. Cor, forma e ícones

Paleta (variáveis `color/…` do Figma, 12/10/2026): roxo primário #4C1D95; cabeçalho da home e splash #2B0F44 (controles do cabeçalho #3A145F); fundo #F5F6FA (telas de serviço #F7F8FC); texto #1F2933 (títulos de estado #151B34); secundário #6B7280; divisor #E3E6ED. Estados: sucesso #237A4B/#E8F7ED, atenção #8A6A00/#F8F4E3, alarme #C23030/#FFECEC, erro #B42318. Faixa informativa #EEECF6 com texto #5F6675. O verde (#248E4E), o cinza (#667080) e o amarelo (#BD9700) do Figma ficam abaixo de 4,5:1 e foram escurecidos na mesma família; o texto #6B7280 sobre #EEECF6 também (4,1:1). Avaliar contraste por combinação; a existência da cor no projeto não garante aprovação de seu uso.

- Mapear cores a papéis de MaterialTheme.colorScheme. Evitar valores literais dispersos nas telas.
- Usar roxo forte em uma ação primária, seleção e pequenos acentos; não cobrir grandes áreas sem função.
- Preferir superfícies neutras e uma superfície principal contínua. Usar borda, sombra e elevação com moderação.
- Texto comum deve atingir contraste de 4,5:1; texto grande e gráficos essenciais, 3:1. Validar estados de erro, seleção e desabilitação.
- Campos: cantos sugeridos de 8–12 dp; cartões: 12–16 dp. Pílula pode ser usada no botão principal, mas não em toda ação.
- Ícones visuais coerentes de cerca de 20–24 dp. Alvos interativos de pelo menos 48 × 48 dp, incluindo o botão de voltar.

## 5. Componentes e composição

### Barra

Altura 56 dp, fundo da tela, título 18/24 sp Regular e chevron (Lucide `chevron-left`, 24 dp) em alvo de 48 dp. Aplicar o inset de status apenas uma vez.

### Navegação inferior (revisada em 12/10/2026)

Segue o componente **Navigation bar** do Figma (157:1183) e os conjuntos "… Menu Button": 80 dp de altura, fundo branco, borda superior de 1 dp #E3E6ED e sombra para cima (0 −2 8 rgba(64,52,80,.1)); padding inferior de 12 dp, três botões de 96 × 60 dp centralizados com 16 dp entre eles. Item normal: ícone de contorno 24 dp #6B7280 e rótulo 11/16 sp Medium (**Início · Alarme · Exaustor**; ícones prédio, chama e ventilador). Item selecionado (revisão de 13/10/2026): o mesmo ícone em versão preenchida #4C1D95 de 32 dp (prédio 28 × 32), sem fundo e sem rótulo visível (o rótulo vira descrição de acessibilidade). Configurações abre pelo ícone de perfil do cabeçalho da home, e Mensagens pelo sino. O menu aparece só nas abas; fica oculto em mensagens, configurações (09–16), histórico e detalhe da central (29–30), como no Figma.

### Barra de navegação do sistema

Em aparelhos com botões fixos (ex.: Galaxy J4), a barra dos botões do Android fica **preta com ícones claros** em todo o app, do splash nativo (tema) às telas (`enableEdgeToEdge(navigationBarStyle = SystemBarStyle.dark(BLACK))`), para integrar-se à moldura do aparelho. O menu inferior do app fica acima dela; os botões não são ocultados (modo imersivo atrapalharia o menu e o "voltar").

### Formulários

- Campo com texto de 16 sp, rótulo persistente e placeholder apenas como exemplo. Altura visual alvo **52–56 dp**. O OutlinedTextField do Material 3 tem mínimo próprio: não forçar 52 dp se isso cortar rótulo, cursor ou texto ampliado; validar o componente real.
- Botão principal com pelo menos 48 dp de altura e texto de 14 sp Medium. Largura total apenas dentro da coluna de conteúdo, que tem largura máxima em telas largas.
- “Esqueci minha senha” deve ser link/TextButton com área de toque adequada, não uma segunda pílula branca de largura total. Login Google deve parecer alternativa, sem disputar a ação principal.
- Mostrar erro junto ao campo e deixar a tela rolar com o teclado aberto. Evitar vários espaços de 20 dp entre botão, link, separador e ação social.

### Home, listas e configurações

Linhas interativas têm alvo mínimo de 48 dp; altura visual típica de 56–64 dp, crescente quando o texto precisar. Título 16 sp, apoio 12–14 sp. Usar divisão sutil; cartão apenas quando agrupar informação de fato.

### Exaustor

Destacar estado atual e ação disponível; unidade e explicação ficam em segundo plano. Evitar SpaceBetween que empurre estado e botão para extremos de uma tela alta. Em tela baixa, permitir rolagem e preservar a visibilidade de erro/aviso. Contador grande é exceção funcional, testada com fonte ampliada.

### Navegação inferior

Usar o componente Material 3 sem adicionar um segundo espaço para a barra do sistema. Altura de 80 dp é referência atual, sujeita a validação visual no J4; evitar altura fixa que conflite com insets. Rótulos explícitos de 11–12 sp, ícones de 20–24 dp. Testar “Exaustores” em 360 dp. Em janelas médias/expandidas, considerar navegação lateral.

## 6. Densidade e adaptação

**Decisão de produto (01/10/2026):** o app deve ficar proporcionalmente idêntico ao Figma. `NovaResidenceTheme` aplica densidade proporcional quando a janela é mais estreita que 412 dp (no Galaxy J4, 360 dp, um texto de 14 sp ocupa o mesmo espaço relativo do Figma). Janelas iguais ou maiores mantêm a densidade real e usam largura máxima nos conteúdos. A escala de fonte do sistema continua aplicada. O splash nativo usa `@dimen/nova_splash_logo_*` por largura (`values-w412dp`) para ter o mesmo tamanho do splash Compose. Adaptar composição conforme largura e altura úteis da janela, depois dos insets.

Classes oficiais de largura: compacta < 600 dp, média 600–839 dp, expandida ≥ 840 dp. O corte de 400 dp abaixo é regra específica deste produto, não nova classe Material 3.

| Espaço disponível | Regra de composição |
|---|---|
| < 412 dp de largura | Densidade proporcional ao Figma; mesmas margens e quebras do frame |
| 412–599 dp | Valores do Figma em dp/sp reais |
| ≥ 600 dp | Formulário centralizado com largura máxima aproximada de 480 dp; conteúdo legível; avaliar NavigationRail |
| Altura útil < 700 dp | Reduzir intervalos e imagens dispensáveis; rolar; não diminuir automaticamente fonte ou alvo |
| Teclado visível | Campo, erro e ação alcançáveis; IME sem sobreposição |
| FontScale ≥ 1,3 | Reflow e crescimento vertical; sem corte ou diminuição programática |

Implementação Compose: usar WindowSizeClass para mudança estrutural; BoxWithConstraints apenas para reação local ao espaço recebido. Preferir widthIn(max = 480.dp) em formulários largos, LazyColumn para listas variáveis e rolagem em formulários. Manter enableEdgeToEdge e definir um responsável pelo consumo de cada inset. Se introduzir Scaffold, aplicar innerPadding corretamente e conferir a interação com systemBarsPadding/imePadding e os insets internos dos componentes Material 3.

## 7. Figma e verificação

1. Atualizar estilos, variáveis, componentes, estados e Auto Layout do Figma com estes tokens. Não escalar frames inteiros.
2. Projetar e verificar ao menos 360 × 740 dp (Galaxy J4), 412 × 892 dp (frame atual) e largura ≥ 600 dp. Declarar se cada frame inclui barras do sistema.
3. Confrontar capturas do app em escala 1:1 de dp com o Figma revisado. Comparar hierarquia, quebras, áreas úteis e prioridade das ações; diferenças de plataforma são documentadas.
4. Testar 100% e 130% de fonte, teclado aberto, barras por gesto e por botões, rotação e janela redimensionada. Nenhuma informação ou ação essencial deve ser cortada.
5. Se o Figma mantiver títulos concorrentes, três botões igualmente fortes ou espaços repetitivos, alterar o Figma.

## 8. Aplicação no código

Ordem recomendada: (1) core/ui/NovaTheme.kt — estilos por papel, cores e formas; (2) core/ui/NovaComponents.kt — barra e variantes reais de ação; (3) feature/auth/AuthScreen.kt — hierarquia, espaços e largura; (4) feature/residence/ResidenceScreen.kt e ResidenceSettings.kt — linhas e navegação; (5) feature/exhaust/ExhaustScreen.kt — estado, ação e distribuição vertical. **Este documento não altera a implementação.**

Critérios de aceite: uma ação dominante por área; leitura e interação confortáveis no J4; nenhuma quebra nociva em 412 dp; alvos ≥ 48 dp; escalas de fonte 100%/130%; teclado e insets corretos; contraste verificado; capturas revisadas com o Figma atualizado.

## Referências oficiais

- [Material 3 em Compose — tema, tipografia, cores e formas](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Layouts adaptativos e classes de janela](https://developer.android.com/develop/adaptive-apps/guides/support-different-display-sizes)
- [Insets em Compose](https://developer.android.com/develop/ui/compose/system/insets)
- [Alvos de toque](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
- [Campos de texto Material](https://developer.android.com/develop/ui/compose/text/user-input)
