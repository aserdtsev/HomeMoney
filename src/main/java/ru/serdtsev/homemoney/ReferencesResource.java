package ru.serdtsev.homemoney;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.dao.ReferencesDao;
import ru.serdtsev.homemoney.dto.HmResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/{bsId}/references")
public final class ReferencesResource {
  @RequestMapping("currencies")
  public final HmResponse getCurrencies(@PathVariable UUID bsId) {
    return HmResponse.getOk(ReferencesDao.getCurrencies(bsId));
  }
}
