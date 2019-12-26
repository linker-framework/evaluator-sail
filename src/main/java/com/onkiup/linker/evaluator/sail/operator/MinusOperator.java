package com.onkiup.linker.evaluator.sail.operator;

import static com.onkiup.linker.evaluator.sail.operator.MathUtils.allOf;
import static com.onkiup.linker.evaluator.sail.operator.MathUtils.anyOf;
import static com.onkiup.linker.evaluator.sail.operator.MathUtils.firstOf;

public class MinusOperator {

  public static <L, R, X> X apply(L leftOperand, R rightOperand) {
    Object result = null;
    if (leftOperand == null || rightOperand == null) {
      return null;
    }

    if (anyOf(String.class, leftOperand, rightOperand)) {
      throw new IllegalArgumentException("Unable to use minus operator on strings");
    }

    if (allOf(Number.class, leftOperand, rightOperand)) {

      Number left = (Number) leftOperand;
      Number right = (Number) rightOperand;

      if (anyOf(Double.class, left, right)) {
        return (X)(Object)(left.doubleValue() - right.doubleValue());
      }

      if (anyOf(Float.class, left, right)) {
        return (X)(Object)(left.floatValue() - right.floatValue());
      }

      if (anyOf(Long.class, left, right)) {
        return (X)(Object)(left.longValue() - right.longValue());
      }

      if(anyOf(Integer.class, left, right)) {
        return (X)(Object)(left.intValue() - right.intValue());
      }

      if(anyOf(Byte.class, left, right)) {
        return (X)(Object)(left.byteValue() - right.byteValue());
      }
    }

    Overloader overloader = firstOf(Overloader.class, leftOperand, rightOperand);
    if (overloader != null) {
      if (overloader == leftOperand) {
        return (X) overloader.sub(rightOperand);
      }
      return (X) overloader.subFrom(leftOperand);
    }

    throw new RuntimeException("Unsupported operand types: " + leftOperand.getClass() + " and " + rightOperand.getClass());
  }

  public static interface Overloader {
    Object sub(Object other);
    Object subFrom(Object other);
  }
}

