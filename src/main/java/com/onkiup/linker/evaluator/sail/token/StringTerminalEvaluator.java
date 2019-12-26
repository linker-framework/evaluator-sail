package com.onkiup.linker.evaluator.sail.token;

import com.onkiup.linker.grammar.sail.token.StringTerminal;

public class StringTerminalEvaluator implements SailEvaluator<StringTerminal, String> {
  @Override
  public String evaluate() {
    return base().toString();
  }
}

