package com.onkiup.linker.evaluator.sail.token;

import com.onkiup.linker.grammar.sail.token.NumberTerminal;

public class NumberTerminalEvaluator implements SailEvaluator<NumberTerminal, Number> {
  @Override
  public Number evaluate() {
    return base().number();
  }
}

