package ru.serdtsev.homemoney.domain.usecase.moneyoper

import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import java.util.*

@Service
class DeleteMoneyOperUseCase(private val moneyOperRepository: MoneyOperRepository) {
    fun run(moneyOperId: UUID) {
        val origMoneyOper = moneyOperRepository.findById(moneyOperId)
        origMoneyOper.cancel()
    }
}