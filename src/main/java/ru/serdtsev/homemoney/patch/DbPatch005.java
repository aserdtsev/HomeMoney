package ru.serdtsev.homemoney.patch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.dao.MoneyTrnsDao;
import ru.serdtsev.homemoney.dto.MoneyTrnTempl;
import ru.serdtsev.homemoney.moneyoper.MoneyOper;
import ru.serdtsev.homemoney.moneyoper.MoneyOperRepository;
import ru.serdtsev.homemoney.moneyoper.MoneyOperStatus;

import javax.sql.DataSource;
import javax.transaction.Transactional;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DbPatch005 {
  private Logger log = LoggerFactory.getLogger(this.getClass());
  private final BalanceSheetRepository balanceSheetRepo;
  private final MoneyOperRepository moneyOperRepo;
  private final MoneyTrnsDao moneyTrnsDao;
  private final DataSource dataSource;

  @Autowired
  public DbPatch005(BalanceSheetRepository balanceSheetRepo, MoneyOperRepository moneyOperRepo, MoneyTrnsDao moneyTrnsDao, DataSource dataSource) {
    this.balanceSheetRepo = balanceSheetRepo;
    this.moneyOperRepo = moneyOperRepo;
    this.moneyTrnsDao = moneyTrnsDao;
    this.dataSource = dataSource;
  }

  @Transactional
  public void invoke() {
    log.info("DbPatch005 invoked");
    try (Connection conn = dataSource.getConnection()) {
      balanceSheetRepo.findAll().forEach(balanceSheet -> {
        try {
          List<MoneyTrnTempl> templs = moneyTrnsDao.getMoneyTrnTempls(conn, balanceSheet.getId(), null);
          templs.forEach(templ -> {
            List<MoneyOper> recurrenceOpers = moneyOperRepo
                .findByBalanceSheetAndTemplIdAndStatusOrderByPerformed(balanceSheet, templ.getId(), MoneyOperStatus.done)
                .collect(Collectors.toList());
            if (!recurrenceOpers.isEmpty()) {
              MoneyOper firstOper = recurrenceOpers.get(0);
              UUID recurrenceId = firstOper.getId();
              log.info("recurrenceId = '{}'", recurrenceId);

              MoneyOper prevOper = null;
              Iterator<MoneyOper> iterator = recurrenceOpers.iterator();
              while(iterator.hasNext()) {
                MoneyOper oper = iterator.next();
                oper.setRecurrenceId(recurrenceId);
                oper.setTemplateOper(prevOper);
                moneyOperRepo.save(oper);
                prevOper = oper;
              }

              MoneyOper lastOper = recurrenceOpers.get(recurrenceOpers.size()-1);
              lastOper.setNextDate(templ.getNextDate());
              lastOper.setTemplate(true);
            }
          });
        } catch (SQLException e) {
          e.printStackTrace();
        }
      });
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
