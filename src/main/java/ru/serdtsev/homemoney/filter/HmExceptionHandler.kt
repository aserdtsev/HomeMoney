package ru.serdtsev.homemoney.filter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.serdtsev.homemoney.common.HmException;

@ControllerAdvice
public class HmExceptionHandler extends ResponseEntityExceptionHandler {
  @ExceptionHandler(HmException.class)
  ResponseEntity<Object> handleBadRequest(HmException ex, WebRequest request) {
    return handleExceptionInternal(ex, "", new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
  }
}
