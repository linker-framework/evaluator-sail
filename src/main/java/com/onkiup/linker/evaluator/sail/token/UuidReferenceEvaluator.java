package com.onkiup.linker.evaluator.sail.token;

import com.onkiup.linker.evaluator.sail.util.LisaError;
import com.onkiup.linker.grammar.sail.token.UuidReference;

public class UuidReferenceEvaluator<X> implements SailEvaluator<UuidReference, X> {
  @Override
  public X evaluate() {
    String name = base().name();
    return (X)context().resolve(name).orElseThrow(() -> new LisaError("Failed to resolve UUID '" + name + "'"));
  }
}
