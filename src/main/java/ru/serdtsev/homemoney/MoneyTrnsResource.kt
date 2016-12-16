package ru.serdtsev.homemoney

import org.springframework.web.bind.annotation.*
import ru.serdtsev.homemoney.dao.MoneyTrnsDao
import ru.serdtsev.homemoney.dto.HmResponse
import ru.serdtsev.homemoney.dto.MoneyTrn
import ru.serdtsev.homemoney.dto.PagedList
import java.sql.Date
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/{bsId}/money-trns")
class MoneyTrnsResource {
  @RequestMapping
  fun getMoneyTrns(
      @PathVariable bsId: UUID,
      @RequestParam search: String?,
      @RequestParam(defaultValue = "10") limit: Int,
      @RequestParam(defaultValue = "0") offset: Int): HmResponse {
    return try {
      val trns = ArrayList<MoneyTrn>()

      if (offset == 0) {
        val beforeDate = LocalDate.now().plusDays(14)
        val pendingTrns = MoneyTrnsDao.getPendingMoneyTrns(bsId, search, Date.valueOf(beforeDate))
        trns.addAll(pendingTrns)
      }

      // Запросим на одну операцию больше, чем нужно, чтобы понять, есть ли еще (hasNext).
      val doneTrns = MoneyTrnsDao.getDoneMoneyTrns(bsId, search, limit + 1, offset)
      val hasNext = doneTrns.size > limit
      trns.addAll(if (hasNext) doneTrns.subList(0, limit) else doneTrns)

      val pagedList = PagedList(trns, limit, offset, hasNext)
      HmResponse("OK", pagedList)
    } catch (e: HmException) {
      HmResponse.getFail(e.getCode())
    }
  }

  @RequestMapping("/item")
  fun getMoneyTrn(
      @PathVariable bsId: UUID,
      @RequestParam id: UUID): HmResponse {
    try {
      val moneyTrn = MoneyTrnsDao.getMoneyTrn(bsId, id)
      return HmResponse.getOk(moneyTrn)
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
    }

  }

  @RequestMapping("/create")
  fun createMoneyTrn(
      @PathVariable bsId: UUID,
      @RequestBody moneyTrn: MoneyTrn): HmResponse {
    return HmResponse.getOk(MoneyTrnsDao.createMoneyTrn(bsId, moneyTrn))
  }

  @RequestMapping("/delete")
  fun deleteMoneyTrn(
      @PathVariable bsId: UUID,
      @RequestBody moneyTrn: MoneyTrn): HmResponse =
    try {
      MoneyTrnsDao.deleteMoneyTrn(bsId, moneyTrn.id!!)
      HmResponse.getOk()
    } catch (e: HmException) {
      HmResponse.getFail(e.getCode())
    }

  @RequestMapping("/update")
  fun updateMoneyTrn(
      @PathVariable bsId: UUID,
      @RequestBody moneyTrn: MoneyTrn): HmResponse =
    try {
      MoneyTrnsDao.updateMoneyTrn(bsId, moneyTrn)
      HmResponse.getOk()
    } catch (e: HmException) {
      HmResponse.getFail(e.getCode())
    }

  @RequestMapping("/skip")
  fun skipMoneyTrn(
      @PathVariable bsId: UUID,
      @RequestBody moneyTrn: MoneyTrn): HmResponse =
    try {
      MoneyTrnsDao.skipMoneyTrn(bsId, moneyTrn)
      HmResponse.getOk()
    } catch (e: HmException) {
      HmResponse.getFail(e.getCode())
    }

  @RequestMapping("/up")
  fun upMoneyTrn(
      @PathVariable bsId: UUID,
      @RequestBody moneyTrn: MoneyTrn): HmResponse =
    try {
      MoneyTrnsDao.upMoneyTrn(bsId, moneyTrn.id!!)
      HmResponse.getOk()
    } catch (e: HmException) {
      HmResponse.getFail(e.getCode())
    }
}
