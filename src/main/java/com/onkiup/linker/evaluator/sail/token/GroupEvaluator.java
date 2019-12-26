package com.onkiup.linker.evaluator.sail.token;

import com.onkiup.linker.evaluator.api.Evaluator;
import com.onkiup.linker.grammar.sail.token.Group;

public class GroupEvaluator<X> implements SailEvaluator<Group, X> {

  @Override
  public X evaluate() {
    return (X)base().token().as(Evaluator.class).value();
  }
}

