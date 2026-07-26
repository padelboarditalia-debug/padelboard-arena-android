package com.example.padelboardarena.arena

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface ArenaSessionStore {
    fun readRefreshToken(): String?

    fun saveSession(
        session: ArenaAuthSession
    )

    fun clear()
}

object NoopArenaSessionStore : ArenaSessionStore {
    override fun readRefreshToken(): String? {
        return null
    }

    override fun saveSession(
        session: ArenaAuthSession
    ) = Unit

    override fun clear() = Unit
}

class AndroidKeystoreArenaSessionStore(
    context: Context
) : ArenaSessionStore {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    override fun readRefreshToken(): String? {
        val encryptedToken =
            preferences.getString(
                KEY_REFRESH_TOKEN,
                null
            ) ?: return null

        val iv =
            preferences.getString(
                KEY_REFRESH_TOKEN_IV,
                null
            ) ?: return null

        return try {
            decrypt(
                encryptedValue = encryptedToken,
                encodedIv = iv
            )
        } catch (_: Exception) {
            clear()
            null
        }
    }

    override fun saveSession(
        session: ArenaAuthSession
    ) {
        val encrypted =
            encrypt(
                session.refreshToken
            )

        preferences.edit()
            .putString(
                KEY_REFRESH_TOKEN,
                encrypted.value
            )
            .putString(
                KEY_REFRESH_TOKEN_IV,
                encrypted.iv
            )
            .apply()
    }

    override fun clear() {
        preferences.edit()
            .remove(
                KEY_REFRESH_TOKEN
            )
            .remove(
                KEY_REFRESH_TOKEN_IV
            )
            .apply()
    }

    private fun encrypt(
        value: String
    ): EncryptedValue {
        val cipher =
            Cipher.getInstance(
                TRANSFORMATION
            )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey()
        )

        val encryptedBytes =
            cipher.doFinal(
                value.toByteArray(
                    Charsets.UTF_8
                )
            )

        return EncryptedValue(
            value = encode(encryptedBytes),
            iv = encode(cipher.iv)
        )
    }

    private fun decrypt(
        encryptedValue: String,
        encodedIv: String
    ): String {
        val cipher =
            Cipher.getInstance(
                TRANSFORMATION
            )

        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(
                GCM_TAG_BITS,
                decode(encodedIv)
            )
        )

        return String(
            cipher.doFinal(
                decode(encryptedValue)
            ),
            Charsets.UTF_8
        )
    }

    private fun secretKey(): SecretKey {
        val keyStore =
            KeyStore.getInstance(
                ANDROID_KEYSTORE
            ).apply {
                load(null)
            }

        val existingKey =
            keyStore.getKey(
                KEY_ALIAS,
                null
            ) as? SecretKey

        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setRandomizedEncryptionRequired(true)
                .build()
        )

        return keyGenerator.generateKey()
    }

    private fun encode(
        bytes: ByteArray
    ): String {
        return Base64.encodeToString(
            bytes,
            Base64.NO_WRAP
        )
    }

    private fun decode(
        value: String
    ): ByteArray {
        return Base64.decode(
            value,
            Base64.NO_WRAP
        )
    }

    private data class EncryptedValue(
        val value: String,
        val iv: String
    )

    companion object {
        private const val PREFS_NAME =
            "padelboard_arena_secure_session"
        private const val KEY_REFRESH_TOKEN =
            "arena_refresh_token"
        private const val KEY_REFRESH_TOKEN_IV =
            "arena_refresh_token_iv"
        private const val KEY_ALIAS =
            "padelboard_arena_refresh_token_key"
        private const val ANDROID_KEYSTORE =
            "AndroidKeyStore"
        private const val TRANSFORMATION =
            "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS =
            128
    }
}