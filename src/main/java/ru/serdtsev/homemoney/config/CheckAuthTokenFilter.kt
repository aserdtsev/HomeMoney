package ru.serdtsev.homemoney.config

import net.sf.ehcache.Cache
import net.sf.ehcache.CacheManager
import net.sf.ehcache.Element
import org.apache.catalina.connector.RequestFacade
import org.springframework.data.repository.findByIdOrNull
import ru.serdtsev.homemoney.common.HmException
import ru.serdtsev.homemoney.user.UserAuthTokenRepository
import java.util.*
import javax.servlet.*
import javax.servlet.annotation.WebFilter

@WebFilter("/api/*")
class CheckAuthTokenFilter(private val userAuthTokenRepo: UserAuthTokenRepository) : Filter {
    override fun init(filterConfig: FilterConfig) {
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val path = (request as RequestFacade).requestURI
        if (!path.contains("api/user/login")) {
            val cookies = listOf(*request.cookies)
            val userIdCookie = cookies.firstOrNull { "userId" == it.name }
            val authTokenCookie = cookies.firstOrNull { "authToken" == it.name }
            if (userIdCookie== null || authTokenCookie == null) return
            val userId = UUID.fromString(userIdCookie.value)
            val authToken = UUID.fromString(authTokenCookie.value)
            checkAuthToken(userId, authToken)
        }
        chain.doFilter(request, response)
    }

    override fun destroy() {}

    @Suppress("UNCHECKED_CAST")
    private fun checkAuthToken(userId: UUID, authToken: UUID) {
        val authTokensCache = authTokensCache
        val element = authTokensCache[userId]?.also {
            val authTokens = it.objectValue as MutableSet<UUID>
            if (authTokens.contains(authToken)) {
                return
            }
        } ?: Element(userId, HashSet<UUID>())
        val userAuthToken = userAuthTokenRepo.findByIdOrNull(authToken)
        if (userAuthToken == null || userAuthToken.userId != userId) {
            throw HmException(HmException.Code.WrongAuth, null)
        }
        val authTokens = element.objectValue as MutableSet<UUID>
        if (authTokens.isEmpty()) {
            authTokensCache.put(element)
        }
        authTokens.add(authToken)
    }

    private val authTokensCache: Cache
        get() = CacheManager.getInstance().getCache("authTokens")

}