package ru.serdtsev.homemoney.domain.usecase.moneyoper

import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.domain.repository.RecurrenceOperRepository

@Service
class SkipMoneyOperUseCase(
    private val recurrenceOperRepository: RecurrenceOperRepository,
    private val moneyOperRepository: MoneyOperRepository
) {
    fun run(moneyOper: MoneyOper) {
        when (moneyOper.status) {
            MoneyOperStatus.pending -> skipPendingMoneyOper(moneyOper)
            MoneyOperStatus.recurrence -> skipRecurrenceMoneyOper(moneyOper)
            else -> throw IllegalStateException("MoneyOper invalid status: $moneyOper")
        }
    }

    private fun skipPendingMoneyOper(moneyOper: MoneyOper) {
        moneyOper.skipPending()
    }

    private fun skipRecurrenceMoneyOper(moneyOper: MoneyOper) {
        recurrenceOperRepository.findById(moneyOper.recurrenceId!!).skipNextDate()
    }
}