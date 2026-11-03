package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurityManager(context: Context) {

  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences("docker_remote_security_prefs", Context.MODE_PRIVATE)

  private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
    load(null)
  }

  init {
    ensureMasterKey()
  }

  private fun ensureMasterKey() {
    if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
      val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
      val spec = KeyGenParameterSpec.Builder(
        MASTER_KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .setRandomizedEncryptionRequired(true)
        .build()

      keyGenerator.init(spec)
      keyGenerator.generateKey()
    }
  }

  private fun getMasterKey(): SecretKey {
    return (keyStore.getEntry(MASTER_KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
  }

  /**
   * Encrypts plaintext using hardware-backed AES-GCM 256.
   * Format: ENC:<base64(12-byte IV + ciphertext + tag)>
   */
  fun encrypt(plaintext: String): String {
    if (plaintext.isBlank()) return ""
    try {
      val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
      cipher.init(Cipher.ENCRYPT_MODE, getMasterKey())
      val iv = cipher.iv
      val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

      val combined = ByteArray(iv.size + ciphertext.size)
      System.arraycopy(iv, 0, combined, 0, iv.size)
      System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

      return PREFIX_ENCRYPTED + Base64.encodeToString(combined, Base64.NO_WRAP)
    } catch (e: Exception) {
      e.printStackTrace()
      return plaintext
    }
  }

  /**
   * Decrypts ciphertext with the master key.
   * If not prefixed with ENC:, assumes legacy unencrypted and returns as-is.
   */
  fun decrypt(encrypted: String): String {
    if (encrypted.isBlank()) return ""
    if (!encrypted.startsWith(PREFIX_ENCRYPTED)) {
      return encrypted // Legacy unencrypted string
    }

    try {
      val encoded = encrypted.removePrefix(PREFIX_ENCRYPTED)
      val combined = Base64.decode(encoded, Base64.NO_WRAP)
      if (combined.size < GCM_IV_LENGTH) return ""

      val iv = ByteArray(GCM_IV_LENGTH)
      val ciphertext = ByteArray(combined.size - GCM_IV_LENGTH)
      System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
      System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.size)

      val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
      val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
      cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), spec)

      val decryptedBytes = cipher.doFinal(ciphertext)
      return String(decryptedBytes, Charsets.UTF_8)
    } catch (e: Exception) {
      e.printStackTrace()
      return ""
    }
  }

  // --- PIN Code Management ---

  fun isPinSet(): Boolean {
    return prefs.getBoolean(KEY_IS_PIN_SET, false) && prefs.getString(KEY_PIN_HASH, null) != null
  }

  fun setPin(pin: String) {
    val salt = generateSalt()
    val hash = hashPin(pin, salt)
    prefs.edit()
      .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
      .putString(KEY_PIN_HASH, hash)
      .putBoolean(KEY_IS_PIN_SET, true)
      .apply()
  }

  fun verifyPin(pin: String): Boolean {
    val saltString = prefs.getString(KEY_PIN_SALT, null) ?: return false
    val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
    val salt = Base64.decode(saltString, Base64.NO_WRAP)
    val inputHash = hashPin(pin, salt)
    return inputHash == savedHash
  }

  // --- Biometric Authentication Management ---

  fun isBiometricEnabled(): Boolean {
    return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
  }

  fun setBiometricEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
  }

  fun isBiometricSupported(context: Context): Boolean {
    val biometricManager = BiometricManager.from(context)
    val canAuth = biometricManager.canAuthenticate(
      BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
    )
    return canAuth == BiometricManager.BIOMETRIC_SUCCESS
  }

  fun authenticateBiometric(
    activity: FragmentActivity,
    title: String = "Biometric Authentication",
    subtitle: String = "Scan fingerprint to access Server Control",
    onSuccess: () -> Unit,
    onError: (String) -> Unit = {}
  ) {
    val executor = ContextCompat.getMainExecutor(activity)
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
      .setTitle(title)
      .setSubtitle(subtitle)
      .setNegativeButtonText("Use PIN")
      .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
      .build()

    val biometricPrompt = BiometricPrompt(
      activity,
      executor,
      object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
          super.onAuthenticationSucceeded(result)
          onSuccess()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
          super.onAuthenticationError(errorCode, errString)
          if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
            onError(errString.toString())
          }
        }
      }
    )
    biometricPrompt.authenticate(promptInfo)
  }

  private fun hashPin(pin: String, salt: ByteArray): String {
    val md = MessageDigest.getInstance("SHA-256")
    md.update(salt)
    val digest = md.digest(pin.toByteArray(Charsets.UTF_8))
    return Base64.encodeToString(digest, Base64.NO_WRAP)
  }

  private fun generateSalt(): ByteArray {
    val random = SecureRandom()
    val salt = ByteArray(16)
    random.nextBytes(salt)
    return salt
  }

  companion object {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "DockerRemoteMasterKey"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val PREFIX_ENCRYPTED = "ENC:"

    private const val KEY_IS_PIN_SET = "is_pin_set"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_PIN_SALT = "pin_salt"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
  }
}
