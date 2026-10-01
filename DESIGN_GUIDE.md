# DESIGN GUIDE — Nova Residence

Guia de produto para o app Android. Objetivo: interface profissional, minimalista, legível e consistente, baseada em Material 3. **O Figma é referência visual revisável, não fonte da verdade.** Se desenho, implementação e uso real divergirem, validar a solução no aparelho e atualizar o Figma.

## 1. Princípios

1. Uma ação primária por região. Ações secundárias não devem parecer igualmente importantes.
2. Hierarquia por papel: barra, título da página, seção, corpo e legenda têm estilos distintos.
3. Reduzir espaços, superfícies e ornamentos desnecessários antes de diminuir texto essencial ou alvo de toque.
4. Usar dp para layout e sp para texto. Nunca escalar a interface inteira por densidade, modelo ou tamanho do frame.
5. Respeitar fontScale, texto longo, teclado, insets, rotação e janelas redimensionáveis.
6. Cores, tipografia e formas são tokens compartilhados; telas não inventam estilos locais sem justificativa.

## 2. Tipografia

Usar fonte padrão Android/Material 3 nos textos funcionais. Reservar a fonte de marca para logotipo e raros destaques. Normal no corpo; Medium para títulos e ações; SemiBold para estado ou valor de destaque. Evitar negrito em blocos inteiros.

Estes são **tokens de produto propostos**, uma personalização permitida pelo Material 3, e devem ser avaliados nas capturas do app:

| Papel | Tamanho / linha | Peso | Uso |
|---|---:|---|---|
| Barra superior | 18/24 sp | Medium | Nome curto da tela |
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
| Margem em telefone 360–399 dp | 16 dp |
| Margem em telefone 400–599 dp | 20–24 dp |

Alinhar título, campos, divisores e ações à mesma grade. Evitar texto longo centralizado. Usar espaço ou divisor simples em vez de vários cartões aninhados. Espaço vazio deve ter finalidade; eliminar cadeias de Spacer(20.dp) em formulários.

## 4. Cor, forma e ícones

Paleta existente como ponto de partida: roxo primário #45207A; roxo escuro #2E0F44; fundo #F5F6FA; texto #1F2933; secundário #6B7280; divisor #E3E6ED. Avaliar contraste por combinação; a existência da cor no projeto não garante aprovação de seu uso.

- Mapear cores a papéis de MaterialTheme.colorScheme. Evitar valores literais dispersos nas telas.
- Usar roxo forte em uma ação primária, seleção e pequenos acentos; não cobrir grandes áreas sem função.
- Preferir superfícies neutras e uma superfície principal contínua. Usar borda, sombra e elevação com moderação.
- Texto comum deve atingir contraste de 4,5:1; texto grande e gráficos essenciais, 3:1. Validar estados de erro, seleção e desabilitação.
- Campos: cantos sugeridos de 8–12 dp; cartões: 12–16 dp. Pílula pode ser usada no botão principal, mas não em toda ação.
- Ícones visuais coerentes de cerca de 20–24 dp. Alvos interativos de pelo menos 48 × 48 dp, incluindo o botão de voltar.

## 5. Componentes e composição

### Barra

Altura visual alvo 56–64 dp; título 18/24 sp Medium. Aplicar o inset de status apenas uma vez. O ícone de voltar atual de 5 × 10 dp deve ser revisto visualmente, preservando o alvo de 48 dp.

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

dp já compensa densidade física; sp também responde à escala de fonte. **Não usar LocalDensity para neutralizar fontScale nem reduzir tudo por fator global.** Adaptar composição conforme largura e altura úteis da janela, depois dos insets.

Classes oficiais de largura: compacta < 600 dp, média 600–839 dp, expandida ≥ 840 dp. O corte de 400 dp abaixo é regra específica deste produto, não nova classe Material 3.

| Espaço disponível | Regra de composição |
|---|---|
| 360–399 dp de largura | Margem 16 dp; ações empilhadas; quebra natural de texto |
| 400–599 dp | Margem 20–24 dp; mesma escala tipográfica |
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
