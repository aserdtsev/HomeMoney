package ru.serdtsev.homemoney.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import ru.serdtsev.homemoney.user.CustomUserDetailsService
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Configuration
@EnableWebSecurity
class WebSecurityConfig(val userDetailsService: CustomUserDetailsService) : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http.csrf().disable()
            .authorizeRequests().antMatchers("/api/**").hasRole("USER")
            .and().httpBasic().authenticationEntryPoint(NoPopupBasicAuthenticationEntryPoint())
            .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    }

    override fun configure(builder: AuthenticationManagerBuilder) {
        builder.userDetailsService(userDetailsService)
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