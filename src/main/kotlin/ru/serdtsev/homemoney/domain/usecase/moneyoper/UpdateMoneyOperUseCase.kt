package ru.serdtsev.homemoney.domain.usecase.moneyoper

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository

@Service
class UpdateMoneyOperUseCase(private val moneyOperRepository: MoneyOperRepository) {
    @Transactional
    fun run(moneyOper: MoneyOper) {
        val origMoneyOper = moneyOperRepository.findById(moneyOper.id)
        MoneyOper.merge(moneyOper, origMoneyOper)
    }
}