package com.onkiup.linker.evaluator.sail.token;

import java.io.Serializable;

import com.onkiup.linker.evaluator.api.Evaluator;
import com.onkiup.linker.grammar.sail.token.LisaToken;
import com.onkiup.linker.grammar.sail.token.LisaValueToken;
import com.onkiup.linker.grammar.sail.token.NamedEntity;
import com.onkiup.linker.grammar.sail.token.VariableDeclaration;
import com.onkiup.linker.parser.annotation.IgnoreCharacters;

public class VariableDeclarationEvaluator<X> implements SailEvaluator<VariableDeclaration, X> {

  public X evaluate() {
    LisaValueToken valueToken = base().token();
    NamedEntity name = base().name();
    X value = (X) valueToken.as(Evaluator.class).evaluate();

    context().store(name.toString(), value);

    return value;
  }

}

