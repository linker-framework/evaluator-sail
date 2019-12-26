package com.onkiup.linker.evaluator.sail.operator;

import java.util.Arrays;

public class MathUtils {
  public static boolean anyOf(Class type, Object... any) {
    return Arrays.stream(any)
        .anyMatch(item -> type.isAssignableFrom(item.getClass()));
  }

  public static boolean allOf(Class type, Object ... any) {
    return Arrays.stream(any)
        .allMatch(item -> type.isAssignableFrom(item.getClass()));
  }

  public static <O> O firstOf(Class<O> type, Object... any) {
    return (O) Arrays.stream(any)
        .filter(item -> type.isAssignableFrom(item.getClass()))
        .findFirst()
        .orElse(null);
  }
}
