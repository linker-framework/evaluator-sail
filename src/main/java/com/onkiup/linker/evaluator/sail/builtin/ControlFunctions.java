package com.onkiup.linker.evaluator.sail.builtin;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import com.onkiup.linker.evaluator.api.ArrayEvaluator;
import com.onkiup.linker.evaluator.api.EvaluationContext;
import com.onkiup.linker.evaluator.api.Evaluator;
import com.onkiup.linker.evaluator.api.Invoker;
import com.onkiup.linker.evaluator.api.RuleEvaluator;
import com.onkiup.linker.evaluator.common.EvaluationError;
import com.onkiup.linker.evaluator.sail.SailRef;
import com.onkiup.linker.evaluator.sail.Variant;
import com.onkiup.linker.evaluator.sail.util.SailRule;
import com.onkiup.linker.evaluator.sail.error.MissingParameter;
import com.onkiup.linker.grammar.sail.token.InvocationParameter;
import com.onkiup.linker.grammar.sail.token.NamedInvocationParameter;
import com.onkiup.linker.grammar.sail.token.TypeReference;
import com.onkiup.linker.parser.Rule;

public final class ControlFunctions {
  private ControlFunctions() {

  }

  @SailRef({"apply", "fn!apply"})
  public static <I, O> O[] fnApply(Invoker<O> predicate, ArrayEvaluator<I> listToken, Evaluator... contextValues) {
    Evaluator[] parameters = new Evaluator[contextValues.length + 1];
    System.arraycopy(contextValues, 0, parameters, 1, parameters.length);

    I[] list = listToken.value();
    if (list == null) {
      return null;
    }

    int size = list.length;
    O[] result = (O[]) Array.newInstance(predicate.resultType(), size);
    for (int i = 0; i < size; i++) {
      if (list[i] instanceof  Evaluator) {
        parameters[0] = (Evaluator)list[i];
      } else {
        parameters[0] = new Variant(list[i]);
      }
      result[i] = predicate.invoke(parameters);
    }

    return result;
  }

  @SailRef({"foreach", "a!foreach"})
  public static <I, O> O[] forEach(Evaluator<I> itemsToken, Evaluator<O> expression) {
    I items = itemsToken.value();
    Class itemsClass = items.getClass();
    if (itemsClass.isArray()) {
      return forEachInArray((Object[]) items, expression);
    } else if (Map.class.isAssignableFrom(itemsClass)) {
      return forEachInMap((Map<String, Object>) items, expression);
    } else {
      return forEachInObject(items, expression);
    }
  }

  @SailRef({"choose", "fn!choose"})
  public static <O> O choose(int index, Evaluator... values) {
    return (O)values[index - 1].value();
  }

  @SailRef({"if", "fn!if"})
  public static <O> O fnIf(Evaluator... arguments) {
    if (arguments.length < 2) {
      throw new EvaluationError("Not enough arguments (at least 2 are required)");
    }
    Boolean spillover = null;
    int lastPossibleThen = arguments.length - 1;
    for (int i = 0; i < arguments.length; i++) {
      Evaluator argument = arguments[i++];
      Object currentValue = argument.value();
      if (spillover != null && spillover) {
        return (O) currentValue;
      } else if (spillover != null) {
        continue;
      }

      if (currentValue instanceof Boolean) {
        if((Boolean) currentValue) {
          if (i < arguments.length) {
            return (O) arguments[i].value();
          } else {
            throw new MissingParameter("Expected 'then' value at position " + i);
          }
        } else if (++i < arguments.length) {
          Object elseCandidate = arguments[i].value();
          if (elseCandidate instanceof Boolean && i < lastPossibleThen) {
            spillover = (Boolean) elseCandidate;
          } else {
            return (O) elseCandidate;
          }
        } else {
          throw new MissingParameter("Expected 'otherwise' value at position " + i);
        }
      } else {
        throw new MissingParameter("Expected 'condition' value at position " + i);
      }
    }

    return null;
  }

  public static <I, O> O[] forEachInArray(I[] items, Evaluator<O> expression) {
    return EvaluationContext.isolated(String.class, context -> {
      context.store("fv!itemcount", items.length, false);
      O[] result = (O[])Array.newInstance(expression.resultType(), items.length);
      int lastI = items.length - 1;
      for (int i = 0; i < items.length; i++) {
        context.override("fv!index", i + 1, false);
        context.override("fv!item", items[i], false);
        context.override("fv!isfirst", i == 0, false);
        context.override("fv!islast", i == lastI, false);
        result[i] = expression.value();
      }
      return result;
    });
  }

  public static <O> O[] forEachInMap(Map<String, Object> items, Evaluator<O> expression) {
    return EvaluationContext.isolated(String.class, context -> {
      context.store("fv!itemcount", items.size(), false);
      O[] result = (O[])Array.newInstance(expression.resultType(), items.size());
      int lastI = items.size() - 1;
      Object[] keys = items.keySet().toArray();
      for (int i = 0; i < keys.length; i++) {
        String key = (String) keys[i];
        Object value = items.get(key);
        context.override("fv!index", i + 1, false);
        context.override("fv!identifier", key, false);
        context.override("fv!item", value, false);
        context.override("fv!isfirst", i == 0, false);
        context.override("fv!islast", i == lastI, false);
        result[i] = expression.value();
      }
      return result;
    });
  }

  public static <O> O[] forEachInObject(Object source, Evaluator<O> expression) {
    return EvaluationContext.isolated(String.class, context -> {
      Field[] fields = source.getClass().getDeclaredFields();
      O[] result = (O[])Array.newInstance(expression.resultType(), fields.length);
      int lastI = fields.length - 1;
      context.store("fv!itemcount", fields.length, false);
      for (int i = 0; i < fields.length; i++) {
        Field field = fields[i];
        field.setAccessible(true);
        String identifier = field.getName();
        Object value = null;
        try {
          if (Modifier.isStatic(field.getModifiers())) {
            value = field.get(null);
          } else {
            value = field.get(source);
          }
        } catch (Exception e) {
          throw new EvaluationError("Failed to read field " + field.getName() + " from " + source, e);
        }
        context.override("fv!index", i + 1, false);
        context.override("fv!identifier", identifier, false);
        context.override("fv!item", value, false);
        context.override("fv!isfirst", i == 0, false);
        context.override("fv!islast", i == lastI, false);
        result[i] = expression.value();
      }

      return result;
    });
  }

  @SailRef({"try", "fn!try"})
  public static <O> O fnTry(Evaluator test, Evaluator recover) {
    try {
      return (O) test.value();
    } catch (Throwable e) {
      return EvaluationContext.isolated(String.class, context -> {
        context.store("fv!error", e);
        return (O) recover.value();
      });
    }
  }

  @SailRef({"function", "ls!fun"})
  public static SailRule newFunction(InvocationParameter... args) {
    if (args.length == 0) {
      throw new EvaluationError("Not enough arguments (at least one is required)");
    }

    boolean namedArgs = false;
    boolean positionalArgs = false;
    NamedInvocationParameter[] params = new NamedInvocationParameter[args.length - 1];
    for (int i = 0; i < args.length - 1; i++) {
      InvocationParameter argument = args[i];
      Rule type = argument.token();

      if (!TypeReference.class.isInstance(type)) {
        throw new EvaluationError("Invalid function definition: argument #" + i + " is not a TypeReference");
      }

      if (!NamedInvocationParameter.class.isInstance(argument)) {
        throw new EvaluationError("Invalid function definition: argument #" + i + " is not named");
      }

      params[i] = (NamedInvocationParameter)argument;
    }

    return new SailRule(args[args.length - 1].token().as(RuleEvaluator.class), params);
  }
}

