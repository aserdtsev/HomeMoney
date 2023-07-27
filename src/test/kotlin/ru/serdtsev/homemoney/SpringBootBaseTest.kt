package ru.serdtsev.homemoney

import jakarta.annotation.PostConstruct
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.junit.jupiter.Testcontainers
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.infra.dao.BalanceSheetDao
import ru.serdtsev.homemoney.infra.config.FlywayConfiguration
import ru.serdtsev.homemoney.infra.config.PostgreSqlConfiguration

@SpringBootTest(classes = [Main::class, PostgreSqlConfiguration::class, FlywayConfiguration::class])
@Testcontainers
abstract class SpringBootBaseTest {
    @Autowired
    protected lateinit var domainEventPublisher: DomainEventPublisher
    @Autowired
    protected lateinit var balanceSheetDao: BalanceSheetDao

    protected final val balanceSheet = BalanceSheet()

    @PostConstruct
    fun init() {
        DomainEventPublisher.instance = domainEventPublisher
        balanceSheetDao.save(balanceSheet)
    }
}