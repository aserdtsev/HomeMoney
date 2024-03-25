package ru.serdtsev.homemoney.domain.event

import mu.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import ru.serdtsev.homemoney.domain.repository.BalanceRepository
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class CreateOrUpdateTrendMoneyOperHandler(
    private val moneyOperRepository: MoneyOperRepository,
    private val balanceRepository: BalanceRepository,
    private val clock: Clock
) {
    private val log = KotlinLogging.logger {  }

    @EventListener
    fun moneyOperStatusChangedHandlerHandler(event: MoneyOperStatusChanged) {
        val moneyOper = event.moneyOper
        val category = moneyOper.tags.firstOrNull { it.isCategory }
        val period = moneyOper.period ?: Period.Day
        if (moneyOper.recurrenceId != null || period != Period.Day || category == null) {
            return
        }
        createOrUpdateTrendMoneyOper(period, category)
    }

    @EventListener
    fun trendMoneyOperNeedsToBeUpdatedHandler(event: TrendMoneyOperNeedsToBeUpdated) = with(event) {
        createOrUpdateTrendMoneyOper(period, category)
    }

    private fun createOrUpdateTrendMoneyOper(period: Period, category: Tag) {
        val currentDate = LocalDate.now(clock)
        val items = getItemsAndIntervalDays(period, category)
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
        val days = dateToSumMap.map { it.first }
        val lastDate = requireNotNull(days.max())
        val calcIntervalDays =  ChronoUnit.DAYS.between(days.min(), currentDate).toInt()
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
                val currentDateSum = items.filter { moItem -> moItem.performed == currentDate }
                        .sumOf { it.value }
                        .abs()
                if (currentDateSum > avg.abs().multiply(BigDecimal("0.75"))) {
                    this.performed = recurrenceParams.getNext(currentDate)
                } else if (currentDateSum > BigDecimal.ZERO) {
                    this.performed = currentDate
                }
                while (this.performed < currentDate) {
                    this.performed = recurrenceParams.getNext(this.performed)
                }
            }
            ?: MoneyOper.of(MoneyOperStatus.Trend, Period.Day, recurrenceParams).apply {
                this.setTags(listOf(category))
                this.performed = recurrenceParams.getNext(lastDate).let {
                    if (it < currentDate) currentDate else it
                }
                val balance = balanceRepository.findById(balanceId)
                this.addItem(balance, avg)
            }
        log.info { "Trend: $trendMoneyOper" }
        DomainEventPublisher.instance.publish(trendMoneyOper)
    }

    private fun getItemsAndIntervalDays(period: Period, category: Tag): List<MoneyOperItem> {
        val currentDate = LocalDate.now(clock)
        var intervalDays = 30L
        lateinit var items: List<MoneyOperItem>
        while (intervalDays < 366L) {
            val startDate = currentDate.minusDays(intervalDays)
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