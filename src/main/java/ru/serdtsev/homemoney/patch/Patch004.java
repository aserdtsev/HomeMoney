package ru.serdtsev.homemoney.patch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.moneyoper.model.Label;
import ru.serdtsev.homemoney.moneyoper.LabelRepository;
import ru.serdtsev.homemoney.moneyoper.MoneyOperRepo;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class Patch004 {
  private Logger log = LoggerFactory.getLogger(this.getClass());
  private final MoneyOperRepo moneyOperRepo;
  private final LabelRepository labelRepo;

  @Autowired
  public Patch004(MoneyOperRepo moneyOperRepo, LabelRepository labelRepo) {
    this.moneyOperRepo = moneyOperRepo;
    this.labelRepo = labelRepo;
  }

  @Transactional
  public void invoke() {
    moneyOperRepo.findAll().forEach(oper -> {
      oper.getLabels().forEach(label -> {
        if (label.getBalanceSheet() == null) {
          BalanceSheet balanceSheet = label.getBalanceSheet() != null ? label.getBalanceSheet() : oper.getBalanceSheet();
          assert balanceSheet != null;
          // Если уже есть такая метка с balanceSheet, привяжем операцию к ней.
          Label trueLabel = labelRepo.findByBalanceSheetAndName(balanceSheet, label.getName());
          if (trueLabel == null) {
            // Не нашли такую метку. Сделаем такой меткой нашу метку.
            label.setBalanceSheet(balanceSheet);
            labelRepo.save(label);
          } else {
            oper.getLabels().remove(label);
            oper.getLabels().add(trueLabel);
            moneyOperRepo.save(oper);
          }
        }
      });
    });
//    labelRepo.findByBalanceSheetIsNull().forEach(label -> {
//      assert label.getBalanceSheet() == null : label;
//      List<Label> childLabels = labelRepo.findByRootId(label.getId()).collect(Collectors.toList());
//      if (childLabels.isEmpty()) {
//        if (moneyOperRepo.findByLabelsContains(label).count() == 0) {
//          log.info("To delete: {}", label);
//          labelRepo.delete(label);
//        }
//      }
//    });
    labelRepo.findByBalanceSheetIsNull().forEach(label -> {
      assert label.getBalanceSheet() == null : label;
      List<Label> childLabels = labelRepo.findByRootId(label.getId()).collect(Collectors.toList());
      assert !childLabels.isEmpty() : label;
      childLabels.forEach(childLabel -> {
        assert childLabel.getRootId().equals(label.getId());
        if (label.getBalanceSheet() == null) {
          BalanceSheet balanceSheet = childLabel.getBalanceSheet();
          assert balanceSheet != null : childLabel;
          Label rootLabel = labelRepo.findByBalanceSheetAndName(balanceSheet, label.getName());
          if (rootLabel == null) {
            label.setBalanceSheet(balanceSheet);
            labelRepo.save(label);
          } else {
            childLabel.setRootId(rootLabel.getId());
            labelRepo.save(childLabel);
          }
        }
      });
    });
  }

}
