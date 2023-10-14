package ru.serdtsev.homemoney.domain.event

import org.apache.commons.lang3.ObjectUtils.min
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperItem
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus.*
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatusChanged
import ru.serdtsev.homemoney.domain.model.moneyoper.RepaymentSchedule
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import java.math.BigDecimal

@Service
class MoneyOperStatusChangedHandler(val moneyOperRepository: MoneyOperRepository) {
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
        moneyOper.items.forEach { operItem ->
            operItem.balance.changeValue(operItem.value * factor, moneyOper.id)
            if (operItem.isDebtRepayment)
                if (!revert) earlyRepaymentDebt(operItem) else rollbackEarlyRepaymentDebt(operItem)
        }
    }

    private fun earlyRepaymentDebt(operItem: MoneyOperItem) {
        var debtAmount = operItem.value
        moneyOperRepository.findByCreditCardChargesForEarlyRepaymentDebt(operItem.balanceId, operItem.performed)
            .forEach { oper ->
                oper.items
                    .filter { creditCardChargeItem -> creditCardChargeItem.balanceId == operItem.balanceId }
                    .forEach { creditCardChargeItem ->
                        // todo Учесть, что задолженность может гаситься несколькими операциями
                        creditCardChargeItem.repaymentSchedule?.get(0)?.let {
                            it.repaymentDebtOperItemId = operItem.id
                            val repaymentDebtAmount =
                                (it.repaidDebtAmount ?: BigDecimal.ZERO) + min(it.totalAmount, debtAmount)
                            it.repaidDebtAmount = repaymentDebtAmount
                            if (it.repaidDebtAmount == it.totalAmount) {
                                it.endDate = operItem.performed
                            }
                            debtAmount -= repaymentDebtAmount
                        }
                    }
                DomainEventPublisher.instance.publish(oper)
            }
    }

    private fun rollbackEarlyRepaymentDebt(operItem: MoneyOperItem) {
        moneyOperRepository.findByCreditCardChargesForRollbackEarlyRepaymentDebt(operItem.id)
            .forEach { oper ->
                oper.items
                    .filter { creditCardChargeItem -> creditCardChargeItem.balanceId == operItem.balanceId }
                    .forEach { creditCardChargeItem ->
                        val credit = requireNotNull(creditCardChargeItem.balance.credit)
                        creditCardChargeItem.repaymentSchedule = RepaymentSchedule.of(creditCardChargeItem.performed,
                            credit, creditCardChargeItem.value.abs())
                    }
                DomainEventPublisher.instance.publish(oper)
            }
    }

}