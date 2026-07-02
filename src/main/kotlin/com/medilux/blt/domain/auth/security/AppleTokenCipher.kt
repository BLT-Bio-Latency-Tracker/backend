package com.medilux.blt.domain.auth.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Apple refresh token 저장용 AES-256-GCM 암복호화.
 */
@Component
class AppleTokenCipher(
    @Value("\${blt.apple.token-encryption-key:}")
    private val base64Key: String,
) {
    private val secretKey by lazy {
        require(base64Key.isNotBlank()) { "blt.apple.token-encryption-key 미설정 — Apple 토큰 암복호화 불가" }
        val keyBytes = Base64.getDecoder().decode(base64Key)
        require(keyBytes.size == KEY_SIZE_BYTES) { "AES 키는 base64 32바이트여야 합니다 (현재 ${keyBytes.size}B)" }
        SecretKeySpec(keyBytes, "AES")
    }
    private val secureRandom = SecureRandom()

    fun encrypt(plaintext: String): String {
        val iv = ByteArray(IV_SIZE_BYTES).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE_BITS, iv))
        }
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv + ciphertext)
    }

    fun decrypt(encoded: String): String {
        val bytes = Base64.getDecoder().decode(encoded)
        require(bytes.size > IV_SIZE_BYTES) { "암호문 길이가 유효하지 않습니다" }
        val iv = bytes.copyOfRange(0, IV_SIZE_BYTES)
        val ciphertext = bytes.copyOfRange(IV_SIZE_BYTES, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE_BITS, iv))
        }
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BYTES = 32
        const val IV_SIZE_BYTES = 12
        const val TAG_SIZE_BITS = 128
    }
}
