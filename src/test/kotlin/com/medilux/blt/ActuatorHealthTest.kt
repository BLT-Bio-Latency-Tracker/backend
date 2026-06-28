package com.medilux.blt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.HttpStatus
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * 배포 스모크: 전체 컨텍스트가 Flyway + Actuator + Sentry와 함께 부팅되고,
 * `/actuator/health`가 인증 없이 200 UP을 반환하는지(CloudFront/EC2 헬스체크용) 검증.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ActuatorHealthTest {
    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun `health endpoint is public and reports UP`() {
        val response = restTemplate.getForEntity("/actuator/health", String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("UP")
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
    }
}
