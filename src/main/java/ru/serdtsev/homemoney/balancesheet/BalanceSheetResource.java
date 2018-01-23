package ru.serdtsev.homemoney.balancesheet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.common.HmException;
import ru.serdtsev.homemoney.common.HmResponse;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BalanceSheetResource {
  private final StatService statService;

  @RequestMapping("/api/{bsId}/bs-stat")
  @Transactional(readOnly = true)
  public HmResponse getBalanceSheetInfo(
      @PathVariable UUID bsId,
      @RequestParam(defaultValue = "30") Long interval) {
    log.info("bs-stat start");
    HmResponse response;
    try {
      response = HmResponse.getOk(statService.getBsStat(bsId, interval));
    } catch (HmException e) {
      response = HmResponse.getFail("INCORRECT_AUTH_TOKEN");
    }
    log.info("bs-stat end");
    return response;
  }
}
