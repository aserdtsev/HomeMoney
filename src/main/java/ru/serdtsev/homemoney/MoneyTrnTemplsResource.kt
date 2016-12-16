package ru.serdtsev.homemoney

import org.springframework.web.bind.annotation.*
import ru.serdtsev.homemoney.dao.MoneyTrnTemplsDao
import ru.serdtsev.homemoney.dto.HmResponse
import ru.serdtsev.homemoney.dto.MoneyTrn
import ru.serdtsev.homemoney.dto.MoneyTrnTempl
import java.util.*

@RestController
@RequestMapping("/api/{bsId}/money-trn-templs")
class MoneyTrnTemplsResource {
  @RequestMapping
  fun getList(
      @PathVariable bsId: UUID,
      @RequestParam search: String?): HmResponse {
    try {
      val list = MoneyTrnTemplsDao.getMoneyTrnTempls(bsId, search)
      return HmResponse.getOk(list)
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
    }

  }

  @RequestMapping("/create")
  fun create(
      @PathVariable bsId: UUID,
      @RequestBody moneyTrn: MoneyTrn): HmResponse {
    val nextDate = MoneyTrnTempl.calcNextDate(moneyTrn.trnDate!!, moneyTrn.period!!)
    val templ = MoneyTrnTempl(UUID.randomUUID(), moneyTrn.id!!, moneyTrn.id!!, nextDate,
        moneyTrn.period!!, moneyTrn.fromAccId!!, moneyTrn.toAccId!!, moneyTrn.amount!!,
        moneyTrn.comment, moneyTrn.labels)
    MoneyTrnTemplsDao.createMoneyTrnTempl(bsId, templ)
    return HmResponse.getOk()
  }

  @RequestMapping("/skip")
  fun skip(
      @PathVariable bsId: UUID,
      @RequestBody templ: MoneyTrnTempl): HmResponse {
    templ.nextDate = MoneyTrnTempl.calcNextDate(templ.nextDate!!, templ.period!!)
    MoneyTrnTemplsDao.updateMoneyTrnTempl(bsId, templ)
    return HmResponse.getOk()
  }

  @RequestMapping("/delete")
  fun delete(
      @PathVariable bsId: UUID,
      @RequestBody templ: MoneyTrnTempl): HmResponse =
    try {
      MoneyTrnTemplsDao.deleteMoneyTrnTempl(bsId, templ.id!!)
      HmResponse.getOk()
    } catch (e: HmException) {
      HmResponse.getFail(e.getCode())
    }

  @RequestMapping("/update")
  fun updateTempl(
      @PathVariable bsId: UUID,
      @RequestBody templ: MoneyTrnTempl): HmResponse =
    try {
      MoneyTrnTemplsDao.updateMoneyTrnTempl(bsId, templ)
      HmResponse.getOk()
    } catch (e: HmException) {
      HmResponse.getFail(e.getCode())
    }

}
