package ru.serdtsev.homemoney.user

import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpHeaders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import ru.serdtsev.homemoney.balancesheet.BalanceSheet.Companion.newInstance
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository
import ru.serdtsev.homemoney.common.HmException
import ru.serdtsev.homemoney.common.HmResponse
import java.util.*

@RestController
@RequestMapping("/api/user")
class UserController(
        private val userRepo: UserRepository,
        private val balanceSheetRepo: BalanceSheetRepository
) {
    @RequestMapping("/balance-sheet-id")
    @Transactional(readOnly = true)
    fun getBalanceSheetId(@CookieValue(value = "userId", required = false) userId: UUID?): HmResponse {
        return try {
            if (userId == null) {
                throw HmException(HmException.Code.UserIdCookieIsEmpty)
            }
            val user = userRepo.findByIdOrNull(userId)!!
            HmResponse.getOk(user.bsId)
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    @RequestMapping(value = ["/login"], method = [RequestMethod.POST])
    @Transactional
    fun login(@RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String): HmResponse {
        return try {
            val email = decodeAuthorization(authorization).first
            log.info { "User login; email:$email" }
            val user = userRepo.findByEmail(email)!!
            val auth = Authentication(user.id, user.bsId)
            HmResponse.getOk(auth)
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    fun getUser(authorization: String): User? {
        val login = decodeAuthorization(authorization).first
        return userRepo.findByEmail(login)
    }

    @RequestMapping(method = [RequestMethod.DELETE])
    @Transactional
    fun deleteBalanceSheet(@RequestParam id: UUID) {
        balanceSheetRepo.deleteById(id)
    }

    fun createUserWithBalanceSheet(email: String, pwdHash: String): User {
        val bs = newInstance()
        balanceSheetRepo.save(bs)
        val user = User(UUID.randomUUID(), bs.id, email, pwdHash)
        userRepo.save(user)
        return user
    }

    private fun decodeAuthorization(authorization: String): Pair<String, String> {
        val base64Credentials = authorization.substring("Basic".length).trim()
        val credDecoded = Base64.getDecoder().decode(base64Credentials)
        val credentials = String(credDecoded)
        val arrayCredential = credentials.split(":".toRegex(), 2)
        return arrayCredential[0] to arrayCredential[1]
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }

}