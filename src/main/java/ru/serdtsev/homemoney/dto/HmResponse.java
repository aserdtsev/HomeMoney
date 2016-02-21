package ru.serdtsev.homemoney.dto;

public class HmResponse implements java.io.Serializable {
  public String status;
  public Object data;

  @SuppressWarnings("unused")
  public HmResponse() {
  }

  private HmResponse(String status, Object data) {
    this.status = status;
    this.data = data;
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

  public static HmResponse getFail(String status, Object data) {
    return new HmResponse(status, data);
  }

}
