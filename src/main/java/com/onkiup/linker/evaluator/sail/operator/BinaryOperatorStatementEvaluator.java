package com.onkiup.linker.evaluator.sail.operator;

import java.io.Serializable;

import com.onkiup.linker.evaluator.sail.token.SailEvaluator;
import com.onkiup.linker.grammar.sail.operator.BinaryOperatorStatement;
import com.onkiup.linker.parser.annotation.IgnoreCharacters;

@IgnoreCharacters(" \n\t\r")
public class BinaryOperatorStatementEvaluator<X> implements SailEvaluator<BinaryOperatorStatement, X>, Serializable {

  @Override
  public X evaluate() {
    BinaryOperatorStatement statement = base();
    SailBinaryOperatorEvaluator operator = statement.operator().as(SailBinaryOperatorEvaluator.class);

    return (X)operator.evaluate(statement.leftOperand(), statement.rightOperand());
  }
}
