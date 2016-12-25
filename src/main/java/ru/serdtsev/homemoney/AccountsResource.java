package ru.serdtsev.homemoney;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.dao.AccountsDao;
import ru.serdtsev.homemoney.dto.HmResponse;

import java.util.List;
import java.util.UUID;

@RestController
public final class AccountsResource {
  @RequestMapping("/api/{bsId}/accounts")
  public final HmResponse getAccountList(@PathVariable UUID bsId) {
    List allAccounts = AccountsDao.getAccounts(bsId);
    return HmResponse.getOk(allAccounts);
  }
}
