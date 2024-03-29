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
                oper.items
                    .filter { creditCardChargeItem -> creditCardChargeItem.balanceId == operItem.balanceId }
                    .forEach { creditCardChargeItem ->
                        // todo Учесть, что задолженность может гаситься несколькими операциями
                        if (debtAmount > BigDecimal.ZERO) {
                            creditCardChargeItem.repaymentSchedule?.get(0)?.let {
                                it.repaymentDebtOperItemId = operItem.id
                                val repaymentDebtAmount =
                                    (it.repaidDebtAmount ?: BigDecimal.ZERO) + min(it.totalAmount, debtAmount)
                                assert(repaymentDebtAmount > BigDecimal.ZERO)
                                it.repaidDebtAmount = repaymentDebtAmount
                                if (it.repaidDebtAmount == it.totalAmount) {
                                    it.endDate = operItem.performed
                                }
                                debtAmount -= repaymentDebtAmount
                            }
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