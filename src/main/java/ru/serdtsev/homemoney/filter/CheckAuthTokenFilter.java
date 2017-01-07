package ru.serdtsev.homemoney.filter;

import org.apache.catalina.connector.RequestFacade;
import ru.serdtsev.homemoney.HmException;
import ru.serdtsev.homemoney.dao.UsersDao;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
@WebFilter("/api/*")
public class CheckAuthTokenFilter implements Filter {

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    String path = ((RequestFacade) request).getRequestURI();
    if (!path.contains("api/user/login")) {
      List<Cookie> cookies = Arrays.asList(((RequestFacade) request).getCookies());
      Optional<Cookie> userIdCookie = cookies.stream().filter(c -> "userId".equals(c.getName())).findFirst();
      Optional<Cookie> authTokenCookie = cookies.stream().filter(c -> "authToken".equals(c.getName())).findFirst();
      if (!userIdCookie.isPresent() || !authTokenCookie.isPresent()) {
        throw new HmException(HmException.Code.WrongAuth);
      }
      UsersDao.checkAuthToken(
          UUID.fromString(userIdCookie.get().getValue()),
          UUID.fromString(authTokenCookie.get().getValue()));
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }
}
