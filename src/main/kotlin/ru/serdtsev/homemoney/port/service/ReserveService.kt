package ru.serdtsev.homemoney.port.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.account.Reserve
import ru.serdtsev.homemoney.domain.repository.ReserveRepository
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import java.util.*

@Service
class ReserveService(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val reserveRepository: ReserveRepository,
    private val balanceService: BalanceService
) {
    @Transactional(readOnly = true)
    fun getReserves(): List<Reserve> {
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        return reserveRepository.findByBalanceSheet(balanceSheet)
    }

    @Transactional
    fun create(reserve: Reserve) {
        DomainEventPublisher.instance.publish(reserve)
    }

    @Transactional
    fun update(reserve: Reserve) {
        val origReserve = reserveRepository.findByIdOrNull(reserve.id)!!
        Reserve.merge(reserve, origReserve)
        DomainEventPublisher.instance.publish(origReserve)
    }

    @Transactional
    fun deleteOrArchive(reserveId: UUID) {
        balanceService.deleteOrArchiveBalance(reserveId)
    }
}