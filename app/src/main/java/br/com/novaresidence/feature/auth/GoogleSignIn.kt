package br.com.novaresidence.feature.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import br.com.novaresidence.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

suspend fun googleIdToken(context: Context): String {
    val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
    val response = CredentialManager.create(context).getCredential(
        context, GetCredentialRequest.Builder().addCredentialOption(option).build(),
    )
    val credential = response.credential
    require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
    return GoogleIdTokenCredential.createFrom(credential.data).idToken
}
