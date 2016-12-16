package ru.serdtsev.homemoney;

public class HmException extends RuntimeException {
  private final HmException.Code code;

  public final String getCode() {
    return this.code.name();
  }

  public HmException(HmException.Code code) {
    super();
    this.code = code;
  }

  public enum Code {
    WrongAuth,
    UnknownAccountTypeCode,
    UnknownMoneyTrnStatus,
    IdentifiersDoNotMatch,
    WrongAmount,
    UserIdCookieIsEmpty;
  }
}
