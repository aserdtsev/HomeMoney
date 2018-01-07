package ru.serdtsev.homemoney.patch;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.val;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.serdtsev.homemoney.account.Category;
import ru.serdtsev.homemoney.account.CategoryRepository;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.moneyoper.LabelRepository;
import ru.serdtsev.homemoney.moneyoper.model.CategoryType;
import ru.serdtsev.homemoney.moneyoper.model.Label;

import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Component
@Log
@RequiredArgsConstructor
public class DbPatch006 {
  private final BalanceSheetRepository balanceSheetRepo;
  private final LabelRepository labelRepo;
  private final CategoryRepository categoryRepo;

  @Transactional
  public void invoke() {
    balanceSheetRepo.findAll().forEach(balanceSheet -> {
      log.info(balanceSheet.toString());
      categoryRepo.findByBalanceSheet(balanceSheet)
          .forEach(category -> categoryToLabel(category));
    });

  }

  private void categoryToLabel(Category category) {
    BalanceSheet balanceSheet = category.getBalanceSheet();
    Label label = labelRepo.findByBalanceSheetAndName(balanceSheet, category.getName());
    if (isNull(label)) {
      label = new Label(UUID.randomUUID(), balanceSheet, category.getName(), null, true, null);
    } else if (!label.getIsCategory()) {
      label.setIsCategory(true);
    }
    if (nonNull(category.getRoot()) && isNull(label.getRootId())) {
      Label rootLabel = getOrCreateRootLabel(category);
      UUID rootLabelId = nonNull(rootLabel) ? rootLabel.getId() : null;
      label.setRootId(rootLabelId);
    }
    if (isNull(label.getCategoryType())) {
      val categoryType = CategoryType.valueOf(category.getType().name());
      label.setCategoryType(categoryType);
    }
    labelRepo.save(label);
  }

  private Label getOrCreateRootLabel(Category category) {
    BalanceSheet balanceSheet = category.getBalanceSheet();
    Label rootLabel = null;
    Category rootCategory = category.getRoot();
    if (nonNull(rootCategory)) {
      rootLabel = labelRepo.findByBalanceSheetAndName(balanceSheet, rootCategory.getName());
      if (isNull(rootLabel)) {
        val categoryType = CategoryType.valueOf(category.getType().name());
        rootLabel = new Label(UUID.randomUUID(), balanceSheet, rootCategory.getName(), null, true, categoryType);
        labelRepo.save(rootLabel);
      }
    }
    return rootLabel;
  }
}
