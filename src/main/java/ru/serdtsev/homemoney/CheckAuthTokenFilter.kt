package ru.serdtsev.homemoney

import org.glassfish.jersey.server.ContainerRequest
import ru.serdtsev.homemoney.dao.UsersDao
import java.io.IOException
import java.util.*
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.core.Cookie
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider

@Provider
class CheckAuthTokenFilter : ContainerRequestFilter {
  @Throws(IOException::class)
  override fun filter(requestCtx: ContainerRequestContext) {
    val path = (requestCtx as ContainerRequest).absolutePath.path
    if (!path.contains("api/user/login")) {
      val cookieMap = requestCtx.getCookies()
      try {
        val userIdCookie = Optional.ofNullable<Cookie>(cookieMap["userId"])
        val authTokenCookie = Optional.ofNullable<Cookie>(cookieMap["authToken"])
        if (!userIdCookie.isPresent || !authTokenCookie.isPresent) {
          throw HmException(HmException.Code.AuthWrong)
        }
        UsersDao.checkAuthToken(
            UUID.fromString(userIdCookie.get().value),
            UUID.fromString(authTokenCookie.get().value))
      } catch (e: HmException) {
        requestCtx.abortWith(Response
            .status(Response.Status.UNAUTHORIZED)
            .entity("User cannot access the resource.")
            .build())
      }

    }
  }
}
