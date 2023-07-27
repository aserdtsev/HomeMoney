package ru.serdtsev.homemoney.domain.event

import jakarta.annotation.PostConstruct
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
final class DomainEventPublisher(private val applicationEventPublisher: ApplicationEventPublisher) {

    @PostConstruct
    fun init() {
        instance = this
    }

    fun publish(domainEvent: DomainEvent) {
        applicationEventPublisher.publishEvent(domainEvent)
    }

    companion object {
        lateinit var instance: DomainEventPublisher
    }
}
