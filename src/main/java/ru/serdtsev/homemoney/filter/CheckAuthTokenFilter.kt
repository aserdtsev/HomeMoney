package ru.serdtsev.homemoney.filter

import org.glassfish.jersey.server.ContainerRequest
import ru.serdtsev.homemoney.HmException
import ru.serdtsev.homemoney.dao.UsersDao
import java.io.IOException
import java.util.*
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
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
        val userIdCookie = cookieMap["userId"]
        val authTokenCookie = cookieMap["authToken"]
        if (userIdCookie == null || authTokenCookie == null)
          throw HmException(HmException.Code.WrongAuth)
        UsersDao.checkAuthToken(
            UUID.fromString(userIdCookie.value),
            UUID.fromString(authTokenCookie.value))
      } catch (e: HmException) {
        requestCtx.abortWith(Response
            .status(Response.Status.UNAUTHORIZED)
            .entity("User cannot access the resource.")
            .build())
      }
    }
  }
}
