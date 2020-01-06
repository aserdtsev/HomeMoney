package ru.serdtsev.homemoney.dao;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.serdtsev.homemoney.account.AccountRepository;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.moneyoper.RecurrenceOperRepo;

import javax.annotation.PostConstruct;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class MainDao {
  private static Logger log = LoggerFactory.getLogger(MainDao.class);
  @Value("${spring.datasource.driver-class-name}")
  private String jdbcDriverClassName;
  @Value("${spring.datasource.url}")
  private String jdbcUrl;
  @Value("${spring.datasource.username}")
  private String userName;
  @Value("${spring.datasource.password}")
  private String password;
  private ComboPooledDataSource cpds;
  private JdbcTemplate jdbcTemplate;

  private final AccountRepository accountRepo;
  private final RecurrenceOperRepo recurrenceOperRepo;
  private final BalanceSheetRepository balanceSheetRepo;

  @Autowired
  public MainDao(AccountRepository accountRepo, RecurrenceOperRepo recurrenceOperRepo, BalanceSheetRepository balanceSheetRepo) {
    this.accountRepo = accountRepo;
    this.recurrenceOperRepo = recurrenceOperRepo;
    this.balanceSheetRepo = balanceSheetRepo;
  }

  @PostConstruct
  private void init() {
    cpds = new ComboPooledDataSource();
    try {
      cpds.setDriverClass(jdbcDriverClassName);
      cpds.setJdbcUrl(jdbcUrl);
      cpds.setUser(userName);
      cpds.setPassword(password);
      cpds.setMinPoolSize(5);
      cpds.setMaxPoolSize(10);
      cpds.setInitialPoolSize(5);
      cpds.setAcquireIncrement(5);
      cpds.setPreferredTestQuery("select 'x' from dual");
    } catch (PropertyVetoException e) {
      log.error("Error database pool initialization", e);
    }
    jdbcTemplate = new JdbcTemplate(cpds);
  }

  public JdbcTemplate jdbcTemplate() {
    return jdbcTemplate;
  }

  public Connection getConnection() {
    try {
      Connection conn = jdbcTemplate.getDataSource().getConnection();
      conn.setAutoCommit(false);
      return conn;
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public void clearDatabase() {
    try (Connection conn = getConnection()) {
      QueryRunner run = new QueryRunner();
      run.update(conn, "delete from money_trns");
      run.update(conn, "delete from balances");
      run.update(conn, "delete from reserves");
      run.update(conn, "delete from accounts");
      run.update(conn, "delete from users");
      run.update(conn, "delete from balance_sheets");
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }
}
