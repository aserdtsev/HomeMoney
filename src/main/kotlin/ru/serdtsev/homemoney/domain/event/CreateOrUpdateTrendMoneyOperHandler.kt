package ru.serdtsev.homemoney.domain.event

import mu.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import ru.serdtsev.homemoney.domain.repository.BalanceRepository
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import java.math.RoundingMode
import java.time.LocalDate

@Service
class CreateOrUpdateTrendMoneyOperHandler(
    private val moneyOperRepository: MoneyOperRepository,
    private val balanceRepository: BalanceRepository
) {
    private val log = KotlinLogging.logger {  }

    @EventListener
    fun handler(event: MoneyOperStatusChanged) {
        val moneyOper = event.moneyOper
        val category = moneyOper.tags.firstOrNull { it.isCategory }
        val period = moneyOper.period
        val calculatedDate = moneyOper.performed
        if (moneyOper.recurrenceId != null || period != Period.Month || category == null) {
            return
        }

        createOrUpdateTrendMoneyOper(period, category, calculatedDate)
    }

    @EventListener
    fun handler(event: TrendMoneyOperNeedsToBeUpdated) = with(event) {
        createOrUpdateTrendMoneyOper(period, category, calculatedDate)
    }

    private fun createOrUpdateTrendMoneyOper(period: Period, category: Tag, calculatedDate: LocalDate) {
        val (items, intervalDays) = getItemsAndIntervalDays(period, category)
        val dateToSumMap = items
            .groupBy { it.performed }
            .entries
            .map { it.key to it.value.sumOf { item -> item.value } }
        val sum = dateToSumMap.sumOf { it.second }
        val count = dateToSumMap.size
        val avg = sum.divide(count.toBigDecimal(), RoundingMode.HALF_UP)
        val n = intervalDays / count
        val recurrenceParams = DayRecurrenceParams(n.toInt())
        val balanceId = items.groupBy { it.balanceId }
            .entries
            .map { it.key to it.value.sumOf { item -> item.value } }
            .maxBy { it.second.abs() }
            .first
        val trendMoneyOper = moneyOperRepository.findTrend(category, Period.Day)
            ?.apply {
                this.recurrenceParams = recurrenceParams
                val item = this.items[0]
                item.value = avg
                item.balanceId = balanceId
                this.performed = recurrenceParams.getNext(calculatedDate)
                while (this.performed < LocalDate.now()) {
                    this.performed = recurrenceParams.getNext(this.performed)
                }
            }
            ?: MoneyOper.of(MoneyOperStatus.Trend, Period.Day, recurrenceParams).apply {
                this.setTags(listOf(category))
                this.performed = recurrenceParams.getNext(calculatedDate)
                val balance = balanceRepository.findById(balanceId)
                this.addItem(balance, avg)
            }
        log.info { "Trend: $trendMoneyOper" }
        DomainEventPublisher.instance.publish(trendMoneyOper)
    }

    private fun getItemsAndIntervalDays(period: Period, category: Tag): Pair<List<MoneyOperItem>, Long> {
        var intervalDays = 30L
        lateinit var items: List<MoneyOperItem>
        while (intervalDays < 366L) {
            val startDate = LocalDate.now().minusDays(intervalDays)
            items = moneyOperRepository.findByStatusAndPerformedGreaterThan(MoneyOperStatus.Done, startDate)
                .filter { it.period == period }
                .filter { it.recurrenceId == null }
                .filter { it.tags.contains(category) }
                .flatMap { it.items }
            if (items.size >= 5) {
                break
            } else {
                intervalDays *= 3
            }
        }
        return Pair(items, intervalDays)
    }
}