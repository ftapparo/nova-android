# Play Console — formulário "Segurança dos dados"

Respostas para o formulário da Google Play (Play Console → Política do app → Segurança dos dados), conforme o que o app coleta em 16/10/2026 e a Política de Privacidade 1.3. Revisar sempre que um recurso novo enviar dados do aparelho. A localização para ligar o exaustor (STATUS §7.2) foi implementada em 14/10/2026 e entra como **Local → Local exato**; o rascunho salvo no Play Console antes disso precisa ser atualizado. O vínculo automático por CPF e celular (STATUS §3.2, 16/10/2026) entra como **Número de telefone** e **Outras informações**, ambos com processamento efêmero: **atualizar o formulário no Play Console**.

Pela definição da Google, "coletar" é transmitir dados do aparelho para fora dele. Os dados que vêm do cadastro do condomínio e só são exibidos no app (CPF mascarado, unidades, vínculo) não contam como coleta do app.

## Visão geral

| Pergunta | Resposta |
|---|---|
| O app coleta ou compartilha algum dos tipos de dados exigidos? | **Sim** |
| Todos os dados coletados são criptografados em trânsito? | **Sim** (somente HTTPS/WSS) |
| Você oferece uma forma de os usuários solicitarem a exclusão dos dados? | **Sim**: no app (Configurações → Privacidade → Solicitar exclusão da conta) e na página `https://novaresidence.com.br/excluir-conta` |
| URL da política de privacidade | `https://novaresidence.com.br/politica-de-privacidade` |
| Dados compartilhados com terceiros | **Nenhum**. O Google (FCM e login com Google) atua como prestador de serviço em nome do condomínio, o que a Google não considera compartilhamento |
| Processamento efêmero | Sim para o local exato (comparado na hora do comando e descartado) e para o número de telefone e o CPF (comparados com o cadastro na vinculação e descartados); não nos demais tipos |
| Métodos de criação de conta | Usuário e senha; OAuth (login com Google) |
| Excluir dados sem excluir a conta (opcional) | Não oferecido: só a exclusão da conta inteira |

**Situação (14/10/2026):** formulário preenchido e salvo como rascunho no Play Console, conferido ao reabrir. Para concluir e enviar para revisão, a Play exige antes a etapa **Público-alvo e conteúdo**: marcar só **18 anos ou mais** (app de moradores e equipe com vínculo validado pela administração; faixas abaixo de 18 entram no programa Families, com exigências próprias).

## Tipos de dados coletados

| Categoria → tipo | Coletado | Obrigatório? | Finalidades | Origem no app |
|---|---|---|---|---|
| Informações pessoais → **Endereço de e-mail** | Sim | Obrigatório | Funcionalidade do app; Gerenciamento da conta | Login, cadastro e recuperação de senha |
| Informações pessoais → **Nome** | Sim | Opcional (só com login Google) | Gerenciamento da conta | Token do login com Google |
| Informações pessoais → **Número de telefone** | Sim, processamento efêmero | Obrigatório para o morador vincular a conta (a equipe não envia) | Gerenciamento da conta; Prevenção de fraudes, segurança e conformidade | Celular com DDI digitado na tela "Vincular conta" (07b) e enviado no `POST /residence/link/self/check`; a API só compara com o cadastro do condomínio e não guarda (Política 3.3-A e 5.5) |
| Informações pessoais → **Outras informações** | Sim, processamento efêmero | Obrigatório para o morador vincular a conta | Gerenciamento da conta; Prevenção de fraudes, segurança e conformidade | CPF digitado na mesma tela, com o mesmo tratamento (comparado e descartado) |
| Informações pessoais → **IDs de usuário** | Sim | Obrigatório | Funcionalidade do app; Gerenciamento da conta; Prevenção de fraudes, segurança e conformidade | Identificador da conta enviado em cada requisição autenticada |
| Atividade no app → **Outras ações** | Sim | Obrigatório | Funcionalidade do app; Prevenção de fraudes, segurança e conformidade | Comandos do exaustor (ligar, reiniciar, desligar), leitura de comunicados, aceite do Termo, preferências de notificação; registro de atividades na API (ações de escrita com conta, resultado, IP e versão do app, 12 meses — Política 3.5) |
| IDs do dispositivo ou outros IDs → **IDs do dispositivo ou outros IDs** | Sim | Obrigatório | Funcionalidade do app | Token do Firebase Cloud Messaging e identificador de instalação gerado pelo app (`/notifications/devices/android`) |
| Local → **Local exato** | Sim | Obrigatório para o morador ligar ou reiniciar o exaustor (a equipe não envia) | Funcionalidade do app; Prevenção de fraudes, segurança e conformidade | Latitude, longitude, precisão e indicação de localização simulada, lidas no toque e enviadas no `POST /exhausts/{id}/on`; a API só compara com a área do condomínio e não guarda as coordenadas (Política 8-A). Sem localização em segundo plano |
| Informações e desempenho do app → **Diagnóstico** | Sim | Obrigatório | Funcionalidade do app | Modelo do aparelho, versão do Android e versão do app enviados no registro para notificações |

Não coletados: local aproximado, informações financeiras, saúde, mensagens, fotos e vídeos, áudio, arquivos, agenda, contatos, histórico de navegação, apps instalados, registros de falhas (o app não tem SDK de crash nem analytics).

## Quando atualizar

- Qualquer SDK novo (analytics, crash, publicidade): reavaliar antes da publicação.
- App iOS: o formulário da App Store ("Privacy Nutrition Label") segue as mesmas categorias.
