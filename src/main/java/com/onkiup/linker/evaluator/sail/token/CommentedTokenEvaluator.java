package com.onkiup.linker.evaluator.sail.token;

import com.onkiup.linker.evaluator.api.Evaluator;
import com.onkiup.linker.grammar.sail.token.CommentedLisaValueToken;
import com.onkiup.linker.parser.Rule;

public class CommentedTokenEvaluator<X> implements SailEvaluator<CommentedLisaValueToken, X> {
  @Override
  public X evaluate() {
    Object token = base().token();
    if (token instanceof Rule) {
      return (X)((Rule)token).as(Evaluator.class).value();
    }
    return (X)token;
  }
}
