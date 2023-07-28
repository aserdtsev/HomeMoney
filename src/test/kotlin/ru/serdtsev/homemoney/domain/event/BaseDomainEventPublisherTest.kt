package ru.serdtsev.homemoney.domain.event

import com.nhaarman.mockito_kotlin.mock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import kotlin.reflect.jvm.isAccessible

open class BaseDomainEventPublisherTest {
    protected val domainEventPublisher: DomainEventPublisher = mock { }
    private val beforeDomainEventPublisher =
        if (DomainEventPublisher.Companion::instance.isAccessible) DomainEventPublisher.instance else null
    protected val balanceSheet = BalanceSheet().apply {
        ApiRequestContextHolder.balanceSheet = this
    }

    @BeforeEach
    open fun setUp() {
        DomainEventPublisher.instance = domainEventPublisher
    }

    @AfterEach
    internal fun tearDown() {
        if (beforeDomainEventPublisher != null) {
            DomainEventPublisher.instance = beforeDomainEventPublisher
        }
    }
}