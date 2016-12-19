package ru.serdtsev.homemoney.utils;

public class Utils {
  public static <T> T nvl(T value1, T value2) {
    if (value1 != null)
      return value1;
    else
      return value2;
  }
}
