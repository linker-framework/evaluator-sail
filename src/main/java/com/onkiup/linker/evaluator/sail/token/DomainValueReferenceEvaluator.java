package com.onkiup.linker.evaluator.sail.token;

import com.onkiup.linker.grammar.sail.token.DomainValueReference;

public class DomainValueReferenceEvaluator<T extends Object> implements SailEvaluator<DomainValueReference, T> {

  @Override
  public T evaluate() {
    return (T)creationContext().resolve(base().toString()).orElse(null);
  }
}

