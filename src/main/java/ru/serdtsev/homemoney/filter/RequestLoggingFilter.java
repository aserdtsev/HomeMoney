package ru.serdtsev.homemoney.filter;

import org.apache.log4j.NDC;
import org.springframework.web.filter.AbstractRequestLoggingFilter;

import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

@WebFilter("/api/*")
public class RequestLoggingFilter extends AbstractRequestLoggingFilter {
  @Override
  protected void beforeRequest(HttpServletRequest request, String message) {
    Optional.ofNullable(request.getCookies()).ifPresent(cookies -> {
      Optional<Cookie> userIdCookie = Arrays.stream(cookies)
          .filter(c -> "userId".equals(c.getName()))
          .findFirst();
      userIdCookie.ifPresent(c -> NDC.push("userId:" + c.getValue()));
    });
  }

  @Override
  protected void afterRequest(HttpServletRequest request, String message) {
    NDC.remove();
  }
}
