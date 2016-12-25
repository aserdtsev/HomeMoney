package ru.serdtsev.homemoney;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.dao.ReservesDao;
import ru.serdtsev.homemoney.dto.Account.Type;
import ru.serdtsev.homemoney.dto.HmResponse;
import ru.serdtsev.homemoney.dto.Reserve;

import java.util.UUID;

@RestController
@RequestMapping("/api/{bsId}/reserves")
public final class ReservesResource {
  @RequestMapping
  public final HmResponse getReserveList(@PathVariable UUID bsId) {
    return HmResponse.getOk(ReservesDao.getReserves(bsId));
  }

  @RequestMapping("/create")
  public final HmResponse createReserve(
      @PathVariable UUID bsId,
      @RequestBody Reserve reserve) {
    try {
      reserve.setType(Type.reserve);
      ReservesDao.createReserve(bsId, reserve);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/update")
  public final HmResponse updateReserve(
      @PathVariable UUID bsId,
      @RequestBody Reserve reserve) {
    try {
      ReservesDao.updateReserve(bsId, reserve);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/delete")
  public final HmResponse deleteReserve(
      @PathVariable UUID bsId,
      @RequestBody Reserve reserve) {
    try {
      ReservesDao.deleteReserve(bsId, reserve.getId());
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }
}
