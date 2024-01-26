package ru.serdtsev.homemoney.domain.usecase.moneyoper

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import java.util.*

@Service
class DeleteMoneyOperUseCase(private val moneyOperRepository: MoneyOperRepository) {
    @Transactional
    fun run(moneyOperId: UUID) {
        val origMoneyOper = moneyOperRepository.findById(moneyOperId)
        origMoneyOper.cancel()
    }
}