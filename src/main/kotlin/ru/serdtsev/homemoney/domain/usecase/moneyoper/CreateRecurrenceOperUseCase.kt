package ru.serdtsev.homemoney.domain.usecase.moneyoper

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.domain.model.moneyoper.RecurrenceOper
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import java.util.*

@Service
class CreateRecurrenceOperUseCase(private val moneyOperRepository: MoneyOperRepository) {
    @Transactional
    fun run(moneyOperId: UUID) {
        val sample = moneyOperRepository.findById(moneyOperId)
        RecurrenceOper.of(sample)
    }
}