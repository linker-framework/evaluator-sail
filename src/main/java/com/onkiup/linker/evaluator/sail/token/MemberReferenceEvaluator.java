package com.onkiup.linker.evaluator.sail.token;

import com.onkiup.linker.evaluator.api.Evaluator;
import com.onkiup.linker.evaluator.sail.builtin.DataFunctions;
import com.onkiup.linker.grammar.sail.token.LisaValueToken;
import com.onkiup.linker.grammar.sail.token.MemberReference;

public class MemberReferenceEvaluator<X> implements SailEvaluator<MemberReference, X> {
  @Override
  public X evaluate() {
    MemberReference base = base();
    LisaValueToken sourceToken = base.sourceToken();
    LisaValueToken keyToken = base.keyToken();
    return DataFunctions.index(sourceToken.as(Evaluator.class), keyToken.as(Evaluator.class));
  }
}
