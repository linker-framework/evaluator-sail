package com.onkiup.linker.evaluator.sail.token;

import com.onkiup.linker.evaluator.api.Evaluator;
import com.onkiup.linker.grammar.sail.token.EqualsPrefix;
import com.onkiup.linker.parser.Rule;
import com.onkiup.linker.parser.annotation.IgnoreCharacters;

@IgnoreCharacters(" \r\t\n")
public class EqualsPrefixEvaluator<X> implements SailEvaluator<EqualsPrefix, X> {
  @Override
  public X evaluate() {
    Object result = base().token();
    if (result instanceof Rule) {
      return (X) ((Rule)result).as(Evaluator.class).value();
    }

    return (X) result;
  }
}
