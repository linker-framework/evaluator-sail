package com.onkiup.linker.evaluator.sail.builtin;

import java.util.Arrays;

import com.onkiup.linker.evaluator.api.Evaluator;
import com.onkiup.linker.evaluator.api.Invoker;
import com.onkiup.linker.evaluator.sail.SailRef;
import com.onkiup.linker.evaluator.sail.Variant;
import com.onkiup.linker.evaluator.sail.token.ListEvaluator;

public class LogicalFunctions {

  @SailRef({"all", "fn!all"})
  public static <I> Boolean fnAll(Invoker<Boolean> predicate, ListEvaluator<I> listToken, Evaluator<?>... context) {
    Evaluator<?>[] predicateArguments = new Evaluator[context.length + 1];
    System.arraycopy(context, 0, predicateArguments, 1, context.length);

    I[] list = listToken.value();
    if (list == null) {
      return null;
    }

    Variant<I> zero = new Variant<>(null);
    predicateArguments[0] = zero;

    int listSize = list.length;
    for (int i = 0; i < listSize; i++) {
      zero.set(list[i]);
      if (!predicate.invoke(predicateArguments)) {
        return false;
      }
    }

    return true;
  }

  @SailRef({"none", "fn!none"})
  public static <I> Boolean fnNone(Invoker<Boolean> predicate, ListEvaluator<I> listToken, Evaluator<?>... context) {
    Evaluator<?>[] predicateArguments = new Evaluator[context.length + 1];
    System.arraycopy(context, 0, predicateArguments, 1, context.length);

    I[] list = listToken.value();
    if (list == null) {
      return null;
    }

    Variant<I> zero = new Variant<>(null);
    predicateArguments[0] = zero;

    int listSize = list.length;
    for (int i = 0; i < listSize; i++) {
      zero.set(list[i]);
      if (predicate.invoke(predicateArguments)) {
        return false;
      }
    }

    return true;
  }

  @SailRef({"any", "fn!any"})
  public static <I> Boolean any(Invoker<Boolean> predicate, ListEvaluator<I> listToken, Evaluator<?>... context) {
    Evaluator<?>[] predicateArguments = new Evaluator[context.length + 1];
    System.arraycopy(context, 0, predicateArguments, 1, context.length);

    I[] list = listToken.value();
    if (list == null) {
      return null;
    }

    Variant<I> zero = new Variant<>(null);
    predicateArguments[0] = zero;

    int listSize = list.length;
    for (int i = 0; i < listSize; i++) {
      zero.set(list[i]);
      if (predicate.invoke(predicateArguments)) {
        return true;
      }
    }

    return false;
  }

  @SailRef({"and", "fn!and"})
  public static Boolean fnAnd(Boolean... values) {
    for (int i = 0; i < values.length; i++) {
      if (!values[i]) {
        return false;
      }
    }
    return true;
  }

  @SailRef({"false", "fn!false"})
  public static Boolean fnFalse() {
    return false;
  }

  @SailRef({"not", "fn!not"})
  public static Variant fnNot(Boolean... value) {
    if (value == null || value.length == 0) {
      return null;
    }

    if (value.length == 1) {
      return new Variant(!value[0]);
    } else {
      return new Variant(Arrays.stream(value).map(v -> !v).toArray(Boolean[]::new));
    }
  }

  @SailRef({"or", "fn!or"})
  public static Boolean fnOr(Boolean... values) {
    return Arrays.stream(values).anyMatch(Boolean.TRUE::equals);
  }
  
  @SailRef({"true", "fn!true"})
  public static Boolean fnTrue() {
    return true;
  }

}

