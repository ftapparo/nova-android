# Checklist de publicação — Google Play

Primeira publicação do app Nova Residence. Conta de **organização** (MEI) com o e-mail `@flaviotapparo.com.br`, o que dispensa o teste fechado obrigatório de 12 testadores por 14 dias.

Legenda: `[x]` feito · `[~]` parcial ou a conferir · `[ ]` falta.

## App — funcionalidades

- [x] Login por e-mail, cadastro, recuperação por código e troca obrigatória de senha
- [x] Login com Google (validado no build de debug; depende do item de OAuth de release)
- [x] Sessão criptografada (Android Keystore) e refresh serializado
- [x] Home com unidade, vínculo e bloqueios
- [x] Exaustor (telas 14–20), com reinício e manutenção, validado no aparelho
- [~] Central de Incêndio (08c, 09–13), somente leitura — falta validar um alarme real ou simulado com a equipe
- [x] Configurações, seleção de unidade e idioma
- [x] Fidelidade ao Figma: densidade proporcional, menu inferior e splash
- [x] Barra dos botões do Android preta
- [~] Comunicados na home (08d/08e): só o estado vazio — opcional na 1ª versão (exige rota na API)
- [x] **Exclusão de conta no app** — Configurações → Privacidade → Solicitar exclusão da conta (13/10/2026)
- [~] Push nativo (alarme, exaustor e comunicados) implementado no app e na API — falta o projeto Firebase (`google-services.json`) e a validação no J4
- [~] Política de Privacidade 1.2 citando o token do aparelho e os demais dados das notificações (API, falta o deploy); "Segurança dos dados" com as respostas em `docs/seguranca-dos-dados.md`

## API (v3)

- [x] Rotas de autenticação, residência, exaustor e central no ar
- [x] Campos `command` e `maintenance` do exaustor no ar
- [ ] **`DELETE /auth/me`** — implementada em `src/v3` (13/10/2026); falta o deploy
- [ ] **Página web de exclusão de conta** — `/excluir-conta` implementada na API (13/10/2026); falta o deploy e informar a URL na Play

## Build de release

- [ ] Clientes OAuth Android de release no Google Cloud: SHA-1 da chave de upload e SHA-1 da Play App Signing (sem isso o login com Google falha no app baixado da loja)
- [ ] Chave de upload (`.jks`) gerada, com backup, fora do Git, e assinatura configurada no Gradle
- [ ] Ícone do app com a marca (adaptativo e 512×512) — hoje é o padrão do template
- [~] `versionCode` / `versionName` definidos (sugestão: 1 / 1.0.0)
- [ ] R8 (minify) ativado, regras de serialização ajustadas e release testado no J4
- [ ] Geração do `.aab` (`./gradlew bundleRelease`)
- [~] `targetSdk` conferido com o mínimo exigido pela Play
- [x] Somente HTTPS
- [~] Lint e testes passando — rodar também no build de release

## Play Console

- [ ] Número D-U-N-S do MEI (gratuito, leva alguns dias — pedir primeiro)
- [ ] Conta de organização criada (US$ 25) e identidade verificada
- [ ] Política de privacidade em URL pública: `/politica-de-privacidade` publicada pela API (13/10/2026); falta o deploy e informar a URL na Play
- [ ] Formulário "Segurança dos dados": preencher conforme `docs/seguranca-dos-dados.md` (e-mail, nome, IDs de usuário, outras ações, IDs do dispositivo, diagnóstico; nada compartilhado; criptografia em trânsito; exclusão disponível)
- [ ] Classificação de conteúdo e público-alvo
- [ ] Ficha da loja: descrição, capturas de tela do celular e imagem de destaque 1024×500
- [ ] Conta de teste para a revisão do Google: conta dedicada com unidade ativa, nunca a pessoal
- [ ] Teste interno: enviar o `.aab`, instalar pela loja e validar o login com Google
- [ ] Publicação em produção (revisão leva de dias a uma semana)

## Segurança

- [ ] Trocar a senha pessoal usada nos testes do aparelho

## Ordem sugerida

1. Pedir o D-U-N-S e criar a conta na Play Console.
2. Exclusão de conta: rota na API, tela no app e página web.
3. Ícone, versão, R8, chave de upload e `.aab`.
4. Clientes OAuth de release, política de privacidade, Segurança dos dados e ficha da loja.
5. Teste interno pela loja e, depois, produção.
