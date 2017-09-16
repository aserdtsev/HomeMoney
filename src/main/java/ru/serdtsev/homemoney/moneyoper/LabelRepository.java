package ru.serdtsev.homemoney.moneyoper;

import org.springframework.data.repository.CrudRepository;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import java.util.UUID;
import java.util.stream.Stream;

public interface LabelRepository extends CrudRepository<Label, UUID> {
  Label findByBalanceSheetAndName(BalanceSheet balanceSheet, String name);
  Stream<Label> findByBalanceSheetIsNull();
  Stream<Label> findByRootId(UUID rootId);
}
