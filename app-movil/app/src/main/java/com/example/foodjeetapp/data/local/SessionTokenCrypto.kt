package com.example.foodjeetapp.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Contrato de cifrado autenticado para secretos de sesión persistidos localmente.
 * Permite inyectar una implementación con Android Keystore en producción y una
 * implementación AES-GCM en memoria en pruebas unitarias JVM.
 */
interface SessionCipher {
    fun encrypt(plaintext: String): String
    fun decrypt(payload: String): String
}

/**
 * Excepción controlada cuando falla el descifrado o validación de integridad
 * de datos de sesión. Nunca incluye el secreto ni el texto cifrado en el mensaje.
 */
class SessionDecryptionException(
    message: String,
    cause: Throwable? = null
) : GeneralSecurityException(message, cause)

/**
 * Codificación versionada para cargas cifradas con AES-GCM:
 * `v1:<base64(iv_12_bytes)>:<base64(ciphertext_con_tag_128_bits)>`.
 */
internal object AesGcmPayloadCodec {
    const val PREFIX = "v1"
    const val IV_LENGTH_BYTES = 12
    const val TAG_LENGTH_BITS = 128
    private const val MIN_CIPHERTEXT_BYTES = TAG_LENGTH_BITS / 8

    fun encode(iv: ByteArray, ciphertext: ByteArray): String {
        require(iv.size == IV_LENGTH_BYTES) { "IV AES-GCM inválido" }
        require(ciphertext.size >= MIN_CIPHERTEXT_BYTES) { "Texto cifrado AES-GCM incompleto" }
        val encoder = Base64.getEncoder()
        return "$PREFIX:${encoder.encodeToString(iv)}:${encoder.encodeToString(ciphertext)}"
    }

    fun decode(payload: String): Pair<ByteArray, ByteArray> {
        val parts = payload.split(':')
        if (parts.size != 3 || parts[0] != PREFIX) {
            throw SessionDecryptionException("Formato de carga cifrada inválido")
        }
        return try {
            val decoder = Base64.getDecoder()
            val iv = decoder.decode(parts[1])
            val ciphertext = decoder.decode(parts[2])
            if (iv.size != IV_LENGTH_BYTES || ciphertext.size < MIN_CIPHERTEXT_BYTES) {
                throw SessionDecryptionException("Parámetros criptográficos corruptos")
            }
            iv to ciphertext
        } catch (e: SessionDecryptionException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw SessionDecryptionException("Codificación Base64 de sesión corrupta", e)
        }
    }
}

/**
 * Implementación de cifrado autenticado AES-256-GCM respaldada por Android Keystore.
 *
 * - La clave se genera y almacena dentro del Android Keystore (nunca en código fuente ni en DataStore).
 * - `setRandomizedEncryptionRequired(true)` garantiza un IV único de 12 bytes generado
 *   por el proveedor criptográfico en cada operación de cifrado.
 * - Ante invalidación o corrupción de clave, la clave puede regenerarse para nuevas sesiones
 *   sin exponer información sensible en logs o excepciones.
 */
class AndroidKeystoreAesGcmCipher(
    private val keyAlias: String = DEFAULT_KEY_ALIAS
) : SessionCipher {

    companion object {
        const val DEFAULT_KEY_ALIAS = "foodjet_session_aes_gcm_v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }

    private val lock = Any()

    override fun encrypt(plaintext: String): String = synchronized(lock) {
        try {
            encryptWithKey(getOrCreateSecretKey(), plaintext)
        } catch (_: KeyPermanentlyInvalidatedException) {
            deleteKeySilently()
            encryptWithKey(createSecretKey(), plaintext)
        } catch (e: GeneralSecurityException) {
            throw SessionDecryptionException("No se pudo cifrar el dato de sesión", e)
        }
    }

    override fun decrypt(payload: String): String = synchronized(lock) {
        val (iv, ciphertext) = AesGcmPayloadCodec.decode(payload)
        val secretKey = getExistingSecretKey()
            ?: throw SessionDecryptionException("Clave de sesión ausente en Android Keystore")
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(AesGcmPayloadCodec.TAG_LENGTH_BITS, iv))
            val plainBytes = cipher.doFinal(ciphertext)
            String(plainBytes, Charsets.UTF_8)
        } catch (e: KeyPermanentlyInvalidatedException) {
            deleteKeySilently()
            throw SessionDecryptionException("La clave de sesión fue invalidada por el sistema", e)
        } catch (e: GeneralSecurityException) {
            throw SessionDecryptionException("Error de integridad o descifrado de sesión", e)
        }
    }

    private fun encryptWithKey(secretKey: SecretKey, plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
            ?: throw SessionDecryptionException("Android Keystore no generó IV aleatorio")
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return AesGcmPayloadCodec.encode(iv, ciphertext)
    }

    private fun loadKeyStore(): KeyStore {
        return KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private fun getExistingSecretKey(): SecretKey? {
        return try {
            val keyStore = loadKeyStore()
            val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
            entry?.secretKey
        } catch (e: GeneralSecurityException) {
            throw SessionDecryptionException("No se pudo acceder a Android Keystore", e)
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        return getExistingSecretKey() ?: createSecretKey()
    }

    private fun createSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    internal fun deleteKeySilently() {
        try {
            val keyStore = loadKeyStore()
            if (keyStore.containsAlias(keyAlias)) {
                keyStore.deleteEntry(keyAlias)
            }
        } catch (_: Exception) {
            // Sin propagar ni registrar secretos.
        }
    }
}
