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

  public HmException(HmException.Code code, String message) {
    super(message);
    this.code = code;
  }

  public enum Code {
    WrongAuth,
    UnknownAccountTypeCode,
    UnknownMoneyTrnStatus,
    IdentifiersDoNotMatch,
    WrongAmount,
    UserIdCookieIsEmpty
  }

  @Override
  public String toString() {
    return "HmException{" +
        "code=" + code +
        "} " + super.toString();
  }
}
