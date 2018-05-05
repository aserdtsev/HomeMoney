package ru.serdtsev.homemoney.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
  @Nonnull
  public static <T> T nvl(@Nullable T value1, @Nullable T value2) {
    assert value2 != null;
    if (value1 != null)
      return value1;
    else
      return value2;
  }

  public static void assertNonNulls(Object... objects) {
    if (objects != null) {
      Stream.of(objects).forEachOrdered(obj -> {
        assert obj != null : nullValuesMsg(objects);
      });
    } else {
      assert false : nullValuesMsg(objects);
    }
  }

  public static String nullValuesMsg(Object... objects) {
    List<String> nullValues = new ArrayList<>();
    if (objects == null) {
      nullValues.add("0");
    } else {
      for (int i = 0; i < objects.length; i++) {
        if (objects[i] == null) {
          nullValues.add(Integer.toString(i));
        }
      }
    }
    String nullValuesStr = nullValues.stream().collect(Collectors.joining(", ", "", "."));
    return nullValuesStr.equals(".") ? null : "Null values: " + nullValuesStr;
  }
}
