package com.onkiup.linker.evaluator.sail.builtin;

import com.onkiup.linker.evaluator.sail.SailRef;
import com.onkiup.linker.evaluator.sail.operator.PlusOperator;

public class MathFunctions {

  private static PlusOperator plusOperator = new PlusOperator();

  @SailRef({"sum", "fn!sum"})
  public static Number fnSum(Number left, Number right) {
    return (Number) plusOperator.apply(left, right);
  }
}

