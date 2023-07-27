package ru.serdtsev.homemoney.port.common

class HmResponse(val status: String, val data: Any?) {

    companion object {
        @JvmStatic
        fun getOk(): HmResponse = HmResponse("OK", null)

        @JvmStatic
        fun getOk(data: Any?): HmResponse = HmResponse("OK", data)

        @JvmStatic
        fun getFail(status: String): HmResponse = getFail(status, null)

        @JvmStatic
        fun getFail(status: String, data: Any?): HmResponse = HmResponse(status, data)
    }

}