package ru.serdtsev.homemoney.dto

class HmResponse(var status: String, var data: Any?) {
  companion object {
    val ok: HmResponse
      get() = getOk(null)

    fun getOk(data: Any?): HmResponse {
      return HmResponse("OK", data)
    }

    @JvmOverloads
    fun getFail(status: String, data: Any? = null): HmResponse {
      return HmResponse(status, data)
    }
  }
}
