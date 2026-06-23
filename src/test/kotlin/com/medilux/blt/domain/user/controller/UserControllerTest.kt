package com.medilux.blt.domain.user.controller

import com.medilux.blt.domain.auth.security.AuthUserPrincipal
import com.medilux.blt.domain.auth.security.JwtAuthenticationFilter
import com.medilux.blt.domain.user.dto.TermsHistoryItemResponse
import com.medilux.blt.domain.user.entity.ConsentType
import com.medilux.blt.domain.user.service.UserService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Instant

@WebMvcTest(
    controllers = [UserController::class],
    excludeFilters = [
        ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [JwtAuthenticationFilter::class]),
    ],
)
@Import(UserControllerTest.TestSecurityConfig::class)
class UserControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var userService: UserService

    @MockkBean
    lateinit var jpaMetamodelMappingContext: JpaMetamodelMappingContext

    private val userId = 1L

    private fun auth(id: Long = userId) = authentication(UsernamePasswordAuthenticationToken(AuthUserPrincipal(id), null, emptyList()))

    @Test
    fun `getTermsHistory returns 200 with history list`() {
        every { userService.getTermsHistory(userId) } returns listOf(
            TermsHistoryItemResponse(
                consentType = ConsentType.MARKETING,
                policyVersion = "1.0",
                agreed = false,
                agreedAt = Instant.parse("2026-05-17T09:30:22Z"),
            ),
        )

        mockMvc.get("/api/v1/users/me/terms/history") { with(auth()) }
            .andExpect {
                status { isOk() }
                jsonPath("$[0].consentType") { value("MARKETING") }
                jsonPath("$[0].policyVersion") { value("1.0") }
                jsonPath("$[0].agreed") { value(false) }
            }
    }

    @Test
    fun `getTermsHistory without authentication returns 401`() {
        mockMvc.get("/api/v1/users/me/terms/history")
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_INVALID_CREDENTIALS") }
            }
    }

    @TestConfiguration
    class TestSecurityConfig {
        @Bean
        fun testFilterChain(http: HttpSecurity): SecurityFilterChain = http
            .csrf { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .build()
    }
}
