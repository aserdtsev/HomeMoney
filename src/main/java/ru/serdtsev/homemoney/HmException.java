package ru.serdtsev.homemoney;

public class HmException extends RuntimeException {
  public enum Code {
    AuthWrong,
    UnknownAccountTypeCode,
    UnknownMoneyTrnStatus,
    IdentifiersDoNotMatch,
    AmountWrong
  }

  private Code code;

  public HmException(Code code) {
    this.code = code;
  }

  public String getCode() {
    return code.name();
  }
}
