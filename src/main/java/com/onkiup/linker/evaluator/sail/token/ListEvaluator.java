package com.onkiup.linker.evaluator.sail.token;

import java.util.Arrays;

import com.onkiup.linker.evaluator.api.Evaluator;
import com.onkiup.linker.grammar.sail.token.LisaListToken;
import com.onkiup.linker.util.TypeUtils;

public class ListEvaluator<X> implements SailEvaluator<LisaListToken, X[]> {

  @Override
  public X[] evaluate() {
    return (X[]) Arrays.stream(base().items())
        .map(LisaListToken.Member::token)
        .map(token -> token.as(Evaluator.class).value())
        .toArray();
  }

  public Class<X> componentType() {
    return (Class<X>)TypeUtils.typeParameter(getClass(), ListEvaluator.class, 0);
  }
}

