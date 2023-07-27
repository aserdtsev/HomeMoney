package ru.serdtsev.homemoney.domain.usecase.moneyoper

import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository

@Service
class UpdateMoneyOperUseCase(private val moneyOperRepository: MoneyOperRepository) {
    fun run(moneyOper: MoneyOper) {
        val origMoneyOper = moneyOperRepository.findById(moneyOper.id)
        MoneyOper.merge(moneyOper, origMoneyOper)
    }
}