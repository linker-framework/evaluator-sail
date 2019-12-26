package com.onkiup.linker.evaluator.sail.token;

import java.util.Arrays;

import com.onkiup.linker.evaluator.api.Connector;
import com.onkiup.linker.evaluator.api.Evaluator;
import com.onkiup.linker.evaluator.api.Invoker;
import com.onkiup.linker.evaluator.common.EvaluationError;
import com.onkiup.linker.grammar.sail.token.RuleInvocation;

public class RuleInvocationEvaluator<O> implements SailEvaluator<RuleInvocation, O> {
  @Override
  public O evaluate() {
    RuleInvocation base = base();
    Object target = base.rule().as(Evaluator.class).value();

    Invoker<O> invoker = null;
    if (target instanceof Invoker) {
      invoker = (Invoker<O>)target;
    } else if (target instanceof Connector) {
      invoker = ((Connector<?, O>)target).as(Invoker.class);
    } else {
      throw new EvaluationError("Not invocable: " + target);
    }

    return invoker.invoke(Arrays.stream(base.parameters()).map(parameter -> parameter.as(Evaluator.class)).toArray(Evaluator[]::new));
  }
}
