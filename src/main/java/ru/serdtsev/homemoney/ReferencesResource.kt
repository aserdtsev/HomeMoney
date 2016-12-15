package ru.serdtsev.homemoney

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.dao.ReferencesDao
import ru.serdtsev.homemoney.dto.HmResponse
import java.util.*

@RestController
@RequestMapping("/api/{bsId}/references")
class ReferencesResource {
  @RequestMapping("currencies")
  fun getCurrencies(@PathVariable("bsId") bsId: UUID): HmResponse = HmResponse.getOk(ReferencesDao.getCurrencies(bsId))
}