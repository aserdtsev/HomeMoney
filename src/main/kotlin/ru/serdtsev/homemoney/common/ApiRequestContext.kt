package ru.serdtsev.homemoney.common

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.balancesheet.dao.BalanceSheetRepo
import java.util.*
import kotlin.concurrent.getOrSet

@Service
class ApiRequestContextHolder(private val balanceSheetRepo: BalanceSheetRepo) {
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

    fun getBalanceSheet(): BalanceSheet =
        balanceSheetRepo.findByIdOrNull(bsId) ?: throw HmException(HmException.Code.BalanceSheetNotFound)
}

data class ApiRequestContext(var requestId: String? = null, var bsId: UUID? = null, var balanceSheet: BalanceSheet? = null)