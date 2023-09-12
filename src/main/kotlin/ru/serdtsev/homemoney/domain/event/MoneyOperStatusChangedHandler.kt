package ru.serdtsev.homemoney.domain.event

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus.*
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatusChanged
import java.math.BigDecimal

@Service
class MoneyOperStatusChangedHandler {
    @EventListener
    fun handler(event: MoneyOperStatusChanged) {
        assert(event.afterStatus == event.moneyOper.status) { event.moneyOper }
        assert(event.afterStatus != doneNew) { event.moneyOper }

        val beforeStatus = event.beforeStatus
        val afterStatus = event.afterStatus
        if (beforeStatus in listOf(pending, cancelled, doneNew, recurrence) && afterStatus in listOf(pending, cancelled)) {
            return
        }
        val revert = beforeStatus == done && afterStatus != done
        val factor = BigDecimal.ONE.let { if (revert) it.negate() else it }
        val moneyOper = event.moneyOper
        moneyOper.items.forEach {
            it.balance.changeValue(it.value * factor, moneyOper.id)
        }
    }
}