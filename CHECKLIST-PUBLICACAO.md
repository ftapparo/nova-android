# Checklist de publicação — Google Play

Primeira publicação do app Nova Residence. Conta de **organização** (MEI) com o e-mail `@flaviotapparo.com.br`, o que dispensa o teste fechado obrigatório de 12 testadores por 14 dias.

Legenda: `[x]` feito · `[~]` parcial ou a conferir · `[ ]` falta.

> **Situação em 26/10/2026:** a versão 1 ("1.0.0") passou na revisão do Google e foi **publicada na Google Play** em 26/10, por volta do meio-dia (Brasil). O app está disponível na loja para os moradores.

> **Situação em 20/10/2026:** versão 1 ("1.0.0") publicada no **teste interno** em 16/10 (disponível para testadores internos, ainda não revisada pelo Google; até a revisão a loja mostra o nome temporário "br.com.novaresidence (unreviewed)"). App instalado pela Play no celular e validado sem falhas. Em 20/10 a versão foi promovida para **produção** (Brasil) e enviada para revisão, com publicação gerenciada ativada: depois da aprovação, o app só fica público quando clicarmos em "Publicar alterações".

## App — funcionalidades

- [x] Login por e-mail, cadastro, recuperação por código e troca obrigatória de senha
- [x] Login com Google — validado no app instalado pelo teste interno (20/10)
- [x] Sessão criptografada (Android Keystore) e refresh serializado
- [x] Home com unidade, vínculo e bloqueios
- [x] Vínculo automático por CPF e celular (07b/07c)
- [x] Exaustor, com reinício, manutenção e localização, validado no aparelho
- [x] Central de Incêndio, somente leitura — alarme real visto no app e push `cie.alarm` validado pela rota de teste
- [x] Configurações, seleção de unidade e idioma (pt-BR, en, es)
- [x] Fidelidade ao Figma: densidade proporcional, menu inferior e splash
- [x] Barra dos botões do Android preta
- [x] Comunicados na home, lista e detalhe (`/messages`)
- [x] **Exclusão de conta no app** — Configurações → Privacidade → Solicitar exclusão da conta (13/10/2026)
- [x] Push nativo (alarme, exaustor e comunicados) — Firebase configurado e validado no J4
- [x] Política de Privacidade 1.3 em produção; "Segurança dos dados" conforme `docs/seguranca-dos-dados.md`

## API (v3)

- [x] Rotas de autenticação, residência, exaustor, central, comunicados, push e vínculo automático no ar
- [x] Campos `command` e `maintenance` do exaustor no ar
- [x] **`DELETE /auth/me`** em produção (13/10/2026)
- [x] **Página web de exclusão de conta** — `https://novaresidence.com.br/excluir-conta`

## Build de release

- [x] Clientes OAuth Android de release no Google Cloud ("Android - Upload" e "Android - Play"); login com Google conferido no app da Play (20/10)
- [x] Chave de upload (`.jks`) gerada, com backup, fora do Git, e assinatura configurada no Gradle (16/10)
- [x] Ícone do app com a marca (adaptativo e 512×512) (15/10)
- [x] `versionCode` 1 / `versionName` "1.0.0"
- [x] R8 (minify) ativado e release testado no J4 (16/10)
- [x] Geração do `.aab` assinado (`./gradlew bundleRelease`)
- [x] `targetSdk` 37, acima do mínimo exigido pela Play
- [x] Somente HTTPS
- [x] Lint e testes passando: `lintRelease` sem erros (9 avisos) e 81 testes unitários aprovados (20/10)

## Play Console

- [x] Número D-U-N-S do MEI
- [x] Conta de organização criada e identidade verificada
- [x] Política de privacidade em URL pública: `https://novaresidence.com.br/politica-de-privacidade`
- [x] Formulário "Segurança dos dados" com local exato, telefone e CPF (processamento efêmero) (16/10)
- [x] Permissões: localização só em primeiro plano (`ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION`), sem `ACCESS_BACKGROUND_LOCATION`; conferido em 20/10 que o Console não pediu declaração extra nem permissões confidenciais
- [x] Conferência antes da produção (20/10): configuração inicial concluída, "Precisa de atenção" vazia, 10 declarações concluídas, sem exigência de teste fechado, publicação gerenciada desativada
- [x] Classificação de conteúdo e público-alvo **18 anos ou mais**
- [x] Ficha da loja: textos em pt-BR, en-US e es-ES, ícone, imagem de destaque e 6 capturas (16/10)
- [x] Conta de teste para a revisão do Google: `app.test@novaresidence.com.br` (senha só no Play Console), vinculada à pessoa fictícia 23002 "CONTA TESTE" do ERP, unidade XYZ-100, Termo aceito. O exaustor dela é **simulado** na API (`EXHAUST_DEMO_UNITS=XYZ-100`): funciona como o real, sem relé e sem verificação de área. "Detalhes do login" salvo no Play Console com as instruções em inglês e sem uso das credenciais nos testes automáticos (o robô poderia excluir a conta). Se o revisor excluir a conta, recriar e vincular de novo antes de outra revisão.
  - Instruções aos revisores (Play Console → Acesso ao app, limite de 500 caracteres): "Toque em "Entrar" e use o e-mail e a senha acima (não use o login Google). Conta de morador de teste. Alarme: central de incêndio, só leitura. Exaustor: equipamento simulado; pode ligar e reiniciar o tempo à vontade. Ao ligar, permita a localização: a unidade de teste aceita de qualquer lugar. Comunicados: sino na tela inicial. Notificações: perfil > Notificações > Enviar notificação de teste. Excluir a conta apaga a conta de teste. Sem autenticação de dois fatores."
- [x] Teste interno: `.aab` enviado e versão 1 (1.0.0) publicada em 16/10; app instalado pela loja e validado no celular (20/10)
- [x] **Publicação gerenciada** ativada antes do envio (20/10): depois da aprovação, o app só entra na loja quando clicarmos em "Publicar alterações"
- [x] Publicação em produção: versão 1.0.0 promovida para o Brasil e enviada para revisão em 20/10 (12 mudanças, notas em pt-BR, en e es, sem erros; só o aviso de símbolos de depuração); aprovada pelo Google
- [x] "Publicar alterações" clicado: app **publicado na Google Play em 26/10/2026**

## Segurança

- [ ] Trocar a senha pessoal usada nos testes do aparelho

## Ordem sugerida

1. ~~Pedir o D-U-N-S e criar a conta na Play Console.~~
2. ~~Exclusão de conta: rota na API, tela no app e página web.~~
3. ~~Ícone, versão, R8, chave de upload e `.aab`.~~
4. ~~Clientes OAuth de release, política de privacidade, Segurança dos dados e ficha da loja.~~
5. ~~Teste interno pela loja~~ e, depois, produção. ← **publicado em 26/10/2026**
