package com.medilux.blt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@DataJpaTest(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create",
    ],
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BltApplicationTests {
    @Autowired
    lateinit var entityManager: TestEntityManager

    @Test
    fun jpaEntityMappingsLoad() {
        val entityNames = entityManager.entityManager.metamodel.entities.map { it.name }

        assertThat(entityNames)
            .contains(
                "User",
                "ConsentLog",
                "UserDevice",
                "NotificationLog",
                "RefreshToken",
                "SleepRecord",
                "PvtSession",
                "BrainRoiScore",
                "Recommendation",
            )
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
    }
}
