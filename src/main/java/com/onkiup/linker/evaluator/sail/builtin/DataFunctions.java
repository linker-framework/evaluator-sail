package com.onkiup.linker.evaluator.sail.builtin;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.onkiup.linker.evaluator.api.EvaluationContext;
import com.onkiup.linker.evaluator.api.Evaluator;
import com.onkiup.linker.evaluator.api.Invoker;
import com.onkiup.linker.evaluator.sail.SailRef;
import com.onkiup.linker.evaluator.sail.Variant;
import com.onkiup.linker.evaluator.sail.token.InvocationParameterEvaluator;
import com.onkiup.linker.evaluator.sail.token.ListEvaluator;
import com.onkiup.linker.evaluator.sail.util.LisaError;
import com.onkiup.linker.grammar.sail.token.InvocationParameter;
import com.onkiup.linker.grammar.sail.token.NamedInvocationParameter;
import com.onkiup.linker.grammar.sail.token.UnnamedInvocationParameter;

public final class DataFunctions {

  private DataFunctions() {
    super();
  }

  @SailRef({"filter", "fn!filter"})
  public static <O> O[] fnFilter(Invoker<Boolean> predicate, ListEvaluator<O> listToken, Evaluator... context) {
    Evaluator[] parameters = new Evaluator[context.length + 1];
    System.arraycopy(context, 0, parameters, 1, parameters.length);

    O[] list = listToken.value();
    if (list == null) {
      return null;
    }

    Variant zero = new Variant(null);
    parameters[0] = zero;

    return Arrays.stream(list)
        .filter(value -> {
          zero.set(value);
          return predicate.invoke(parameters);
        })
        .toArray(size -> (O[])Array.newInstance(listToken.componentType(), size));
  }


  @SailRef({"merge", "fn!merge"})
  public static <O> O[] fnMerge(ListEvaluator<O>... lists) {
    List<O> result = new ArrayList<>();
    
    Object[] values = new Object[lists.length];
    int position = 0;
    boolean hadLists = false;
    do {
      hadLists = false;
      for (int i = 0; i < lists.length; i++) {
        if (values[i] == null) {
          values[i] = lists[i].value();
        }
        if (position < Array.getLength(values[i])) {
          hadLists = true;
          result.add((O) Array.get(values[i], position));
        }
      }
      position++;
    } while (hadLists);

    return (O[]) result.toArray();
  }

  @SailRef({"reduce", "fn!reduce"})
  public static <O> O fnReduce(Invoker<O> function, Evaluator<O> initial, ListEvaluator<O> listToken, Evaluator... context) {
    O[] list = listToken.value();
    int paramCount = 2 + (context == null ? 0 : context.length);
    Evaluator[] parameters = new Evaluator[paramCount];
    if (context != null) {
      System.arraycopy(context, 0, parameters, 2, context.length);
    }
    Variant<O> result = new Variant<>(initial.value());
    Variant<O> first = new Variant<>(null);
    parameters[0] = result;
    parameters[1] = first;

    for (int i = 0; i < list.length; i++) {
      first.set(list[i]);
      result.set(function.invoke(parameters));
    }

    return result.value();
  }

  @SailRef({"reject", "fn!reject"})
  public static <O> O[] reject(Invoker<Boolean> predicate, ListEvaluator<O> list, Evaluator... context) {
    Evaluator[] parameters;
    if (context != null) {
      parameters = new Evaluator[context.length + 1];
      System.arraycopy(context, 0, parameters, 1, context.length);
    } else {
      parameters = new Evaluator[1];
    }

    final Variant<O> argument = new Variant<>(null);
    parameters[0] = argument;

    return Arrays.stream(list.value())
      .filter(item -> {
        argument.set(item);
        return !predicate.invoke(parameters);
      }).toArray(size -> (O[])Array.newInstance(list.componentType(), size));
  }

  @SailRef("a!flatten")
  public static <O> O[] flatten(Object list) {
    if (list == null) {
      return null;
    }
    List<O> result = new ArrayList<>();
    Class listType = list.getClass();
    if (listType.isArray()) {
      int size = Array.getLength(list);
      for (int i = 0; i < size; i++) {
        Object element = Array.get(list, i);
        if (element == null) {
          result.add(null);
        } else {
          Class elementType = element.getClass();
          if (elementType.isArray()) {
            int elementSize = Array.getLength(element);
            for (int j = 0; j < elementSize; j++) {
              result.add((O)Array.get(element, j));
            }
          } else {
            result.add((O)element);
          }
        }
      }
    }
    return (O[]) result.toArray();
  }

  @SailRef({"append", "fn!append"})
  public static <O> O[] append(Object[] list, Object... values) {
    Object[] result = new Object[list.length + (values == null ? 0 : values.length)];
    System.arraycopy(list, 0, result, 0, list.length);
    if (values != null) {
      System.arraycopy(values, 0, result, list.length, values.length);
    }
    return (O[]) result;
  }

  @SailRef({"index", "fn!index"})
  public static <O> O index(Evaluator source, Evaluator index, Evaluator... def) {
    Object src = source.value();
    Object idx = index.value();

    if (src == null || idx == null) {
      return null;
    }

    Object defVal = null;
    if (def != null) {
      if (def.length > 1) {
        throw new LisaError("Too many arguments for fn!index");
      } else if (def.length == 1){
        defVal = def[0].value();
      }
    }

    Class srcType = src.getClass();

    Object[] result;
    if (srcType.isArray()) {
      result = populateIndexResult(src, idx, defVal, (s, i) -> Array.get(s, -1 + (int)i), DataFunctions::validateArrayKey);
    } else if (List.class.isAssignableFrom(srcType)) {
      result = populateIndexResult(src, idx, defVal, (s, i) -> ((List)s).get(-1 + (int)i), DataFunctions::validateArrayKey);
    } else if (Map.class.isAssignableFrom(srcType)) {
      result = populateIndexResult(src, idx, defVal, (s, i) -> ((Map)s).get(i), null);
    } else {
      result = populateIndexResult(src, idx, defVal, (s, fieldName) -> {
        if (fieldName == null) {
          throw new LisaError("Unable to index using null key");
        }

        try {
          Field field = srcType.getDeclaredField(fieldName.toString());
          field.setAccessible(true);
          return field.get(s);
        } catch (NoSuchFieldException nsfe) {
          try {
            String fieldNameString = fieldName.toString();
            Method getter = srcType.getDeclaredMethod("get" + fieldNameString.substring(0, 1).toUpperCase() + fieldNameString.substring(1));
            return getter.invoke(s);
          } catch (Exception e) {
            throw new LisaError("Faield to get field " + fieldName + ": no such field or geter error", e);
          }
        } catch (Exception e) {
          throw new LisaError("Failed to get field '" + fieldName + "'", e);
        }
      }, null);
    }

    return (O) (result.length == 1 ? result[0] : result);
  }

  private static void validateArrayKey(Object key) {
    if (!(key instanceof Number)) {
      throw new LisaError("Unable to index an array using '" + (key == null ? "null" : key.getClass().getName()) + "' as key");
    }
  }
  
  private static Object[] populateIndexResult(Object source, Object keys, Object defVal, BiFunction getter, Consumer keyValidator) {
    Class keysType = keys.getClass();
    if (!keysType.isArray()) {
      if (List.class.isAssignableFrom(keysType)) {
        keys = ((List)keys).toArray();
      } else {
        keys = new Object[] {keys};
      }
    }
    Object[] result = new Object[Array.getLength(keys)];
    for (int i = 0; i < result.length; i++) {
      Object key = Array.get(keys, i);
      try {
        if (keyValidator != null) {
          keyValidator.accept(key);
        }
        result[i] = getter.apply(source, key);
      } catch (Exception e) {
        if (defVal != null) {
          result[i] = defVal;
        } else {
          throw new LisaError("Failed to fetch index '" + key + "'", e);
        }
      }
    }
    return result;
  }

  @SailRef({"insert", "fn!insert"})
  public static <O> O insert(O target, Object value, Object index) {
    if (target == null) {
      return null;
    }
    if (index == null) {
      throw new LisaError("Unable to insert using null index");
    }

    Class<O> targetType = (Class<O>) target.getClass();
    O result = null;
    if (targetType.isArray()) {
      if (!(index instanceof Number)) {
        throw new LisaError("Unable to insert into a list using non-number index");
      }
      int targetSize = Array.getLength(target);
      int i = -1 + (int) index;
      if (i > targetSize) {
        // prevents ArrayIndexOutOfBoundsException
        i = targetSize;
      }

      result = (O) Array.newInstance(targetType.getComponentType(), Array.getLength(target) + 1);
      try {
        System.arraycopy(target, 0, result, 0, i);
        Array.set(result, i, value);
        System.arraycopy(target, i, result, i + 1, targetSize - i);
      } catch (ArrayStoreException ase) {
        throw new LisaError("Failed to store object of type '" + (value == null ? "null" : value.getClass().getName()) + "' into an array of '" +  targetType.getComponentType().getName() + "'", ase);
      } catch (ArrayIndexOutOfBoundsException aioobe) {
        throw new LisaError("Failed to insert, aioobe. (i = " + i + "; targetSize = " + targetSize + ")");
      }
    } else if (target instanceof List) {
      result = target;
      int i = -1 + (int) index;
      ((List)result).add(i, value);
    } else if (target instanceof Map) {
      result = target;
      ((Map)result).put(index, value);
    } else if (target instanceof String) {
      int i = -1 + (int) index;
      result = (O) new StringBuilder((String)target).insert(i, value).toString();
    } else {
      throw new LisaError("Unable to insert into object of type '" + target.getClass().getName() + "'");
    }

    return result;
  }

  @SailRef({"joinarray", "fn!joinarray"})
  public static String joinArray(Object array, Object separator) {
  
    if (array == null) {
      return null;
    }

    String sep = separator == null ? "null" : separator.toString();
    Class arrType = array.getClass();

    Stream values;
    if (arrType.isArray()) {
      StringBuilder result = new StringBuilder();
      int size = Array.getLength(array);
      int last = size - 1;
      for (int i = 0; i < size; i++) {
        Object item = Array.get(array, i);
        result.append(item == null ? "null" : item.toString());
        if (i < last) {
          result.append(sep);
        }
      }
      return result.toString();
    } else if (array instanceof List) {
      values = ((List)array).stream();
    } else if (array instanceof Map) {
      values = ((Map)array).values().stream();
    } else {
      return array.toString();
    }

    return (String) values
      .map(item -> item == null ? "null" : item.toString())
      .collect(Collectors.joining(sep));
  }

  @SailRef({"fn!with", "with"})
  public static <O> O with(InvocationParameterEvaluator<?>[] arguments) {
    if (arguments.length == 0) {
      return null;
    } else if (arguments.length == 1) {
      InvocationParameterEvaluator<?> expressionParameter = arguments[0];
      if (expressionParameter.base() instanceof NamedInvocationParameter) {
        throw new LisaError("Invalid invocation for fn!with: no expression");
      }
      return (O) expressionParameter.value();
    }

    return EvaluationContext.isolated(String.class, context -> {
      Evaluator<O> expression = null;
      int last = arguments.length - 1;
      for (int i = 0; i < arguments.length; i++) {
        InvocationParameterEvaluator<?> parameter = arguments[i];
        InvocationParameter base = parameter.base();
        if (base instanceof UnnamedInvocationParameter) {
          if (i != last) {
            throw new LisaError("Invalid invocation for fn!with: parameter " + (i+1) + " should have a name");
          }
          expression = (Evaluator<O>)parameter;
        } else if (base instanceof NamedInvocationParameter) {
          context.store(base.name().toString(), parameter.value());
        }
      }
      return expression == null ? null : expression.value();
    });
  }
}

