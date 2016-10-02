package ru.serdtsev.homemoney

import org.apache.log4j.NDC
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.ext.Provider

@Provider
class SetLogNdcFilter : ContainerRequestFilter {
  override fun filter(requestCtx: ContainerRequestContext?) {
    val cookieMap = requestCtx!!.cookies
    val userIdCookie = cookieMap["userId"]
    if (userIdCookie != null && userIdCookie.value != null) {
      val userId = userIdCookie.value
      NDC.push("userId:$userId")
    }
  }
}