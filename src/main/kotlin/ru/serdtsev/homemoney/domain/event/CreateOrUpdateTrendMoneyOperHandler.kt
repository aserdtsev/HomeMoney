package ru.serdtsev.homemoney.domain.event

import mu.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import java.math.RoundingMode
import java.time.LocalDate

@Service
class CreateOrUpdateTrendMoneyOperHandler(
    private val moneyOperRepository: MoneyOperRepository
) {
    private val log = KotlinLogging.logger {  }

    @EventListener
    fun handler(event: MoneyOperStatusChanged) {
        val moneyOper = event.moneyOper
        val category = moneyOper.tags.firstOrNull() { it.isCategory }
        if (moneyOper.recurrenceId != null || moneyOper.period != Period.Month || category == null) {
            return
        }
        val intervalDays = 30L
        val startDate = LocalDate.now().minusDays(intervalDays)
        val dateToSumMap = moneyOperRepository.findByStatusAndPerformedGreaterThan(MoneyOperStatus.Done, startDate)
            .filter { it.period == moneyOper.period }
            .filter { it.recurrenceId == null }
            .filter { it.tags.contains(category) }
            .flatMap { it.items }
            .groupBy { it.performed }
            .entries
            .map { it.key to it.value.sumOf { item -> item.value } }
        if (dateToSumMap.isEmpty()) {
            return
        }
        val sum = dateToSumMap.sumOf { it.second }
        val count = dateToSumMap.size
        val avg = sum.divide(count.toBigDecimal(), RoundingMode.HALF_UP)
        val n = intervalDays / count
        val recurrenceParams = DayRecurrenceParams(n.toInt())
        val trendMoneyOper = moneyOperRepository.findTrend(category, Period.Day)
            ?.apply {
                this.recurrenceParams = recurrenceParams
                this.items[0].value = avg
                this.performed = recurrenceParams.getNext(moneyOper.performed)
                while (this.performed < LocalDate.now()) {
                    this.performed = recurrenceParams.getNext(this.performed)
                }
            }
            ?: MoneyOper.of(MoneyOperStatus.Trend, Period.Day, recurrenceParams).apply {
                this.setTags(listOf(category))
                this.performed = recurrenceParams.getNext(moneyOper.performed)
                val item = moneyOper.items[0]
                this.addItem(item.balance, avg)
            }
        log.info { "Trend: $trendMoneyOper" }
        DomainEventPublisher.instance.publish(trendMoneyOper)
    }
}