package ru.serdtsev.homemoney.port.controller

import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.event.UserLogged
import ru.serdtsev.homemoney.domain.model.User
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.domain.repository.BalanceSheetRepository
import ru.serdtsev.homemoney.domain.repository.UserRepository
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import ru.serdtsev.homemoney.infra.exception.HmException
import ru.serdtsev.homemoney.port.common.HmResponse
import ru.serdtsev.homemoney.port.dto.LoginResponse
import java.util.*

@RestController
@RequestMapping("/api/user")
class UserController(
    private val userRepository: UserRepository,
    private val balanceSheetRepository: BalanceSheetRepository
) {
    @RequestMapping(value = ["/login"], method = [RequestMethod.POST])
    @Transactional
    fun login(@RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String): HmResponse {
        return try {
            val email = decodeAuthorization(authorization).first
            log.info { "User login; email:$email" }
            val user = requireNotNull(userRepository.findByEmail(email))
            ApiRequestContextHolder.balanceSheet = balanceSheetRepository.findById(user.bsId)
            DomainEventPublisher.instance.publish(UserLogged())
            val auth = LoginResponse(user.bsId)
            HmResponse.getOk(auth)
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    @RequestMapping(method = [RequestMethod.DELETE])
    @Transactional
    fun deleteBalanceSheet(@RequestParam id: UUID) {
        balanceSheetRepository.deleteById(id)
    }

    fun createUserWithBalanceSheet(email: String, pwdHash: String): User {
        val bs = BalanceSheet()
        DomainEventPublisher.instance.publish(bs)
        val user = User(UUID.randomUUID(), bs.id, email, pwdHash)
        DomainEventPublisher.instance.publish(user)
        return user
    }

    private fun decodeAuthorization(authorization: String): Pair<String, String> {
        val base64Credentials = authorization.substring("Basic".length).trim()
        val credentials = String(Base64.getDecoder().decode(base64Credentials))
        val arrayCredential = credentials.split(":".toRegex(), 2)
        return arrayCredential[0] to arrayCredential[1]
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }

}