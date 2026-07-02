package com.medilux.blt.domain.auth.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Base64

class AppleTokenCipherTest {
    private val cipher = AppleTokenCipher(Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() }))

    @Test
    fun `encrypt then decrypt round-trips the plaintext`() {
        val plaintext = "apple-refresh-token-abc.123"
        assertEquals(plaintext, cipher.decrypt(cipher.encrypt(plaintext)))
    }

    @Test
    fun `same plaintext encrypts to different ciphertext (random IV)`() {
        val plaintext = "same-token"
        assertNotEquals(cipher.encrypt(plaintext), cipher.encrypt(plaintext))
    }

    @Test
    fun `tampered ciphertext fails authentication`() {
        val encoded = cipher.encrypt("token")
        val tampered = encoded.dropLast(2) + if (encoded.last() == 'A') "BB" else "AA"
        assertThrows(Exception::class.java) { cipher.decrypt(tampered) }
    }

    @Test
    fun `missing key rejects encryption`() {
        assertThrows(IllegalArgumentException::class.java) { AppleTokenCipher("").encrypt("x") }
    }
}
