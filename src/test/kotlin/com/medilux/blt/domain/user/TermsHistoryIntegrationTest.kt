package com.medilux.blt.domain.user

import com.medilux.blt.domain.user.entity.AuthType
import com.medilux.blt.domain.user.entity.ConsentLog
import com.medilux.blt.domain.user.entity.ConsentType
import com.medilux.blt.domain.user.entity.User
import com.medilux.blt.domain.user.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.time.temporal.ChronoUnit

@Testcontainers
@DataJpaTest(properties = ["spring.jpa.hibernate.ddl-auto=create"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserService::class)
class TermsHistoryIntegrationTest {
    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var userService: UserService

    private fun persistUser(hash: String): User = entityManager.persistAndFlush(User(appleSubHash = hash, authType = AuthType.APPLE))

    private fun persistConsent(user: User, type: ConsentType, version: String, agreed: Boolean, agreedAt: Instant) {
        entityManager.persist(ConsentLog(user = user, consentType = type, policyVersion = version, agreed = agreed, agreedAt = agreedAt))
    }

    @Test
    fun `returns history latest-first with mapped fields`() {
        val user = persistUser("hash-terms")
        val base = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(100)
        persistConsent(user, ConsentType.TERMS_OF_SERVICE, "1.0", true, base)
        persistConsent(user, ConsentType.PRIVACY_POLICY, "1.0", true, base.plusSeconds(10))
        persistConsent(user, ConsentType.MARKETING, "1.0", false, base.plusSeconds(20)) // 최신
        entityManager.flush()
        entityManager.clear()

        val history = userService.getTermsHistory(user.id)

        assertThat(history).hasSize(3)
        // agreedAt 내림차순(최신 우선)
        assertThat(history.map { it.consentType }).containsExactly(
            ConsentType.MARKETING,
            ConsentType.PRIVACY_POLICY,
            ConsentType.TERMS_OF_SERVICE,
        )
        val marketing = history.first()
        assertThat(marketing.agreed).isFalse()
        assertThat(marketing.policyVersion).isEqualTo("1.0")
        assertThat(marketing.agreedAt).isEqualTo(base.plusSeconds(20))
    }

    @Test
    fun `append-only keeps multiple versions of same consent type`() {
        val user = persistUser("hash-versions")
        val base = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(100)
        persistConsent(user, ConsentType.PRIVACY_POLICY, "1.0", true, base)
        persistConsent(user, ConsentType.PRIVACY_POLICY, "2.0", true, base.plusSeconds(30)) // 재동의(신버전)
        entityManager.flush()
        entityManager.clear()

        val history = userService.getTermsHistory(user.id)

        assertThat(history).hasSize(2)
        assertThat(history.map { it.policyVersion }).containsExactly("2.0", "1.0") // 최신 버전 먼저
    }

    @Test
    fun `excludes other users and returns empty when none`() {
        val user = persistUser("hash-self")
        val other = persistUser("hash-other")
        persistConsent(other, ConsentType.TERMS_OF_SERVICE, "1.0", true, Instant.now())
        entityManager.flush()
        entityManager.clear()

        assertThat(userService.getTermsHistory(user.id)).isEmpty()
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
    }
}
