package com.onkiup.linker.evaluator.sail.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import com.onkiup.linker.evaluator.api.EvaluationContext;
import com.onkiup.linker.evaluator.api.Evaluator;
import com.onkiup.linker.evaluator.api.Invoker;
import com.onkiup.linker.evaluator.api.RuleEvaluator;
import com.onkiup.linker.evaluator.common.EvaluationError;
import com.onkiup.linker.evaluator.sail.token.InvocationParameterEvaluator;
import com.onkiup.linker.grammar.sail.token.InvocationParameter;
import com.onkiup.linker.grammar.sail.token.NamedInvocationParameter;

public class SailRule<X> implements Invoker<X> {

  private RuleEvaluator<?, X> rule;
  private NamedInvocationParameter[] parameters;
  private EvaluationContext<?> creationContext;

  public SailRule(RuleEvaluator<?, X> rule, NamedInvocationParameter... parameters) {
    this.rule = rule;
    this.parameters = parameters;
    this.creationContext = EvaluationContext.get();
  }

  @Override
  public X execute(Evaluator... arguments) {
    return creationContext.subcontext(String.class, context -> {
      boolean metNamed = false;
      boolean metUnnamed = false;
      for (int i = 0; i < arguments.length; i++) {
        Evaluator argument = arguments[i];
        if (!InvocationParameterEvaluator.class.isInstance(argument)) {
          throw new EvaluationError("Invalid evaluator type: expected InvocationParameterEvaluator");
        }

        InvocationParameter parameter = ((InvocationParameterEvaluator<?>)argument).base();
        int finalI = i;
        NamedInvocationParameter target = findTarget(i, parameter).orElseThrow(
            () -> new EvaluationError("Unable to find target parameter for argument #" + finalI));

        context.store(String.format("ri!%s", target.name()), argument.value(), false);
      }

      return rule.value();
    });
  }

  protected Optional<NamedInvocationParameter> parameter(String name) {
    return Arrays.stream(parameters)
        .filter(NamedInvocationParameter.class::isInstance)
        .filter(parameter -> Objects.equals(parameter.name(), name))
        .findFirst();
  }

  protected Optional<NamedInvocationParameter> findTarget(int position, InvocationParameter argument) {
    if (argument instanceof NamedInvocationParameter) {
      return parameter((String)argument.name());
    } else if (position > -1 && position < parameters.length) {
      return Optional.of(parameters[position]);
    }

    return Optional.empty();
  }
}
