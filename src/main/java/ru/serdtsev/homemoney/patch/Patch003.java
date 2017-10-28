package ru.serdtsev.homemoney.patch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.serdtsev.homemoney.account.*;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.moneyoper.*;

import javax.transaction.Transactional;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static ru.serdtsev.homemoney.moneyoper.MoneyOperType.*;

//@Component
public class Patch003 {
  private Logger log = LoggerFactory.getLogger(this.getClass());
  private final BalanceSheetRepository balanceSheetRepo;
  private final MoneyOperRepo moneyOperRepo;
  private final BalanceRepository balanceRepo;
  private final LabelRepository labelRepo;
  private final CategoryRepository categoryRepo;
  private final AccountRepository accountRepo;

  @Autowired
  public Patch003(BalanceSheetRepository balanceSheetRepo, MoneyOperRepo moneyOperRepo,
      BalanceRepository balanceRepo, LabelRepository labelRepo, CategoryRepository categoryRepo,
      AccountRepository accountRepo) {
    this.balanceSheetRepo = balanceSheetRepo;
    this.moneyOperRepo = moneyOperRepo;
    this.balanceRepo = balanceRepo;
    this.labelRepo = labelRepo;
    this.categoryRepo = categoryRepo;
    this.accountRepo = accountRepo;
  }

  @Transactional
  public void invoke() {
    balanceSheetRepo.findAll().forEach(balanceSheet -> {
      if (balanceSheet.getId().equals(UUID.fromString("f9b8f6b2-7b28-49b4-bf75-89b506f030c3"))) {
        log.info(balanceSheet.toString());
        moneyOperRepo.findByBalanceSheet(balanceSheet)
            .filter(oper -> oper.getItems().isEmpty())
            .forEach(oper -> {
              categoryToLabel(oper);
              createBalanceChanges(oper);
              moneyOperRepo.save(oper);
            });
      }
    });

  }

  private void createBalanceChanges(MoneyOper oper) {
    assert oper.getItems().isEmpty();
    log.info("Empty balanceChanges {}", oper);
    MoneyOperType operType = oper.getType(accountRepo);
    if (operType.equals(expense) || operType.equals(transfer)) {
      Account account = accountRepo.findOne(oper.getFromAccId());
      assert nonNull(account) : oper.getFromAccId();
      if (account instanceof Balance) {
        oper.addItem((Balance) account, oper.getAmount().negate(), oper.getPerformed());
      }
    }
    if (operType.equals(income) || operType.equals(transfer)) {
      Account account = accountRepo.findOne(oper.getToAccId());
      assert nonNull(account) : oper.getToAccId();
      if (account instanceof Balance) {
        oper.addItem((Balance) account , oper.getAmount(), oper.getPerformed());
      }
    }
  }

  private void categoryToLabel(MoneyOper oper) {
    Category category = null;
    MoneyOperType operType = oper.getType(accountRepo);
    if (operType.equals(expense)) {
      category = categoryRepo.findOne(oper.getToAccId());
    } else if (operType.equals(income)) {
      category = categoryRepo.findOne(oper.getFromAccId());
    }
    if (isNull(category)) {
      return;
    }
    BalanceSheet balanceSheet = oper.getBalanceSheet();
    Label label = labelRepo.findByBalanceSheetAndName(balanceSheet, category.getName());
    if (isNull(label)) {
      Category rootCategory = category.getRoot();
      Label rootLabel = null;
      if (nonNull(rootCategory)) {
        rootLabel = labelRepo.findByBalanceSheetAndName(balanceSheet, rootCategory.getName());
        if (isNull(rootLabel)) {
          rootLabel = new Label(UUID.randomUUID(), balanceSheet, rootCategory.getName(), null, true);
          labelRepo.save(rootLabel);
        }
      }
      UUID rootLabelId = nonNull(rootLabel) ? rootLabel.getId() : null;
      label = new Label(UUID.randomUUID(), balanceSheet, category.getName(), rootLabelId, true);
      labelRepo.save(label);
      oper.getLabels().add(label);
    }
  }
}
