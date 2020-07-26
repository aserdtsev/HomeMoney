package ru.serdtsev.homemoney.account;

import org.springframework.data.repository.CrudRepository;
import ru.serdtsev.homemoney.account.model.Category;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends CrudRepository<Category, UUID> {
  List<Category> findByBalanceSheet(BalanceSheet balanceSheet);
}
