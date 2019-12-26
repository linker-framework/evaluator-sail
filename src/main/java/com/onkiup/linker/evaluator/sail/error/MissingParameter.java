package com.onkiup.linker.evaluator.sail.error;

import com.onkiup.linker.evaluator.common.EvaluationError;

public class MissingParameter extends EvaluationError {

  public MissingParameter(String name) {
    super("Missing parameter: '" + name + "'");
  }
  
}
