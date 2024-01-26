package ru.serdtsev.homemoney.domain.usecase.moneyoper

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.repository.RecurrenceOperRepository
import java.util.*

@Service
class SkipRecurrenceOperUseCase(private val recurrenceOperRepository: RecurrenceOperRepository) {
    @Transactional
    fun run(recurrenceId: UUID) {
        val recurrenceOper = recurrenceOperRepository.findById(recurrenceId)
        recurrenceOper.skipNextDate()
        DomainEventPublisher.instance.publish(recurrenceOper)
    }
}