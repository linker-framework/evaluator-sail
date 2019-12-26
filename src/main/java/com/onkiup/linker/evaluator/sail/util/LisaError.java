package com.onkiup.linker.evaluator.sail.util;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;

import com.onkiup.linker.evaluator.api.EvaluationContext;
import com.onkiup.linker.evaluator.api.RuleEvaluator;
import com.onkiup.linker.evaluator.common.EvaluationError;
import com.onkiup.linker.grammar.sail.token.LisaToken;
import com.onkiup.linker.parser.ParserLocation;
import com.onkiup.linker.parser.token.PartialToken;

public class LisaError extends EvaluationError {

  private LisaToken source;
  private LinkedList<StackTraceElement> trace;

  public LisaError(String message) {
    this(message, null);
  }

  public LisaError(String message, Throwable cause) {
    this(EvaluationContext.currentToken().location(), message, cause);
  }

  public LisaError(ParserLocation location, String msg) {
    super(location, msg);
  }

  public LisaError(ParserLocation location, String msg, Throwable cause) {
    super(location, msg, cause);
  }

  @Override
  public Throwable fillInStackTrace() {
    if (trace == null) {
      RuleEvaluator<?, ?> current = EvaluationContext.currentToken();
      if (current == null) {
        return this;
      }
      trace = new LinkedList<>();

      Arrays.stream(Thread.currentThread().getStackTrace())
          .forEach(trace::push);

      EvaluationContext.stack().stream()
        .map(EvaluationContext::asStackTraceElement)
        .filter(Objects::nonNull)
        .forEach(trace::push);

      String sailName = current.base().getClass().getSimpleName();
      try {
        PartialToken<?> meta = current.metadata().get();
        trace.push(new StackTraceElement(sailName, "..", meta.location().name(), meta.location().line()));
      } catch (Exception e) {
        trace.push(new StackTraceElement(sailName, "..", "???", 0));
      }
    }

    return this;
  }
  
  @Override
  public StackTraceElement[] getStackTrace() {
    return (StackTraceElement[]) trace.toArray();
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder()
      .append(getMessage())
      .append("\n");

    if (trace != null) {
      for (StackTraceElement element : trace) {
        result.append("\t").append(element.toString()).append("\n");
      }
    }

    return result.toString();
  }

  @Override
  public String getMessage() {
    return super.getMessage();
  }

}

