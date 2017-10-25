package ru.serdtsev.homemoney;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.common.HmException;
import ru.serdtsev.homemoney.common.HmResponse;
import ru.serdtsev.homemoney.dao.MainDao;

import java.util.UUID;

@RestController
public class BalanceSheetResource {
  private MainDao mainDao;

  @Autowired
  public BalanceSheetResource(MainDao mainDao) {
    this.mainDao = mainDao;
  }

  @RequestMapping("/api/{bsId}/bs-stat")
  @Transactional(readOnly = true)
  public HmResponse getBalanceSheetInfo(
      @PathVariable UUID bsId,
      @RequestParam(defaultValue = "30") Long interval) {
    HmResponse response;
    try {
      response = HmResponse.getOk(mainDao.getBsStat(bsId, interval));
    } catch (HmException e) {
      response = HmResponse.getFail("INCORRECT_AUTH_TOKEN");
    }
    return response;
  }
}
