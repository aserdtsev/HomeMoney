package ru.serdtsev.homemoney.common;

public class HmResponse {
  private String status;
  private Object data;

  public HmResponse(String status, Object data) {
    this.status = status;
    this.data = data;
  }

  public String getStatus() {
    return status;
  }

  public Object getData() {
    return data;
  }

  public static HmResponse getOk() {
    return getOk(null);
  }

  public static HmResponse getOk(Object data) {
    return new HmResponse("OK", data);
  }

  public static HmResponse getFail(String status) {
    return getFail(status, null);
  }

  @SuppressWarnings("WeakerAccess")
  public static HmResponse getFail(String status, @SuppressWarnings("SameParameterValue") Object data) {
    return new HmResponse(status, data);
  }
}
