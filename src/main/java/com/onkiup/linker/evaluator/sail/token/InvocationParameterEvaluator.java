package com.onkiup.linker.evaluator.sail.token;

import com.onkiup.linker.evaluator.api.Evaluator;
import com.onkiup.linker.grammar.sail.token.InvocationParameter;

public class InvocationParameterEvaluator<X> implements SailEvaluator<InvocationParameter, X> {
  @Override
  public X evaluate() {
    return (X)base().token().as(Evaluator.class).value();
  }
}

