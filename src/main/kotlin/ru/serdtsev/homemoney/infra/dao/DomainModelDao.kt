package ru.serdtsev.homemoney.infra.dao

import org.springframework.context.event.EventListener
import ru.serdtsev.homemoney.domain.event.DomainEvent

interface DomainModelDao<in T: DomainEvent> {
    @EventListener
    fun handleEvent(domainEvent: T) = save(domainEvent)
    fun save(domainAggregate: T)
}