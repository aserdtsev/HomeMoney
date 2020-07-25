package ru.serdtsev.homemoney

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import mu.KotlinLogging
import net.sf.ehcache.Cache
import net.sf.ehcache.CacheManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import ru.serdtsev.homemoney.balancesheet.BalanceSheet.Companion.newInstance
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository
import ru.serdtsev.homemoney.common.HmException
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.user.*
import java.util.*

@RestController
@RequestMapping("/api/user")
class UserResource @Autowired constructor(private val userRepo: UserRepository,
        private val userAuthTokenRepo: UserAuthTokenRepository, private val balanceSheetRepo: BalanceSheetRepository
) {
    @RequestMapping("/balance-sheet-id")
    @Transactional(readOnly = true)
    fun getBalanceSheetId(@CookieValue(value = "userId", required = false) userId: UUID?): HmResponse {
        val response: HmResponse
        response = try {
            if (userId == null) {
                throw HmException(HmException.Code.UserIdCookieIsEmpty)
            }
            val user = userRepo.findByIdOrNull(userId)!!
            HmResponse.getOk(user.bsId)
        } catch (e: HmException) {
            HmResponse.getFail(e.code)
        }
        return response
    }

    @RequestMapping(value = ["/login"], method = [RequestMethod.POST])
    @Transactional
    fun login(
            @RequestParam("email") email: String,
            @RequestParam("pwd") pwd: String
    ): HmResponse {
        return try {
            log.info { "User login; email:$email" }
            val pwdHash = Hashing.sha1().hashString(pwd + email + SHARE_SALT, Charsets.UTF_8).toString()
            val user = userRepo.findByEmail(email) ?: run { createUserNBalanceSheet(email, pwdHash) }
            if (user.pwdHash != pwdHash) throw HmException(HmException.Code.WrongAuth)
            val authToken = UUID.randomUUID()
            saveUserAuthToken(authToken, user)
            val auth = Authentication(user.id, user.bsId, authToken)
            HmResponse.getOk(auth)
        } catch (e: HmException) {
            HmResponse.getFail(e.code)
        }
    }

    @RequestMapping(value = ["/logout"], method = [RequestMethod.POST])
    @Transactional
    fun logout(
            @CookieValue(value = "userId", required = false) userId: UUID,
            @CookieValue(value = "authToken", required = false) authToken: UUID
    ): HmResponse {
        val userAuthToken = userAuthTokenRepo.findByIdOrNull(authToken)!!
        if (userAuthToken.userId != userId) {
            throw HmException(HmException.Code.WrongUserId)
        }
        userAuthTokenRepo.delete(userAuthToken)
        val element = authTokensCache[userId]
        if (element != null) {
            val authTokens = element.objectValue as MutableSet<*>
            if (authTokens.contains(authToken)) {
                authTokens.remove(authToken)
                if (authTokens.isEmpty()) {
                    authTokensCache.remove(userId)
                }
            }
        }
        log.info { "User logout" }
        return HmResponse.getOk()
    }

    @RequestMapping(method = [RequestMethod.DELETE])
    @Transactional
    fun deleteBalanceSheet(@RequestParam id: UUID) {
        balanceSheetRepo.deleteById(id)
    }

    fun createUserNBalanceSheet(email: String, pwdHash: String): User {
        val bs = newInstance()
        balanceSheetRepo.save(bs)
        val user = User(UUID.randomUUID(), bs.id, email, pwdHash)
        userRepo.save(user)
        return user
    }

    private fun saveUserAuthToken(authToken: UUID, user: User) {
        val userAuthToken = UserAuthToken(authToken, user.id)
        userAuthTokenRepo.save(userAuthToken)
        log.info { "User is logged; userId:${user.id}." }
    }

    private val authTokensCache: Cache
        get() = CacheManager.getInstance().getCache("authTokens")

    companion object {
        private val log = KotlinLogging.logger {}
        private const val SHARE_SALT = "4301"
    }

}