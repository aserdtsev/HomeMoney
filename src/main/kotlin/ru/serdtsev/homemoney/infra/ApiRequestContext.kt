package ru.serdtsev.homemoney.infra

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.domain.repository.BalanceSheetRepository
import ru.serdtsev.homemoney.infra.exception.HmException
import java.util.*
import kotlin.concurrent.getOrSet

@Service
class ApiRequestContextHolder(
    private val balanceSheetRepository: BalanceSheetRepository
) {
    companion object {
        lateinit var instance: ApiRequestContextHolder
        private val requestContextTls = ThreadLocal<ApiRequestContext>()
        var apiRequestContext: ApiRequestContext
            get() = requestContextTls.getOrSet { ApiRequestContext() }
            set(value) = requestContextTls.set(value)

        var requestId: String
            get() = apiRequestContext.requestId!!
            set(value) {
                apiRequestContext.requestId = value
            }

        // todo Delete
        var bsId: UUID
            get() = apiRequestContext.bsId!!
            set(value) {
                apiRequestContext.bsId = value
                apiRequestContext.balanceSheet = instance.getBalanceSheet()
            }

        var balanceSheet: BalanceSheet
            get() = requireNotNull(apiRequestContext.balanceSheet)
            set(value) {
                apiRequestContext.balanceSheet = value
                apiRequestContext.bsId = value.id
            }

        fun clear() {
            requestContextTls.remove()
        }
    }

    @PostConstruct
    fun init() {
        instance = this
    }

    fun getRequestId(): String = requestId

    fun getBsId(): UUID = bsId

    fun getBalanceSheet(): BalanceSheet =
        balanceSheetRepository.findByIdOrNull(bsId) ?: throw HmException(HmException.Code.BalanceSheetNotFound)
}

data class ApiRequestContext(var requestId: String? = null, var bsId: UUID? = null, var balanceSheet: BalanceSheet? = null)