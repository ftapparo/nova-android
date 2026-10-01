---
name: physical-j4-testing
description: Padroniza a validação e depuração do Nova Residence no Samsung J4 físico conectado por USB, passando por VS Code, Gradle e ADB. Use ao instalar, executar testes, consultar logs, capturar evidências ou depurar o app no J4/J410.
---

# Testes no J4 físico

Use este fluxo para validações no Samsung J4 físico, modelo `SM_J410G`. Ele nunca seleciona emuladores e exige que o aparelho esteja conectado, autorizado para depuração USB e listado como `device` pelo ADB.

## Fluxo padrão

1. Abra a pasta do projeto no VS Code e use o terminal na raiz do repositório.
2. Confirme a conexão:

   ```powershell
   .\skills\physical-j4\scripts\test-j4.ps1 -Mode Status
   ```

3. Para instalar a versão que a pessoa usuária abrirá manualmente no telefone:

   ```powershell
   .\gradlew.bat installDebug
   ```

   Quando houver mais de um dispositivo conectado, use a tarefa do VS Code **Nova: instalar debug no J4 físico** ou o script abaixo, que fixa o `ANDROID_SERIAL` no J4:

   ```powershell
   .\skills\physical-j4\scripts\test-j4.ps1 -Mode Install
   ```

4. Abra **Nova Residence** no J4 e execute o roteiro de teste. Não use credenciais reais em logs ou capturas.
5. Para acompanhar falhas, abra uma segunda aba do terminal:

   ```powershell
   .\skills\physical-j4\scripts\test-j4.ps1 -Mode Logs
   ```

## Modos disponíveis

| Modo | Uso |
| --- | --- |
| `Status` | Confirma ADB, modelo e versão do Android. |
| `Install` | Executa `gradlew.bat installDebug` apenas no J4. |
| `Validate` | Executa `lint`, testes unitários, `assembleDebug` e instala o APK no J4. |
| `Instrumented` | Compila os APKs, instala ambos com ADB e executa o AndroidJUnitRunner no J4; falha se nenhum teste for aprovado. |
| `Launch` | Abre o aplicativo já instalado. |
| `Logs` | Exibe logcat filtrado pelo processo do aplicativo. Encerre com `Ctrl+C`. |
| `Screenshot` | Salva uma captura em `artifacts/physical-j4/`. |

Use as tarefas correspondentes em **Terminal → Run Task** no VS Code quando for mais prático.

## Segurança e limites

- Não limpe dados, desinstale o aplicativo ou altere configurações do aparelho sem pedido explícito.
- Não registre tokens, senhas, códigos de recuperação nem dados pessoais em logcat, evidências ou relatórios.
- Não acione comandos físicos do exaustor como parte da validação padrão. Faça isso somente quando a pessoa usuária pedir uma prova de hardware.
- Testes instrumentados podem substituir a instalação de depuração e encerrar a sessão aberta; avise antes de executá-los em um aparelho usado para demonstração.

## Falhas de conexão

Se `Status` apontar aparelho não autorizado, desbloqueie o J4 e aceite a chave RSA de depuração USB. Se houver mais de um J4 conectado, defina o serial desejado antes do comando:

```powershell
$env:NOVA_DEVICE_SERIAL = "1c4d02f6"
```

O script só aceita um dispositivo físico J4/J410 em estado `device`; ele falha de forma explícita para emulador, aparelho offline ou seleção ambígua.
