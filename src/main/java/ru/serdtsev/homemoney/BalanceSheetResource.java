package ru.serdtsev.homemoney;

import org.springframework.web.bind.annotation.*;
import ru.serdtsev.homemoney.dao.MainDao;
import ru.serdtsev.homemoney.dto.HmResponse;

import java.util.UUID;

@RestController
public class BalanceSheetResource {
  @RequestMapping("/api/{bsId}/bs-stat")
  public HmResponse getBalanceSheetInfo(
      @PathVariable UUID bsId,
      @RequestParam(defaultValue = "30") Long interval) {
    HmResponse response;
    try {
      response = HmResponse.getOk(MainDao.INSTANCE.getBsStat(bsId, interval));
    } catch (HmException e) {
      response = HmResponse.getFail("INCORRECT_AUTH_TOKEN");
    }
    return response;
  }
}
