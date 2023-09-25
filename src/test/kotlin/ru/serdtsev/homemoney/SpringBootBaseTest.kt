package ru.serdtsev.homemoney

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.junit.jupiter.Testcontainers
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.domain.repository.RepositoryRegistry
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import ru.serdtsev.homemoney.infra.config.ClockTestConfig
import ru.serdtsev.homemoney.infra.config.FlywayConfig
import ru.serdtsev.homemoney.infra.config.PostgreSqlConfig
import ru.serdtsev.homemoney.infra.dao.BalanceSheetDao
import java.time.Clock

@SpringBootTest(classes = [
    MainTest::class,
    ClockTestConfig::class,
    PostgreSqlConfig::class,
    FlywayConfig::class]
)
@Testcontainers
abstract class SpringBootBaseTest {
    protected val objectMapper = ObjectMapper()

    @Autowired
    protected lateinit var domainEventPublisher: DomainEventPublisher
    @Autowired
    protected lateinit var repositoryRegistry: RepositoryRegistry
    @Autowired
    protected lateinit var clock: Clock
    @Autowired
    protected lateinit var balanceSheetDao: BalanceSheetDao

    protected final val balanceSheet = BalanceSheet().apply {
        ApiRequestContextHolder.balanceSheet = this
    }

    @PostConstruct
    fun init() {
        ApiRequestContextHolder.requestId = "REQUEST_ID"
        DomainEventPublisher.instance = domainEventPublisher
        domainEventPublisher.publish(balanceSheet)

        RepositoryRegistry.instance = repositoryRegistry
    }
}