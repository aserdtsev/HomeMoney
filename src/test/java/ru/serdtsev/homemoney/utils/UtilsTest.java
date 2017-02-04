package ru.serdtsev.homemoney.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UtilsTest {
  @Test
  public void nvl() throws Exception {
    assertEquals("A", Utils.nvl("A", "B"));
    assertEquals("B", Utils.nvl(null, "B"));
    assertEquals("A", Utils.nvl("A", null));
    assertNull(Utils.nvl(null, null));
  }

  @Test
  public void nullValuesMsg() throws Exception {
    String prefix = "Null values: ";
    assertNull(Utils.nullValuesMsg("A"));
    assertNull(Utils.nullValuesMsg("A", "B"));
    assertEquals(prefix + "0.", Utils.nullValuesMsg(null));
    assertEquals(prefix + "0.", Utils.nullValuesMsg(null, "B"));
    assertEquals(prefix + "1.", Utils.nullValuesMsg("A", null));
    assertEquals(prefix + "1, 3.", Utils.nullValuesMsg("A", null, "C", null, "E"));
  }

  @Test
  void assertNonNulls() {
    assertAll(() -> Utils.assertNonNulls("A"));
    assertAll(() -> Utils.assertNonNulls("B"));
    assertThrows(AssertionError.class, () -> Utils.assertNonNulls(null));
    assertThrows(AssertionError.class, () -> Utils.assertNonNulls(null, "B"));
    assertThrows(AssertionError.class, () -> Utils.assertNonNulls("A", null));
    assertThrows(AssertionError.class, () -> Utils.assertNonNulls("A", null, "C", null, "E"));
  }
}