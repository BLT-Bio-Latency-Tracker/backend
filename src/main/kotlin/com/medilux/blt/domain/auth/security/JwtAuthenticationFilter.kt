package com.medilux.blt.domain.auth.security

import com.medilux.blt.global.exception.BltException
import com.medilux.blt.global.exception.ErrorCode
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    @Qualifier("handlerExceptionResolver")
    private val handlerExceptionResolver: HandlerExceptionResolver,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.servletPath
        return path.startsWith("/api/v1/auth/") ||
            path == "/v3/api-docs" ||
            path.startsWith("/v3/api-docs/") ||
            path.startsWith("/swagger-ui/") ||
            path == "/swagger-ui.html"
    }

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        try {
            val token = resolveBearerToken(request)
            if (token != null) {
                val principal = AuthUserPrincipal(jwtTokenProvider.getUserIdFromAccessToken(token))
                val authentication = UsernamePasswordAuthenticationToken(principal, null, emptyList()).apply {
                    details = WebAuthenticationDetailsSource().buildDetails(request)
                }
                SecurityContextHolder.getContext().authentication = authentication
            }

            filterChain.doFilter(request, response)
        } catch (ex: BltException) {
            SecurityContextHolder.clearContext()
            handlerExceptionResolver.resolveException(request, response, null, ex)
        }
    }

    private fun resolveBearerToken(request: HttpServletRequest): String? {
        val authorization = request.getHeader(AUTHORIZATION_HEADER) ?: return null

        if (!authorization.startsWith(BEARER_PREFIX)) {
            throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
        }

        val token = authorization.removePrefix(BEARER_PREFIX).trim()
        if (token.isBlank()) {
            throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
        }

        return token
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
