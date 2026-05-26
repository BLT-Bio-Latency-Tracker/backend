package com.medilux.blt.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.medilux.blt.domain.auth.security.JwtAuthenticationFilter
import com.medilux.blt.global.exception.ErrorCode
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    @Value("\${blt.cors.allowed-origin-patterns}")
    private val allowedOriginPatternsProperty: String,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val objectMapper: ObjectMapper,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }
        .cors { }
        .httpBasic { it.disable() }
        .formLogin { it.disable() }
        .logout { it.disable() }
        .sessionManagement { sessionManagement ->
            sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        }.exceptionHandling { exceptionHandling ->
            exceptionHandling.authenticationEntryPoint { _, response, _ ->
                writeProblemDetail(response, ErrorCode.UNAUTHORIZED)
            }
            exceptionHandling.accessDeniedHandler { _, response, _ ->
                writeProblemDetail(response, ErrorCode.FORBIDDEN)
            }
        }.authorizeHttpRequests { authorize ->
            authorize
                .requestMatchers(
                    "/api/v1/auth/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                ).permitAll()
                .anyRequest()
                .authenticated()
        }
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .build()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                allowedOriginPatterns =
                    allowedOriginPatternsProperty
                        .split(",")
                        .map { originPattern -> originPattern.trim() }
                        .filter { originPattern -> originPattern.isNotEmpty() }
                allowedMethods =
                    listOf(
                        HttpMethod.GET.name(),
                        HttpMethod.POST.name(),
                        HttpMethod.PUT.name(),
                        HttpMethod.PATCH.name(),
                        HttpMethod.DELETE.name(),
                        HttpMethod.OPTIONS.name(),
                    )
                allowedHeaders = listOf("*")
                exposedHeaders = listOf(HttpHeaders.AUTHORIZATION)
                allowCredentials = false
                maxAge = 3600
            }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    private fun writeProblemDetail(response: HttpServletResponse, errorCode: ErrorCode) {
        val problemDetail = ProblemDetail.forStatusAndDetail(errorCode.status, errorCode.message)
        problemDetail.title = errorCode.status.reasonPhrase
        problemDetail.setProperty("errorCode", errorCode.code)

        response.status = errorCode.status.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        objectMapper.writeValue(response.outputStream, problemDetail)
    }
}
