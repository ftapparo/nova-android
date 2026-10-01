# Validação da autenticação e home inicial

Período: 28 e 29/09/2026.

## Implementação

- API v3 exclusivamente em HTTPS, configurada pelo BuildConfig em debug e release.
- Login por e-mail/senha, cadastro, recuperação por código de seis dígitos e troca obrigatória de senha.
- Google via Credential Manager com o web client ID confirmado no AGENTS.md, sem nonce.
- Sessão AES-GCM com chave no Android Keystore, excluída de backup e transferência.
- Refresh serializado, substituição atômica dos tokens, uma repetição após 401 e proteção contra respostas posteriores ao logout.
- Home com pessoa, unidades, vínculo e bloqueios reais; seleção preservada durante a sessão.
- Conta e logout em Configurações; preferências e serviços futuros desabilitados, sem consultar equipamentos.

## Verificações aprovadas

- `gradlew.bat lint`: sem erros. Há avisos sobre versões de dependências e recursos do template Android.
- `gradlew.bat testDebugUnitTest`: 19 testes, zero falhas, usando MockWebServer nas verificações HTTP.
- `gradlew.bat assembleDebug`: APK gerado em `app/build/outputs/apk/debug/app-debug.apk`.
- `gradlew.bat connectedDebugAndroidTest`: cinco testes aprovados, incluindo o teste original do template.
- Execução adicional de `ScreenTest` pelo AndroidJUnitRunner: quatro testes aprovados após os ajustes visuais finais.

Os testes cobrem envelopes inválidos, falhas de rede, erros 400/401/403/404/409/429/503/500, Retry-After, refresh concorrente, rotação de tokens, logout durante refresh e resposta de autenticação pendente. Também verificam requisições dos fluxos de autenticação, validações de campos, papéis, estados residenciais, seleção de unidades e bloqueios.

As verificações instrumentadas cobrem navegação, validação, carregamento, ações desabilitadas, seleção de unidades e leitura do armazenamento criptografado por uma nova instância. Nenhum teste automatizado usa a API de produção.

## Comparação com o Figma

Arquivo: https://www.figma.com/design/kixzSbydhXP4k7qTSKWJbK

Consultados pelo MCP: boas-vindas `4:29391`, login social `4:29431`, login da conta `4:29479`, cadastro `4:29564`, validação `4:29697`, recuperação `4:29743`, erro `4:29808`, configurações `4:30567`, seletor de unidades `4:30729` e home `6:3353`.

Logo e ícones foram baixados pelo MCP e incluídos localmente. Ícones SVG foram convertidos com Svg2Vector do Android; a fonte Inter inclui sua licença OFL. A troca obrigatória reutiliza os componentes inspecionados de formulário. As adaptações ao contrato incluem identificação somente por e-mail, cadastro sem CPF, recuperação com validade de 15 minutos e senha nova, remoção do comunicado demonstrativo e serviços futuros desabilitados.

Capturas de 18 estados estão disponíveis localmente em `artifacts/visual-final-412/screenshots/` (412 × 892 dp), fora do Git. Também foram verificados 480 × 853 dp e 360 × 640 dp com fonte a 130%, com rolagem para formulários e configurações. As capturas finais da tela menor estão em `artifacts/visual-final-360-fonte-130/`.

## Validação manual

- Login por e-mail no emulador: aprovado; nome e unidade carregados pela API.
- Encerramento e reabertura do processo: aprovado; sessão restaurada e vínculo consultado novamente.
- Logout no emulador: aprovado após corrigir o POST sem corpo JSON.

## Pendências de aceite

- Login Google real: a tentativa no emulador retornou falha do provedor e não produziu uma credencial. O fluxo precisa ser validado em um dispositivo com conta Google disponível.
- Smartphone de teste: não estava conectado ao ADB durante esta validação. Login por senha, Google, persistência e logout ainda precisam ser confirmados nele.
- Cadastro, recuperação e troca obrigatória foram validados com servidor simulado e interface; não alteramos a senha nem criamos contas reais durante os testes.

Credenciais da conta de teste não foram gravadas no código, na documentação ou nos testes.
