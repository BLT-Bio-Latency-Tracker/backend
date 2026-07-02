package com.medilux.blt.domain.user.service

import com.medilux.blt.domain.auth.repository.RefreshTokenRepository
import com.medilux.blt.domain.auth.security.AppleTokenCipher
import com.medilux.blt.domain.auth.security.AppleTokenClient
import com.medilux.blt.domain.user.entity.AuthType
import com.medilux.blt.domain.user.entity.User
import com.medilux.blt.domain.user.entity.UserStatus
import com.medilux.blt.domain.user.repository.ConsentLogRepository
import com.medilux.blt.domain.user.repository.UserDeviceRepository
import com.medilux.blt.domain.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.Base64
import java.util.Optional

class UserServiceWithdrawTest {
    private val userRepository = mockk<UserRepository>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>(relaxed = true)
    private val userDeviceRepository = mockk<UserDeviceRepository>(relaxed = true)
    private val consentLogRepository = mockk<ConsentLogRepository>(relaxed = true)
    private val appleTokenClient = mockk<AppleTokenClient>()
    private val cipher = AppleTokenCipher(Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() }))
    private val service = UserService(
        userRepository,
        refreshTokenRepository,
        userDeviceRepository,
        consentLogRepository,
        appleTokenClient,
        cipher,
    )

    @Test
    fun `withdraw revokes the decrypted apple token and clears it`() {
        val user = User(appleSubHash = "h", authType = AuthType.APPLE).apply {
            appleRefreshToken = cipher.encrypt("apple-rt")
        }
        every { userRepository.findById(1L) } returns Optional.of(user)
        every { appleTokenClient.revokeRefreshToken("apple-rt") } returns true

        service.withdraw(1L)

        verify { appleTokenClient.revokeRefreshToken("apple-rt") }
        assertNull(user.appleRefreshToken)
        assertEquals(UserStatus.WITHDRAW_PENDING, user.status)
    }

    @Test
    fun `withdraw does not call revoke when no apple token is stored`() {
        val user = User(appleSubHash = "h", authType = AuthType.APPLE)
        every { userRepository.findById(2L) } returns Optional.of(user)

        service.withdraw(2L)

        verify(exactly = 0) { appleTokenClient.revokeRefreshToken(any()) }
    }
}
