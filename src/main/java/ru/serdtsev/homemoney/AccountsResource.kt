package ru.serdtsev.homemoney

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.dao.AccountsDao
import ru.serdtsev.homemoney.dto.HmResponse
import java.util.*

@RestController
class AccountsResource {
  @RequestMapping("/api/{bsId}/accounts")
  fun getAccountList(@PathVariable bsId: UUID): HmResponse {
    val allAccounts = AccountsDao.getAccounts(bsId)
    return HmResponse.getOk(allAccounts)
  }
}
