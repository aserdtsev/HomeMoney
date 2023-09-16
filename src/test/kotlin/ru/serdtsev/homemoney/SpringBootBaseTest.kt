package ru.serdtsev.homemoney

import com.fasterxml.jackson.databind.ObjectMapper
import com.fatboyindustrial.gsonjavatime.Converters
import com.google.gson.GsonBuilder
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.junit.jupiter.Testcontainers
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.domain.repository.RepositoryRegistry
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import ru.serdtsev.homemoney.infra.config.FlywayConfiguration
import ru.serdtsev.homemoney.infra.config.PostgreSqlConfiguration
import ru.serdtsev.homemoney.infra.dao.BalanceSheetDao

@SpringBootTest(classes = [Main::class, PostgreSqlConfiguration::class, FlywayConfiguration::class])
@Testcontainers
abstract class SpringBootBaseTest {
    protected val objectMapper = ObjectMapper()

    @Autowired
    protected lateinit var domainEventPublisher: DomainEventPublisher
    @Autowired
    protected lateinit var repositoryRegistry: RepositoryRegistry
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