package ru.serdtsev.homemoney;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.account.AccountsDao;
import ru.serdtsev.homemoney.account.model.Category;
import ru.serdtsev.homemoney.account.CategoryRepository;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.common.HmException;
import ru.serdtsev.homemoney.common.HmResponse;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/{bsId}/categories")
public class CategoriesResource {
  private final BalanceSheetRepository balanceSheetRepo;
  private final CategoryRepository categoryRepo;
  private final AccountsDao accountsDao;

  @Autowired
  public CategoriesResource(BalanceSheetRepository balanceSheetRepo, CategoryRepository categoryRepo, AccountsDao accountsDao) {
    this.balanceSheetRepo = balanceSheetRepo;
    this.categoryRepo = categoryRepo;
    this.accountsDao = accountsDao;
  }

  @RequestMapping
  @Transactional(readOnly = true)
  public HmResponse getCategoryList(@PathVariable UUID bsId) {
    BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);
    List<Category> categories = ((List<Category>) categoryRepo.findByBalanceSheet(balanceSheet)).stream()
        .sorted(Comparator.comparing(Category::getSortIndex))
        .collect(Collectors.toList());
    return HmResponse.getOk(categories);
  }

  @RequestMapping({"/create"})
  public HmResponse createCategory(
      @PathVariable UUID bsId,
      @RequestBody Category category) {
    try {
      BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);
      category.setBalanceSheet(balanceSheet);
      categoryRepo.save(category);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping({"/update"})
  public HmResponse updateCategory(
      @PathVariable UUID bsId,
      @RequestBody Category category) {
    try {
      category.init(categoryRepo);
      categoryRepo.save(category);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping({"/delete"})
  public HmResponse deleteCategory(
      @PathVariable UUID bsId,
      @RequestBody Category category) {
    try {
      if (!accountsDao.isTrnExists(category.getId())) {
        categoryRepo.delete(category);
      }
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }
}
