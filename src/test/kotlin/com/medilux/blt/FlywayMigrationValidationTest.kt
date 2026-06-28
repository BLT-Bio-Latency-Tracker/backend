package com.medilux.blt

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Flyway V1__init.sql 이 현재 JPA 엔티티 매핑과 일치하는지 검증한다.
 *
 * Flyway로 빈 Postgres에 V1을 적용한 뒤 Hibernate `ddl-auto=validate`로 부팅 →
 * 스키마/엔티티 불일치가 있으면 컨텍스트 로드 단계에서 SchemaManagementException으로 실패한다.
 * (기존 @DataJpaTest들은 Flyway 자동구성을 포함하지 않아 영향 없음; 여기서는 명시적으로 임포트.)
 */
@Testcontainers
@DataJpaTest(
    properties = [
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
    ],
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration::class)
class FlywayMigrationValidationTest {
    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    fun `flyway V1 matches entity schema and context boots under validate`() {
        assertThat(entityManager).isNotNull
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
    }
}
