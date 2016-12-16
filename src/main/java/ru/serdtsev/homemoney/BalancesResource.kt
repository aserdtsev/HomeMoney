package ru.serdtsev.homemoney

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.dao.BalancesDao
import ru.serdtsev.homemoney.dto.Balance
import ru.serdtsev.homemoney.dto.HmResponse
import java.util.*

@RestController
@RequestMapping("/api/{bsId}/balances")
class BalancesResource {
  @RequestMapping
  fun getBalances(@PathVariable("bsId") bsId: UUID) =
    HmResponse.getOk(BalancesDao.getBalances(bsId))

  @RequestMapping("/create")
  fun createBalance(
      @PathVariable bsId: UUID,
      @RequestBody balance: Balance) =
    try {
      BalancesDao.createBalance(bsId, balance)
      HmResponse.getOk()
    } catch (e: HmException) {
      HmResponse.getFail(e.getCode())
    }

  @RequestMapping("/update")
  fun updateBalance(
      @PathVariable bsId: UUID,
      @RequestBody balance: Balance) =
    try {
      BalancesDao.updateBalance(bsId, balance)
      HmResponse.getOk()
    } catch (e: HmException) {
      HmResponse.getFail(e.getCode())
    }

  @RequestMapping("/delete")
  fun deleteBalance(
      @PathVariable bsId: UUID,
      @RequestBody balance: Balance) =
    try {
      BalancesDao.deleteBalance(bsId, balance.id!!)
      HmResponse.getOk()
    } catch (e: HmException) {
      HmResponse.getFail(e.getCode())
    }

  @RequestMapping("/up")
  fun upBalance(
      @PathVariable bsId: UUID,
      @RequestBody balance: Balance) =
    try {
      BalancesDao.upBalance(bsId, balance)
      HmResponse.getOk()
    } catch (e: HmException) {
      HmResponse.getFail(e.getCode())
    }
}
