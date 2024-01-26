package ru.serdtsev.homemoney.domain.usecase.moneyoper

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus
import ru.serdtsev.homemoney.domain.repository.RecurrenceOperRepository

@Service
class SkipMoneyOperUseCase(
    private val recurrenceOperRepository: RecurrenceOperRepository
) {
    @Transactional
    fun run(moneyOper: MoneyOper) {
        when (moneyOper.status) {
            MoneyOperStatus.Pending -> skipPendingMoneyOper(moneyOper)
            MoneyOperStatus.Recurrence -> skipRecurrenceMoneyOper(moneyOper)
            else -> throw IllegalStateException("MoneyOper invalid status: $moneyOper")
        }
    }

    private fun skipPendingMoneyOper(moneyOper: MoneyOper) {
        moneyOper.skipPending()
    }

    private fun skipRecurrenceMoneyOper(moneyOper: MoneyOper) {
        recurrenceOperRepository.findById(requireNotNull(moneyOper.recurrenceId)).skipNextDate()
    }
}