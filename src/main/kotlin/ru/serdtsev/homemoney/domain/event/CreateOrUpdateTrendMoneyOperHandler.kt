package ru.serdtsev.homemoney.domain.event

import mu.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import ru.serdtsev.homemoney.domain.repository.BalanceRepository
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import java.math.BigDecimal
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
        val period = moneyOper.period ?: Period.Day
        val calculatedDate = moneyOper.performed
        if (moneyOper.recurrenceId != null || period != Period.Day || category == null) {
            return
        }

        createOrUpdateTrendMoneyOper(period, category, calculatedDate)
    }

    @EventListener
    fun handler(event: TrendMoneyOperNeedsToBeUpdated) = with(event) {
        createOrUpdateTrendMoneyOper(period, category, calculatedDate)
    }

    private fun createOrUpdateTrendMoneyOper(period: Period, category: Tag, calculatedDate: LocalDate) {
        val items = getItemsAndIntervalDays(period, category, calculatedDate)
        val dateToSumMap = items
            .groupBy { it.performed }
            .entries
            .map { it.key to it.value.sumOf { item -> item.value } }
        val sum = dateToSumMap.sumOf { it.second }
        val count = dateToSumMap.size
        if (count < 3) {
            moneyOperRepository.findTrend(category, Period.Day)?.apply { this.cancel() }
            return
        }
        val avg = sum.divide(count.toBigDecimal(), RoundingMode.HALF_UP)
        val calcIntervalDays = run {
            val days = dateToSumMap.map { it.first }
            java.time.Period.between(days.min(), days.max()).days
        }
        val recurrenceParams = DayRecurrenceParams(calcIntervalDays / (count - 1))
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
                if (items.none { moItem -> moItem.performed > calculatedDate }) {
                    val calculatedDaySum = items.filter { moItem -> moItem.performed == calculatedDate }
                            .sumOf { it.value }
                            .abs()
                    if (calculatedDaySum > avg.abs().multiply(BigDecimal("0.75"))) {
                        this.performed = recurrenceParams.getNext(calculatedDate)
                    } else if (calculatedDaySum > BigDecimal.ZERO) {
                        this.performed = calculatedDate
                    }
                    while (this.performed < calculatedDate) {
                        this.performed = recurrenceParams.getNext(this.performed)
                    }
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

    private fun getItemsAndIntervalDays(period: Period, category: Tag, calculatedDate: LocalDate): List<MoneyOperItem> {
        var intervalDays = 30L
        lateinit var items: List<MoneyOperItem>
        while (intervalDays < 366L) {
            val startDate = calculatedDate.minusDays(intervalDays)
            items = moneyOperRepository.findByStatusAndPerformedGreaterThan(MoneyOperStatus.Done, startDate)
                .filter { it.period == period }
                .filter { it.recurrenceId == null }
                .filter { it.tags.contains(category) }
                .flatMap { it.items }
            if (items.size >= 3) {
                break
            } else {
                intervalDays *= 2
            }
        }
        return items
    }
}