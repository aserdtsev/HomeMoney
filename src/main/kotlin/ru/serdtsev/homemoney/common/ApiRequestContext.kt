package ru.serdtsev.homemoney.common

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository
import java.util.*
import kotlin.concurrent.getOrSet

@Service
class ApiRequestContextHolder(private val balanceSheetRepo: BalanceSheetRepository) {
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

        fun clear() {
            requestContextTls.remove()
        }
    }

    fun getBsId(): UUID = bsId

    fun getBalanceSheet(): BalanceSheet {
        if (apiRequestContext.balanceSheet == null) {
            apiRequestContext.balanceSheet = balanceSheetRepo.findByIdOrNull(bsId)
                ?: throw HmException(HmException.Code.BalanceSheetNotFound)
        }
        return apiRequestContext.balanceSheet!!
    }
}

data class ApiRequestContext(var requestId: String? = null, var bsId: UUID? = null, var balanceSheet: BalanceSheet? = null)