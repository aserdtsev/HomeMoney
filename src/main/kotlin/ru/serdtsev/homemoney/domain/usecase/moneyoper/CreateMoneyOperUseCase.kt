package ru.serdtsev.homemoney.domain.usecase.moneyoper

import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.domain.repository.RecurrenceOperRepository
import java.time.LocalDate
import java.util.*

@Service
class CreateMoneyOperUseCase(
    private val recurrenceOperRepository: RecurrenceOperRepository,
    private val moneyOperRepository: MoneyOperRepository
) {

    fun run(moneyOper: MoneyOper): List<MoneyOper> {
        val resultMoneyOpers = mutableListOf(moneyOper)
        val balanceSheet = moneyOper.balanceSheet
        createReserveMoneyOper(balanceSheet, moneyOper)?.let { resultMoneyOpers.add(it) }
        moneyOper.recurrenceId?.also { skipRecurrenceOper(it) }
        if ((moneyOper.status in listOf(MoneyOperStatus.done, MoneyOperStatus.doneNew))
            && !moneyOper.performed.isAfter(LocalDate.now())) {
            resultMoneyOpers.forEach { it.complete() }
        } else {
            resultMoneyOpers.forEach { it.postpone() }
        }
        resultMoneyOpers.forEach { resultMoneyOper ->
            moneyOperRepository
                .findByBalanceSheetAndStatusAndPerformed(
                    balanceSheet.id,
                    resultMoneyOper.status,
                    resultMoneyOper.performed
                ).lastOrNull { moneyOper -> moneyOper.id !in resultMoneyOpers.map { it.id } }
                ?.let {
                    resultMoneyOper.dateNum = it.dateNum + 1
                    DomainEventPublisher.instance.publish(resultMoneyOper)
                }
        }
        return resultMoneyOpers
    }

    private fun createReserveMoneyOper(balanceSheet: BalanceSheet, oper: MoneyOper): MoneyOper? {
        return if (oper.items.any { it.balance.reserve != null }) {
            val tags = oper.tags
            val dateNum = oper.dateNum
            val reserveMoneyOper = MoneyOper(balanceSheet, MoneyOperStatus.pending, oper.performed, dateNum, tags,
                oper.comment, oper.period)
            oper.items
                .filter { it.balance.reserve != null }
                .forEach { reserveMoneyOper.addItem(it.balance.reserve!!, it.value, it.performed, it.index + 1) }
            reserveMoneyOper
        }
        else null
    }

    private fun skipRecurrenceOper(recurrenceId: UUID) {
        recurrenceOperRepository.findById(recurrenceId).skipNextDate()
    }
}