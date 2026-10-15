# Tradução do app (inglês e espanhol)

Os textos do app ficam em `app/src/main/res/values/strings.xml` (português, padrão), `values-en/` e `values-es/`, com as mesmas chaves. As traduções foram feitas e revisadas nas três línguas em 15/10/2026. Os textos do app são os destes recursos; o Figma é referência visual e permanece somente em português. A numeração atual coloca a seleção de idioma na tela 14.

## Como o idioma é escolhido

- Na primeira abertura depois de instalar, o app lê o idioma do Android: português, inglês ou espanhol (qualquer região) viram o idioma do app; qualquer outro vira inglês. O valor fica gravado.
- Daí em diante vale só a escolha em Configurações → Idioma (tela 14). Mudar o idioma do Android não muda o app.
- A API recebe o idioma em `Accept-Language` e no registro do aparelho (push). Ela traduz o `detail` dos erros, os rótulos da central e os avisos. Comunicados do síndico, nomes cadastrados na central e o Termo/Política continuam em português.

## Glossário

| Português | English | Español | Observação |
|---|---|---|---|
| Unidade | Unit | Unidad | |
| Apto. (apartamento) | Apt. | Depto. | "Apt. 124 - Tower A" |
| Torre | Tower | Torre | |
| Prumada / final | Line | Final | Só no push do exaustor ("Tower A, line 4") |
| Morador | Resident | Residente | |
| Síndico / administração | Management | Administración | |
| Condomínio | Condominium | Condominio | |
| Central de incêndio (CIE) | Fire panel | Central de incendios | |
| Alarme de incêndio | Fire alarm | Alarma de incendio | |
| Falha | Fault | Falla | |
| Supervisão | Supervision | Supervisión | |
| Bloqueio (dispositivo) | Isolation / isolated | Bloqueo / bloqueado | Termo usual em painéis de incêndio em inglês |
| Exaustor | Exhaust fan | Extractor | Exaustor da churrasqueira = barbecue exhaust fan / extractor de la parrilla |
| Comunicado | Announcement | Comunicado | |
| Informativos | News | Informativos | |
| Vínculo | Link | Vínculo | |
| Código Freedom | Freedom code | Código Freedom | Nome do sistema do condomínio, não traduzir "Freedom" |
| CPF | CPF | CPF | Documento brasileiro, sem tradução |
| Termo de Uso | Terms of Use | Términos de Uso | O documento oficial é só em português |
| Política de Privacidade | Privacy Policy | Política de Privacidad | Idem |

## Ao adicionar ou mudar um texto

1. Crie a chave em `values/strings.xml` com o texto em português.
2. Adicione a mesma chave em `values-en/` e `values-es/` (o lint falha sem elas).
3. Mantenha os mesmos parâmetros (`%1$s`, `%1$d`); para contagens use `<plurals>` (português e espanhol têm `one`, `many` e `other`).
4. Textos montados fora da tela usam `UiText` (`core/ui/UiText.kt`), nunca `String` pronta.
