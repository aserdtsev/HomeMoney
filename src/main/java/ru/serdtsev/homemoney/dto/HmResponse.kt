package ru.serdtsev.homemoney.dto

class HmResponse(val status: String, val data: Any?) {
  companion object {
    fun getOk(data: Any? = null): HmResponse {
      if (data is PagedList<*>) {
        System.out.println("getOk for PagedList")
      }
      return HmResponse("OK", data)
    }

    @JvmOverloads
    fun getFail(status: String, data: Any? = null) = HmResponse(status, data)
  }
}
