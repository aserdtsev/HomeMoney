package ru.serdtsev.homemoney

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.dao.ReservesDao
import ru.serdtsev.homemoney.dto.Account
import ru.serdtsev.homemoney.dto.HmResponse
import ru.serdtsev.homemoney.dto.Reserve
import java.util.*

@RestController
@RequestMapping("/api/{bsId}/reserves")
class ReservesResource {
  @RequestMapping
  fun getReserveList(@PathVariable("bsId") bsId: UUID): HmResponse {
    return HmResponse.getOk(ReservesDao.getReserves(bsId))
  }

  @RequestMapping("/create")
  fun createReserve(
      @PathVariable("bsId") bsId: UUID,
      reserve: Reserve): HmResponse =
    try {
      reserve.type = Account.Type.reserve
      ReservesDao.createReserve(bsId, reserve)
      HmResponse.getOk()
    } catch (e: HmException) {
      HmResponse.getFail(e.getCode())
    }

  @RequestMapping("/update")
  fun updateReserve(
      @PathVariable("bsId") bsId: UUID,
      reserve: Reserve): HmResponse =
    try {
      ReservesDao.updateReserve(bsId, reserve)
      HmResponse.getOk()
    } catch (e: HmException) {
      HmResponse.getFail(e.getCode())
    }

  @RequestMapping("/delete")
  fun deleteReserve(
      @PathVariable("bsId") bsId: UUID,
      reserve: Reserve): HmResponse =
    try {
      ReservesDao.deleteReserve(bsId, reserve.id!!)
      HmResponse.getOk()
    } catch (e: HmException) {
      HmResponse.getFail(e.getCode())
    }
}
