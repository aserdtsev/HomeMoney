package ru.serdtsev.homemoney.infra.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@TestConfiguration
public class FlywayConfig {
    @Bean(name = "flyway", initMethod = "migrate")
    public Flyway flyway(@Qualifier("dataSource") DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .mixed(true)
                .locations("classpath:db/migration.")
                .schemas("public")
                .load();
    }
}
