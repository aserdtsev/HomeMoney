package ru.serdtsev.homemoney.domain.usecase.moneyoper

import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus.Done
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import java.util.*
import java.util.stream.IntStream

@Service
class UpMoneyOperUseCase(private val moneyOperRepository: MoneyOperRepository) {
    fun run(moneyOperId: UUID) {
        val bsId = ApiRequestContextHolder.bsId
        val moneyOper = moneyOperRepository.findById(moneyOperId)
        val moneyOpers = moneyOperRepository.findByStatusAndPerformed(Done, moneyOper.performed)
            .sortedBy { it.dateNum }
            .toMutableList()
        val index = moneyOpers.indexOf(moneyOper)
        if (index < moneyOpers.size - 1) {
            val nextMoneyOper = moneyOpers[index + 1]
            moneyOpers[index + 1] = moneyOper
            moneyOpers[index] = nextMoneyOper
            IntStream.range(0, moneyOpers.size).forEach { i: Int ->
                val aMoneyOper = moneyOpers[i]
                aMoneyOper.dateNum = i
                DomainEventPublisher.instance.publish(aMoneyOper)
            }
        }
    }
}