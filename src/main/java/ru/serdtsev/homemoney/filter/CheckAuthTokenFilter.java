package ru.serdtsev.homemoney.filter;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.catalina.connector.RequestFacade;
import org.springframework.beans.factory.annotation.Autowired;
import ru.serdtsev.homemoney.common.HmException;
import ru.serdtsev.homemoney.user.UserAuthToken;
import ru.serdtsev.homemoney.user.UserAuthTokenRepository;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("unused")
@WebFilter("/api/*")
public class CheckAuthTokenFilter implements Filter {
  private UserAuthTokenRepository userAuthTokenRepo;

  @Autowired
  public CheckAuthTokenFilter(UserAuthTokenRepository userAuthTokenRepo) {
    this.userAuthTokenRepo = userAuthTokenRepo;
  }

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
        return;
      }

      UUID userId = UUID.fromString(userIdCookie.get().getValue());
      UUID authToken = UUID.fromString(authTokenCookie.get().getValue());
      checkAuthToken(userId, authToken);
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }

  private void checkAuthToken(UUID userId, UUID authToken) {
    Cache authTokensCache = getAuthTokensCache();
    Element element = authTokensCache.get(userId);
    if (element != null) {
      Set authTokens = (Set)element.getObjectValue();
      if (authTokens.contains(authToken)) {
        return;
      }
    }

    UserAuthToken userAuthToken = userAuthTokenRepo.findOne(authToken);
    if (Objects.isNull(userAuthToken) || !Objects.equals(userAuthToken.getUserId(), userId)) {
      throw new HmException(HmException.Code.WrongAuth);
    }

    if (element == null) {
      element = new Element(userId, new HashSet<UUID>());
    }

    @SuppressWarnings("unchecked")
    Set<UUID> authTokens = (Set<UUID>) element.getObjectValue();
    if (authTokens.isEmpty()) {
      authTokensCache.put(element);
    }
    authTokens.add(authToken);
  }

  private Cache getAuthTokensCache() {
    return CacheManager.getInstance().getCache("authTokens");
  }
}
