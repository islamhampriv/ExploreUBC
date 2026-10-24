package com.exploreUBC.cpen321application.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.exploreUBC.cpen321application.data.AppConstants
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONException
import org.json.JSONObject

internal data class EncryptedSessionPayload(
    val initializationVector: ByteArray,
    val ciphertext: ByteArray,
)

internal interface SessionCipher {
    fun encrypt(plaintext: ByteArray): EncryptedSessionPayload

    fun decrypt(payload: EncryptedSessionPayload): ByteArray
}

internal class AndroidKeystoreSessionCipher(
    private val keyAlias: String = AppConstants.Session.DEFAULT_KEY_ALIAS,
) : SessionCipher {
    override fun encrypt(plaintext: ByteArray): EncryptedSessionPayload {
        val cipher = Cipher.getInstance(AppConstants.Session.CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return EncryptedSessionPayload(
            initializationVector = cipher.iv,
            ciphertext = cipher.doFinal(plaintext),
        )
    }

    override fun decrypt(payload: EncryptedSessionPayload): ByteArray {
        val keyStore = loadKeyStore()
        val key = keyStore.getKey(keyAlias, null) as? SecretKey
            ?: throw IllegalStateException("Session encryption key is missing.")
        val cipher = Cipher.getInstance(AppConstants.Session.CIPHER_TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(128, payload.initializationVector),
        )
        return cipher.doFinal(payload.ciphertext)
    }

    internal fun deleteKey() {
        val keyStore = loadKeyStore()
        if (keyStore.containsAlias(keyAlias)) {
            keyStore.deleteEntry(keyAlias)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = loadKeyStore()
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            AppConstants.Session.ANDROID_KEY_STORE,
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    private fun loadKeyStore(): KeyStore = KeyStore.getInstance(
        AppConstants.Session.ANDROID_KEY_STORE,
    ).apply {
        load(null)
    }
}

class SessionStore internal constructor(
    noBackupFilesDir: File,
    fileName: String,
    private val cipher: SessionCipher,
) {
    constructor(context: Context) : this(
        noBackupFilesDir = context.noBackupFilesDir,
        fileName = AppConstants.Session.DEFAULT_FILE_NAME,
        cipher = AndroidKeystoreSessionCipher(),
    )

    private val lock = Any()
    private val atomicFile = AtomicFile(File(noBackupFilesDir, fileName))

    internal val sessionFile: File
        get() = atomicFile.baseFile

    fun save(session: AuthenticatedSession) = synchronized(lock) {
        val plaintext = JSONObject()
            .put("token", session.token)
            .put("userId", session.userId)
            .put("expiresAtEpochMillis", session.expiresAtEpochMillis)
            .toString()
            .toByteArray(StandardCharsets.UTF_8)
        val encrypted = cipher.encrypt(plaintext)
        val encoded = JSONObject()
            .put("version", AppConstants.Session.FILE_VERSION)
            .put(
                "initializationVector",
                Base64.getEncoder().encodeToString(encrypted.initializationVector),
            )
            .put("ciphertext", Base64.getEncoder().encodeToString(encrypted.ciphertext))
            .toString()
            .toByteArray(StandardCharsets.UTF_8)

        val output = atomicFile.startWrite()
        try {
            output.write(encoded)
            atomicFile.finishWrite(output)
        } catch (error: IOException) {
            atomicFile.failWrite(output)
            throw error
        }
    }

    fun load(): AuthenticatedSession? {
        synchronized(lock) {
            if (!atomicFile.baseFile.exists()) return null

            return try {
                readSession()
            } catch (_: IOException) {
                discardInvalidSession()
            } catch (_: GeneralSecurityException) {
                discardInvalidSession()
            } catch (_: JSONException) {
                discardInvalidSession()
            } catch (_: IllegalArgumentException) {
                discardInvalidSession()
            } catch (_: IllegalStateException) {
                discardInvalidSession()
            } catch (_: SecurityException) {
                discardInvalidSession()
            }
        }
    }

    private fun readSession(): AuthenticatedSession {
        val encoded = atomicFile.openRead().use { input ->
            input.readBytes().toString(StandardCharsets.UTF_8)
        }
        val container = JSONObject(encoded)
        check(container.getInt("version") == AppConstants.Session.FILE_VERSION) {
            "Unsupported stored session version."
        }
        val encrypted = EncryptedSessionPayload(
            initializationVector = Base64.getDecoder().decode(
                container.getString("initializationVector"),
            ),
            ciphertext = Base64.getDecoder().decode(container.getString("ciphertext")),
        )
        val plaintext = cipher.decrypt(encrypted).toString(StandardCharsets.UTF_8)
        val session = JSONObject(plaintext)
        return AuthenticatedSession(
            token = session.getString("token"),
            userId = session.getString("userId"),
            expiresAtEpochMillis = session.getLong("expiresAtEpochMillis"),
        )
    }

    private fun discardInvalidSession(): AuthenticatedSession? {
        atomicFile.delete()
        return null
    }

    fun clear() = synchronized(lock) {
        atomicFile.delete()
    }
}
