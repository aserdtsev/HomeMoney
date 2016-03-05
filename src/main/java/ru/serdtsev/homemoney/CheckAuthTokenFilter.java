package ru.serdtsev.homemoney;

import org.glassfish.jersey.server.ContainerRequest;
import ru.serdtsev.homemoney.dao.UsersDao;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Provider
public class CheckAuthTokenFilter implements ContainerRequestFilter {
  @Override
  public void filter(ContainerRequestContext requestCtx) throws IOException {
    String path = ((ContainerRequest) requestCtx).getAbsolutePath().getPath();
    if (!path.contains("api/user/login")) {
      Map<String, Cookie> cookieMap = requestCtx.getCookies();
      try {
        Optional<Cookie> userIdCookie = Optional.ofNullable(cookieMap.get("userId"));
        Optional<Cookie> authTokenCookie = Optional.ofNullable(cookieMap.get("authToken"));
        if (!userIdCookie.isPresent() || !authTokenCookie.isPresent()) {
          throw new HmException(HmException.Code.AuthWrong);
        }
        UsersDao.INSTANCE.checkAuthToken(
            UUID.fromString(userIdCookie.get().getValue()),
            UUID.fromString(authTokenCookie.get().getValue()));
      } catch (HmException e) {
        requestCtx.abortWith(Response
            .status(Response.Status.UNAUTHORIZED)
            .entity("User cannot access the resource.")
            .build());
      }
    }
  }
}
