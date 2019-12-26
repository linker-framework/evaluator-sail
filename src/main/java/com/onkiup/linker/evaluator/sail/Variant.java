package com.onkiup.linker.evaluator.sail;

import java.io.Serializable;

import com.onkiup.linker.evaluator.api.Evaluator;
import com.onkiup.linker.parser.annotation.IgnoreVariant;

@IgnoreVariant
public class Variant<X> implements Evaluator<X>, Serializable {

  private X value;

  public Variant(X value) {
    this.value = value;
  }

  @Override
  public X evaluate() {
    return value;
  }

  public void set(X value) {
    this.value = value;
  }
}

