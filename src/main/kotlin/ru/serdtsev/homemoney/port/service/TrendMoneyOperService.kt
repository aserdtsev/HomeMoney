package ru.serdtsev.homemoney.port.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.event.TrendMoneyOperNeedsToBeUpdated
import ru.serdtsev.homemoney.domain.model.moneyoper.Period
import ru.serdtsev.homemoney.domain.repository.BalanceSheetRepository
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import java.time.LocalDate
import java.util.concurrent.TimeUnit


@Service
class TrendMoneyOperService(
    val balanceSheetRepository: BalanceSheetRepository,
    val moneyOperRepository: MoneyOperRepository
    ) {
    @Scheduled(fixedDelay = 1L, timeUnit = TimeUnit.DAYS)
    fun refreshTrendMoneyOpers() {
        balanceSheetRepository.findAll().forEach { balanceSheet ->
            ApiRequestContextHolder.apiRequestContext.balanceSheet = balanceSheet
            moneyOperRepository.findTrends()
                .forEach {
                    val category = it.tags.first { it.isCategory }
                    val event = TrendMoneyOperNeedsToBeUpdated(Period.Month, category, LocalDate.now())
                    DomainEventPublisher.instance.publish(event)
                }
        }
    }
}