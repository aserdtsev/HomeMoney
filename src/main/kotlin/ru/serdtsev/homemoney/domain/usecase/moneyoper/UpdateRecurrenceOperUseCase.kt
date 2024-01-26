package ru.serdtsev.homemoney.domain.usecase.moneyoper

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.moneyoper.RecurrenceOper
import ru.serdtsev.homemoney.domain.repository.RecurrenceOperRepository

@Service
class UpdateRecurrenceOperUseCase(private val recurrenceOperRepository: RecurrenceOperRepository) {
    @Transactional
    fun run(recurrenceOper: RecurrenceOper) {
        val origRecurrenceOper= requireNotNull(recurrenceOperRepository.findByIdOrNull(recurrenceOper.id))
        origRecurrenceOper.template = recurrenceOper.template
        origRecurrenceOper.nextDate = recurrenceOper.nextDate
        DomainEventPublisher.instance.publish(origRecurrenceOper)
    }
}