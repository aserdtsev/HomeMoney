package ru.serdtsev.homemoney.patch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.moneyoper.MoneyOperRepo;

import javax.sql.DataSource;
import javax.transaction.Transactional;

@Component
public class DbPatch005 {
  private Logger log = LoggerFactory.getLogger(this.getClass());
  private final BalanceSheetRepository balanceSheetRepo;
  private final MoneyOperRepo moneyOperRepo;
  private final DataSource dataSource;

  @Autowired
  public DbPatch005(BalanceSheetRepository balanceSheetRepo, MoneyOperRepo moneyOperRepo, DataSource dataSource) {
    this.balanceSheetRepo = balanceSheetRepo;
    this.moneyOperRepo = moneyOperRepo;
    this.dataSource = dataSource;
  }

  @Transactional
  public void invoke() {
    log.info("DbPatch005 invoked");
//    try (Connection conn = dataSource.getConnection()) {
//      balanceSheetRepo.findAll().forEach(balanceSheet -> {
//        try {
//          List<RecurrenceOperDto> templs = moneyTrnsDao.getMoneyTrnTempls(conn, balanceSheet.getId(), null);
//          templs.forEach(templ -> {
//            List<MoneyOper> recurrenceOpers = moneyOperRepo
//                .findByBalanceSheetAndTemplIdAndStatusOrderByPerformed(balanceSheet, templ.getId(), MoneyOperStatus.done)
//                .collect(Collectors.toList());
//            if (!recurrenceOpers.isEmpty()) {
//              MoneyOper firstOper = recurrenceOpers.get(0);
//              UUID recurrenceId = firstOper.getId();
//              log.info("recurrenceId = '{}'", recurrenceId);
//
//              MoneyOper prevOper = null;
//              Iterator<MoneyOper> iterator = recurrenceOpers.iterator();
//              while(iterator.hasNext()) {
//                MoneyOper oper = iterator.next();
//                oper.setRecurrenceId(recurrenceId);
//                oper.setTemplateId(prevOper.getId());
//                moneyOperRepo.save(oper);
//                prevOper = oper;
//              }
//
//              MoneyOper lastOper = recurrenceOpers.get(recurrenceOpers.size()-1);
////              lastOper.setNextDate(templ.getNextDate());
////              lastOper.setTemplate(true);
//            }
//          });
//        } catch (SQLException e) {
//          e.printStackTrace();
//        }
//      });
//    } catch (SQLException e) {
//      e.printStackTrace();
//    }
  }
}
