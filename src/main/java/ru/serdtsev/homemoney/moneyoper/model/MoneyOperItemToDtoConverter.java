package ru.serdtsev.homemoney.moneyoper.model;

import lombok.val;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import ru.serdtsev.homemoney.account.BalanceRepository;

@Component
public class MoneyOperItemToDtoConverter implements Converter<MoneyOperItem, MoneyOperItemDto> {
  private final ApplicationContext appCtx;
  private BalanceRepository balanceRepo;

  public MoneyOperItemToDtoConverter(ApplicationContext appCtx) {
    this.appCtx = appCtx;
  }

  @Override
  public MoneyOperItemDto convert(MoneyOperItem item) {
    if (balanceRepo == null) {
      balanceRepo = appCtx.getBean(BalanceRepository.class);
    }
    val balance = balanceRepo.findById(item.getBalanceId()).get();
    return new MoneyOperItemDto(item.getId(), item.getBalanceId(), balance.getName(), item.getValue(),
        item.getPerformed(), item.getIndex());
  }
}
