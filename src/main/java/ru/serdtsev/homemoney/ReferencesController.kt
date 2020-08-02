package ru.serdtsev.homemoney;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.dao.ReferencesDao;
import ru.serdtsev.homemoney.common.HmResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/{bsId}/references")
public class ReferencesResource {
  private final ReferencesDao referencesDao;

  @Autowired
  public ReferencesResource(ReferencesDao referencesDao) {
    this.referencesDao = referencesDao;
  }

  @RequestMapping("currencies")
  @Transactional
  public HmResponse getCurrencies(@PathVariable UUID bsId) {
    return HmResponse.getOk(referencesDao.getCurrencies(bsId));
  }
}
