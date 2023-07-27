package ru.serdtsev.homemoney.infra.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain


@Configuration
@EnableWebSecurity
class WebSecurityConfig {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain? {
        http.csrf().disable()
            .authorizeHttpRequests().requestMatchers("/api/**").hasRole("USER")
            .anyRequest().permitAll()
            .and().httpBasic().authenticationEntryPoint(NoPopupBasicAuthenticationEntryPoint())
            .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        return http.build()
    }

    @Bean
    fun encoder(): BCryptPasswordEncoder = BCryptPasswordEncoder()

    class NoPopupBasicAuthenticationEntryPoint : AuthenticationEntryPoint {
        override fun commence(request: HttpServletRequest, response: HttpServletResponse,
            authException: AuthenticationException) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.message)
        }
    }
}