package com.chamsae.chaekchaek.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import com.chaekchaek.app.auth.GuestAuth

class RefreshTokenStore(context: Context) {
  private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  fun read(): String? {
    return readEncrypted(REFRESH_TOKEN_KEY).also { if (it == null) clear() }
  }

  fun write(refreshToken: String) {
    preferences.edit().putString(REFRESH_TOKEN_KEY, encrypt(refreshToken)).apply()
  }

  fun clear() {
    preferences.edit().remove(REFRESH_TOKEN_KEY).apply()
  }

  fun readGuest(): GuestAuth? {
    val token = readEncrypted(GUEST_TOKEN_KEY)
    val nickname = readEncrypted(GUEST_NICKNAME_KEY)
    val expiresAt = readEncrypted(GUEST_EXPIRES_AT_KEY)
    if (token == null || nickname == null || expiresAt == null) {
      clearGuest()
      return null
    }
    return GuestAuth(token, nickname, expiresAt)
  }

  fun writeGuest(guest: GuestAuth) {
    preferences.edit()
      .putString(GUEST_TOKEN_KEY, encrypt(guest.token))
      .putString(GUEST_NICKNAME_KEY, encrypt(guest.nickname))
      .putString(GUEST_EXPIRES_AT_KEY, encrypt(guest.expiresAt))
      .apply()
  }

  fun clearGuest() {
    preferences.edit()
      .remove(GUEST_TOKEN_KEY)
      .remove(GUEST_NICKNAME_KEY)
      .remove(GUEST_EXPIRES_AT_KEY)
      .apply()
  }

  private fun readEncrypted(key: String): String? = runCatching {
    val encrypted = preferences.getString(key, null) ?: return null
    val (iv, ciphertext) = encrypted.split(SEPARATOR, limit = 2).map(::decode)
    Cipher.getInstance(TRANSFORMATION).run {
      init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
      doFinal(ciphertext).decodeToString()
    }
  }.getOrNull()

  private fun encrypt(value: String): String {
    val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
    return "${encode(cipher.iv)}$SEPARATOR${encode(cipher.doFinal(value.encodeToByteArray()))}"
  }

  private fun secretKey(): SecretKey {
    val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

    return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
      init(
        KeyGenParameterSpec.Builder(
          KEY_ALIAS,
          KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
          .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
          .build(),
      )
      generateKey()
    }
  }

  private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

  private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

  private companion object {
    const val PREFERENCES_NAME = "auth_session"
    const val REFRESH_TOKEN_KEY = "refresh_token"
    const val GUEST_TOKEN_KEY = "guest_token"
    const val GUEST_NICKNAME_KEY = "guest_nickname"
    const val GUEST_EXPIRES_AT_KEY = "guest_expires_at"
    const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    const val KEY_ALIAS = "chaekchaek_refresh_token"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val TAG_LENGTH_BITS = 128
    const val SEPARATOR = ":"
  }
}
