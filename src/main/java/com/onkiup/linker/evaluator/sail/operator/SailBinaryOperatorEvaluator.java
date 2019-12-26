package com.onkiup.linker.evaluator.sail.operator;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.onkiup.linker.evaluator.api.Evaluator;
import com.onkiup.linker.evaluator.sail.builtin.DataFunctions;
import com.onkiup.linker.grammar.sail.token.LisaValueToken;

/**
 * This enum works as extension for BinaryOperators enum in grammar-sail
 * use Rule::as(Evaluator.class)
 */
public enum SailBinaryOperatorEvaluator {
  AMPERSAND(SailBinaryOperatorEvaluator::ampersand),
  DOT(SailBinaryOperatorEvaluator::dot),
  DOUBLE_EQUALS(SailBinaryOperatorEvaluator::doubleEquals),
  EQUALS(SailBinaryOperatorEvaluator::equalsOperator),
  LESS_EQUALS(SailBinaryOperatorEvaluator::lessEquals),
  LESS_MORE(SailBinaryOperatorEvaluator::lessMore),
  LESS(SailBinaryOperatorEvaluator::less),
  MINUS(MinusOperator::apply),
  MORE_EQUALS(SailBinaryOperatorEvaluator::moreEquals),
  MORE(SailBinaryOperatorEvaluator::more),
  NOT_EQUALS(SailBinaryOperatorEvaluator::notEquals),
  PLUS(PlusOperator::apply),
  SLASH(SlashOperator::apply),
  STAR(StarOperator::apply),
  STREAM(SailBinaryOperatorEvaluator::stream);

  private static Object stream(Object o, Object o1) {
    throw new RuntimeException("Not implemented");
  }

  private static <L, R> Boolean notEquals(L left, R right) {
    return comparisonOperator(left, right, x -> x != 0, false);
  }

  private static <L, R> Boolean more(L left, R right) {
    return comparisonOperator(left, right, x -> x > 0, false);
  }

  private static <L, R> Boolean moreEquals(L left, R right) {
    return comparisonOperator(left, right, x -> x >= 0, false);
  }

  private static <L, R> Boolean less(L left, R right) {
    return comparisonOperator(left, right, x -> x < 0, false);
  }

  private static <L, R> Boolean lessMore(L left, R right) {
    return comparisonOperator(left, right, x -> x != 0, false);
  }

  private static <L, R> Boolean lessEquals(L left, R right) {
    return comparisonOperator(left, right, x -> x <= 0, false);
  }

  private static <L, R> Boolean equalsOperator(L left, R right) {
    return comparisonOperator(left, right, x -> x == 0, true);
  }

  private static <T, R> Boolean doubleEquals(T left, R right) {
    return comparisonOperator(left, right, x -> x == 0, true);
  }

  private BiFunction evaluator;

  SailBinaryOperatorEvaluator(BiFunction<LisaValueToken, LisaValueToken, Object> evaluator) {
    this.evaluator = evaluator;
  }

  public static <L, R, X> X ampersand(L left, R right) {
    if (left instanceof String || right instanceof String) {
      return (X) ((left == null ? "null" : left.toString()) + (right == null ? "null" : right.toString()));
    }

    if (left == null || right == null) {
      return null;
    }

    throw new RuntimeException("Ampersand operator applies only to Strings, nulls, or instances of AmpersandOperator.Overloader interface");
  }

  public static <X> X dot(LisaValueToken leftOperand, LisaValueToken rightOperand) {
    return (X) DataFunctions.index(leftOperand.as(Evaluator.class), rightOperand.as(Evaluator.class));
  }


  public static <L, R> Boolean comparisonOperator(L left, R right, Function<Integer, Boolean> toBoolean, boolean useEquals) {
    if (left instanceof Comparable && right instanceof Comparable) {
      Comparable leftComparable = (Comparable)left;
      Comparable rightComparable = (Comparable)right;
      Integer result = null;
      try {
        result = leftComparable.compareTo(right);
      } catch (ClassCastException cce) {
        try {
          result = -rightComparable.compareTo(left);
        } catch (ClassCastException cce2) {

        }
      }

      if (result != null) {
        return toBoolean.apply(result);
      }
    }

    if (useEquals) {
      if (Objects.equals(left, right)) {
        return toBoolean.apply(0);
      } else {
        return toBoolean.apply(1);
      }
    }

    throw new RuntimeException("Both operands must implement Comparable");
  }

  public Object evaluate(LisaValueToken left, LisaValueToken right) {
    return evaluator.apply(left, right);
  }
}
