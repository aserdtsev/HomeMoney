package ru.serdtsev.homemoney.domain.event

import kotlinx.coroutines.*
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.model.moneyoper.Period
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.infra.config.CoroutineApiRequestContext
import java.time.LocalDate

@Service
class TrendMoneyOpersRefreshHandler(val moneyOperRepository: MoneyOperRepository) {
    @EventListener
    @OptIn(DelicateCoroutinesApi::class)
    fun handler(event: UserLogged) {
        GlobalScope.launch(Dispatchers.IO + CoroutineApiRequestContext()) {
            moneyOperRepository.findTrends()
                .filter { it.performed < LocalDate.now() }
                .forEach {
                    val category = it.tags.first { it.isCategory }
                    val aEvent = TrendMoneyOperNeedsToBeUpdated(Period.Day, category, LocalDate.now())
                    DomainEventPublisher.instance.publish(aEvent)
                }
        }
    }
}