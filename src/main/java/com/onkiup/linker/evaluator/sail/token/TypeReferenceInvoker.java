package com.onkiup.linker.evaluator.sail.token;

import java.lang.reflect.Constructor;

import com.onkiup.linker.evaluator.api.Evaluator;
import com.onkiup.linker.evaluator.api.RuleInvoker;
import com.onkiup.linker.evaluator.common.EvaluationError;
import com.onkiup.linker.evaluator.sail.util.FakeAppianObject;
import com.onkiup.linker.grammar.sail.token.InvocationParameter;
import com.onkiup.linker.grammar.sail.token.NamedInvocationParameter;
import com.onkiup.linker.grammar.sail.token.TypeReference;

public class TypeReferenceInvoker<X> implements RuleInvoker<TypeReference, X> {

  @Override
  public X execute(Evaluator... evaluators) {
    TypeReference base = base();
    Class<X> target = base.as(TypeReferenceEvaluator.class).value();
    Class[] parameters = new Class[evaluators.length];
    Object[] arguments = new Object[evaluators.length];

    for (int i = 0; i < evaluators.length; i++) {
      if (evaluators[i] != null) {
        parameters[i] = evaluators[i].resultType();
        arguments[i] = evaluators[i].value();
      }
    }


    try {
      if (FakeAppianObject.class.isAssignableFrom(target)) {
        FakeAppianObject result = (FakeAppianObject)target.newInstance();
        for (int i = 0; i < evaluators.length; i++) {
          Evaluator evaluator = evaluators[i];
          if (evaluator instanceof InvocationParameterEvaluator) {
            InvocationParameter parameter = ((InvocationParameterEvaluator<?>)evaluator).base();
            if (parameter instanceof NamedInvocationParameter) {
              result.put(parameter.name(), evaluator.value());
              continue;
            }
          }
          throw new EvaluationError("Unable to instantiate Appian CDT: all arguments must be named");
        }
        return (X)result;
      }

      Constructor<X> constructor = target.getConstructor(parameters);
      return constructor.newInstance(arguments);
    } catch (Exception e) {
      throw new EvaluationError("Failed to instantiate type `" + base + "`", e);
    }

  }
}
