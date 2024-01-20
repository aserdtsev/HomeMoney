package ru.serdtsev.homemoney.domain.event

import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus.*
import java.math.BigDecimal

@Service
@Suppress("DuplicatedCode")
class ChangeBalanceValueHandler {
    @EventListener
    @Order(0)
    fun handler(event: MoneyOperStatusChanged) {
        assert(event.afterStatus == event.moneyOper.status) { event.moneyOper }
        assert(event.afterStatus != New) { event.moneyOper }

        val beforeStatus = event.beforeStatus
        val afterStatus = event.afterStatus
        if (beforeStatus in listOf(New, Pending, Cancelled, Recurrence) && afterStatus in listOf(Pending, Cancelled)) {
            return
        }
        val revert = beforeStatus == Done && afterStatus != Done
        val factor = BigDecimal.ONE.let { if (revert) it.negate() else it }
        val moneyOper = event.moneyOper
        moneyOper.items.forEach { operItem ->
            operItem.balance.changeValue(operItem.value * factor, moneyOper.id)
        }
    }
}