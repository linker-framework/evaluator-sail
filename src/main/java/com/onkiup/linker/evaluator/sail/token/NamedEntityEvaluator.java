package com.onkiup.linker.evaluator.sail.token;

import com.onkiup.linker.grammar.sail.token.NamedEntity;

public class NamedEntityEvaluator<X> implements SailEvaluator<NamedEntity,X> {
  @Override
  public X evaluate() {
    return (X)creationContext().resolve(base().toString());
  }
}
