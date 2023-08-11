package ru.serdtsev.homemoney.domain

import com.nhaarman.mockito_kotlin.mock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.domain.repository.RepositoryRegistry
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import kotlin.reflect.jvm.isAccessible

open class DomainBaseTest {
    protected val domainEventPublisher: DomainEventPublisher = mock { }
    private val beforeDomainEventPublisher =
        if (DomainEventPublisher.Companion::instance.isAccessible) DomainEventPublisher.instance else null
    protected val repositoryRegistry: RepositoryRegistry = mock { }
    private val beforeRepositoryRegistry =
        if (RepositoryRegistry.Companion::instance.isAccessible) RepositoryRegistry.instance else null
    protected val balanceSheet = BalanceSheet().apply {
        ApiRequestContextHolder.balanceSheet = this
    }

    @BeforeEach
    open internal fun setUp() {
        DomainEventPublisher.instance = domainEventPublisher
        RepositoryRegistry.instance = repositoryRegistry
    }

    @AfterEach
    internal fun tearDown() {
        beforeDomainEventPublisher?.let {
            DomainEventPublisher.instance = beforeDomainEventPublisher
        }
        beforeRepositoryRegistry?.let {
            RepositoryRegistry.instance = beforeRepositoryRegistry
        }
    }
}