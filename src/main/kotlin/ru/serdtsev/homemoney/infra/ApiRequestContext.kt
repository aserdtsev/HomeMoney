package ru.serdtsev.homemoney.infra

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
        private val requestContextTls = ThreadLocal<ApiRequestContext>()
        var apiRequestContext: ApiRequestContext
            get() = requestContextTls.getOrSet { ApiRequestContext() }
            set(value) = requestContextTls.set(value)

        var requestId: String
            get() = apiRequestContext.requestId!!
            set(value) {
                apiRequestContext.requestId = value
            }

        var bsId: UUID
            get() = apiRequestContext.bsId!!
            set(value) {
                apiRequestContext.bsId = value
            }

        var balanceSheet: BalanceSheet
            get() = apiRequestContext.balanceSheet!!
            private set(_) {}

        fun clear() {
            requestContextTls.remove()
        }
    }

    fun getRequestId(): String = requestId

    fun getBsId(): UUID = bsId

    fun getBalanceSheet(): BalanceSheet =
        balanceSheetRepository.findByIdOrNull(bsId) ?: throw HmException(HmException.Code.BalanceSheetNotFound)
}

data class ApiRequestContext(var requestId: String? = null, var bsId: UUID? = null, var balanceSheet: BalanceSheet? = null)