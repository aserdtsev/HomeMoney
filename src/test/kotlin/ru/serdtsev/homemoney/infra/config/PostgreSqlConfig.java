package ru.serdtsev.homemoney.infra.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.util.Collections;

@TestConfiguration
public class PostgreSqlConfig {
    @Bean(destroyMethod = "stop")
    public PostgreSQLContainer<?> postgreSQLContainer() {
        PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:14.7-alpine")
                .withDatabaseName("homemoney")
                .withExposedPorts(5432)
                .withUsername("serdtsev")
                .withPassword("serdtsev")
                .withEnv("TZ", "Asia/Novosibirsk")
                .withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"));
        postgreSQLContainer.start();
        return postgreSQLContainer;
    }

    @Bean(name = "dataSource", destroyMethod = "close")
    public DataSource dataSource(PostgreSQLContainer<?> postgreSQLContainer) {
        return DataSourceBuilder.create().type(HikariDataSource.class)
                .url(postgreSQLContainer.getJdbcUrl())
                .username(postgreSQLContainer.getUsername())
                .password(postgreSQLContainer.getPassword())
                .driverClassName(postgreSQLContainer.getDriverClassName())
                .build();
    }

    @Bean
    public DataSourceTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}