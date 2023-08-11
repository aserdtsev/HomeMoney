package ru.serdtsev.homemoney.domain.usecase.moneyoper

import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.model.moneyoper.RecurrenceOper
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import java.util.*

@Service
class CreateRecurrenceOperUseCase(private val moneyOperRepository: MoneyOperRepository) {
    fun run(moneyOperId: UUID) {
        val sample = moneyOperRepository.findById(moneyOperId)
        RecurrenceOper.of(sample)
    }
}