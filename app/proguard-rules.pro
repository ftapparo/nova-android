# Regras do R8 para o release. As bibliotecas (kotlinx.serialization, OkHttp, Firebase, Play Services)
# trazem as próprias regras; aqui fica só o que o app precisa acrescentar.

# Linha e arquivo nos rastros de pilha, para diagnosticar falhas do release.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Credential Manager (login com Google): o provedor do Play Services é carregado por reflexão.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
