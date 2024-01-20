package ru.serdtsev.homemoney.domain.event

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperItem
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus.*
import ru.serdtsev.homemoney.domain.model.moneyoper.RepaymentSchedule
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository

@Service
@Suppress("DuplicatedCode")
class EarlyPaymentDebtHandler(
    private val moneyOperRepository: MoneyOperRepository
) {
    @EventListener
    fun handler(event: MoneyOperStatusChanged) {
        val beforeStatus = event.beforeStatus
        val afterStatus = event.afterStatus
        if (beforeStatus in listOf(New, Pending, Cancelled, Recurrence) && afterStatus in listOf(Pending, Cancelled)) {
            return
        }
        val revert = beforeStatus == Done && afterStatus != Done
        val moneyOper = event.moneyOper
        moneyOper.items.forEach { operItem ->
            if (operItem.isDebtRepayment) {
                if (!revert) {
                    earlyRepaymentDebt(operItem)
                } else {
                    rollbackEarlyRepaymentDebt(operItem)
                }
            }
        }
    }

    private fun earlyRepaymentDebt(operItem: MoneyOperItem) {
        var debtAmount = operItem.value
        moneyOperRepository.findByCreditCardChargesForEarlyRepaymentDebt(operItem.balanceId, operItem.performed)
            .forEach { oper ->
                debtAmount = oper.earlyRepaymentDebt(operItem, debtAmount)
            }
    }

    private fun rollbackEarlyRepaymentDebt(operItem: MoneyOperItem) {
        moneyOperRepository.findByCreditCardChargesForRollbackEarlyRepaymentDebt(operItem.id)
            .forEach { oper ->
                oper.items
                    .filter { creditCardChargeItem -> creditCardChargeItem.balanceId == operItem.balanceId }
                    .forEach { creditCardChargeItem ->
                        val credit = requireNotNull(creditCardChargeItem.balance.credit)
                        // todo Учесть рассрочку на несколько месяцев
                        creditCardChargeItem.repaymentSchedule = RepaymentSchedule.of(creditCardChargeItem.performed,
                            credit, creditCardChargeItem.value.abs())
                    }
                DomainEventPublisher.instance.publish(oper)
            }
    }

}