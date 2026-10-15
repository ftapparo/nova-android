package br.com.novaresidence.core.session

import br.com.novaresidence.core.ui.uiText
import br.com.novaresidence.core.ui.UiText
import br.com.novaresidence.R
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import br.com.novaresidence.core.network.NovaJson
import kotlinx.serialization.encodeToString
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptedSessionStore(context: Context) : SessionStore {
    private val file = AtomicFile(File(context.filesDir, "session.bin"))
    private val alias = "nova_residence_session"

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    override fun read(): SessionData? {
        if (!file.baseFile.exists()) return null
        return try {
            val bytes = file.openRead().use { it.readBytes() }
            require(bytes.size > 28)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            NovaJson.decodeFromString<SessionData>(cipher.doFinal(bytes.copyOfRange(12, bytes.size)).decodeToString())
        } catch (_: Exception) {
            // A restored file or invalidated Keystore key cannot restore an authenticated session.
            file.delete()
            throw SessionStorageException(uiText(R.string.session_restore_failed))
        }
    }

    override fun write(session: SessionData) {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val encrypted = cipher.iv + cipher.doFinal(NovaJson.encodeToString(session).encodeToByteArray())
            val stream = file.startWrite()
            try {
                stream.write(encrypted)
                file.finishWrite(stream)
            } catch (error: Exception) {
                file.failWrite(stream)
                throw error
            }
        } catch (_: Exception) {
            throw SessionStorageException(uiText(R.string.session_save_failed))
        }
    }

    override fun clear() = file.delete()
}

class SessionStorageException(val userMessage: UiText) : Exception("session storage")
