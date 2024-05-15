package ru.serdtsev.homemoney.domain.usecase.moneyoper

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.event.TrendMoneyOperNeedsToBeUpdated
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.Period
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import java.time.Clock
import java.time.LocalDate

@Service
class UpdateMoneyOperUseCase(
    private val moneyOperRepository: MoneyOperRepository,
    private val clock: Clock
) {
    @Transactional
    fun run(moneyOper: MoneyOper) {
        val origMoneyOper = moneyOperRepository.findById(moneyOper.id)
        MoneyOper.merge(moneyOper, origMoneyOper)
        moneyOper.tags
            .firstOrNull { it.isCategory }
            ?.let { category ->
                val event = TrendMoneyOperNeedsToBeUpdated(Period.Day, category, LocalDate.now(clock))
                DomainEventPublisher.instance.publish(event)
            }
    }
}