package ru.serdtsev.homemoney;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.account.*;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.dao.AccountsDao;
import ru.serdtsev.homemoney.dao.MoneyTrnTemplsDao;
import ru.serdtsev.homemoney.dao.MoneyTrnsDao;
import ru.serdtsev.homemoney.dto.HmResponse;
import ru.serdtsev.homemoney.dto.MoneyTrn;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/{bsId}/balances")
public final class BalancesResource {
  private BalanceSheetRepository balanceSheetRepo;
  private BalanceRepository balanceRepo;
  private AccountRepository accountRepo;
  private MoneyTrnsDao moneyTrnsDao;

  @Autowired
  public BalancesResource(BalanceSheetRepository balanceSheetRepo, BalanceRepository balanceRepo,
      AccountRepository accountRepo, MoneyTrnsDao moneyTrnsDao) {
    this.balanceSheetRepo = balanceSheetRepo;
    this.balanceRepo = balanceRepo;
    this.accountRepo = accountRepo;
    this.moneyTrnsDao = moneyTrnsDao;
  }

  @RequestMapping
  public final HmResponse getBalances(@PathVariable UUID bsId) {
    BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);
    List<Balance> balances = ((List<Balance>) balanceRepo.findByBalanceSheet(balanceSheet)).stream()
        .filter(balance -> balance.getType() != AccountType.reserve)
        .sorted(Comparator.comparing(Balance::getNum))
        .collect(Collectors.toList());
    return HmResponse.getOk(balances);
  }

  @RequestMapping("/create")
  public final HmResponse createBalance(
      @PathVariable UUID bsId,
      @RequestBody BalanceDto balanceDto) {
    try {
      BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);
      Balance balance = new Balance(balanceSheet, balanceDto.getType(), balanceDto.getName(), balanceDto.getCreatedDate(),
          balanceDto.getIsArc(), balanceDto.getCurrencyCode(), balanceDto.getValue(), balanceDto.getMinValue());
      balanceRepo.save(balance);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/update")
  public final HmResponse updateBalance(
      @PathVariable UUID bsId,
      // todo Заменить BalanceDto на Balance.
      @RequestBody BalanceDto balanceDto) {
    try {
      Balance balance = balanceRepo.findOne(balanceDto.getId());
      balance.setType(balanceDto.getType());
      balance.setName(balanceDto.getName());
      balance.setCreated(balanceDto.getCreatedDate());
      balance.setArc(balanceDto.getIsArc());
      balance.setCreditLimit(balanceDto.getCreditLimit());
      balance.setMinValue(balanceDto.getMinValue());
      balance.setReserve(balanceDto.getReserveId() != null
          ? (Reserve) accountRepo.findOne(balanceDto.getReserveId())
          : null);

      if (balanceDto.getValue().compareTo(balance.getValue()) != 0) {
        BalanceSheet bs = balance.getBalanceSheet();
        boolean more = balanceDto.getValue().compareTo(balance.getValue()) == 1;
        UUID fromAccId = more ? bs.getUncatIncome().getId() : balanceDto.getId();
        UUID toAccId = more ? balanceDto.getId() : bs.getUncatCosts().getId();
        BigDecimal amount = balanceDto.getValue().subtract(balance.getValue()).abs();
        MoneyTrn moneyTrn = new MoneyTrn(UUID.randomUUID(), MoneyTrn.Status.done, java.sql.Date.valueOf(LocalDate.now()),
            fromAccId, toAccId, amount, MoneyTrn.Period.single, "корректировка остатка");
        moneyTrnsDao.createMoneyTrn(bsId, moneyTrn);
        // todo После полного перехода на JPA обновлять баланс здесь будет не нужно - он будет обновлен при проводке операции.
        balance.setValue(balanceDto.getValue());
      }

      balanceRepo.save(balance);

      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/delete")
  public final HmResponse deleteBalance(
      @PathVariable UUID bsId,
      @RequestBody BalanceDto balanceDto) {
    try {
      Balance balance = balanceRepo.findOne(balanceDto.getId());
      if (!AccountsDao.isTrnExists(balance.getId()) && !MoneyTrnTemplsDao.isTrnTemplExists(balance.getId())) {
        balanceRepo.delete(balance);
      }
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/up")
  public final HmResponse upBalance(
      @PathVariable UUID bsId,
      @RequestBody BalanceDto balanceDto) {
    try {
      Balance balance = balanceRepo.findOne(balanceDto.getId());
      BalanceSheet bs = balanceSheetRepo.findOne(bsId);

      List<Balance> balances = bs.getBalances().stream()
          .sorted((b1, b2) -> b1.getNum() < b2.getNum() ? -1 : (b1.getNum() > b2.getNum() ? 1 : 0))
          .collect(Collectors.toList());
      assert !balances.isEmpty();

      Balance prev;
      do {
        int index = balances.indexOf(balance);
        assert index > -1;
        prev = null;
        if (index > 0) {
          prev = balances.get(index - 1);
          balances.set(index - 1, balance);
          balances.set(index, prev);
          long i = 0;
          for (Balance b : balances) {
            b.setNum(i++);
          }
        }
      } while (prev != null && prev.getArc());
      balances.forEach(b -> balanceRepo.save(b));
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }
}
