package ru.serdtsev.homemoney.user

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService: UserDetailsService {
    @Autowired
    lateinit var repository: UserRepo

    override fun loadUserByUsername(email: String): UserDetails {
        val user = repository.findByEmail(email) ?: throw UsernameNotFoundException("User not found")
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        return org.springframework.security.core.userdetails.User(user.email, user.pwdHash, authorities)
    }
}