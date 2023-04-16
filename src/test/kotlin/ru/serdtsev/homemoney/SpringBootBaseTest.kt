package ru.serdtsev.homemoney

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.junit.jupiter.Testcontainers
import ru.serdtsev.homemoney.balancesheet.BalanceSheetDao
import ru.serdtsev.homemoney.config.FlywayConfiguration
import ru.serdtsev.homemoney.config.PostgreSqlConfiguration

@SpringBootTest(classes = [Main::class, PostgreSqlConfiguration::class, FlywayConfiguration::class])
@Testcontainers
abstract class SpringBootBaseTest {
    @Autowired
    protected lateinit var balanceSheetDao: BalanceSheetDao
}