package com.onkiup.linker.evaluator.sail.token;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.onkiup.linker.evaluator.api.EvaluationContext;
import com.onkiup.linker.evaluator.api.RuleEvaluator;
import com.onkiup.linker.evaluator.common.EvaluationError;
import com.onkiup.linker.parser.Rule;
import com.onkiup.linker.parser.token.PartialToken;

public interface SailEvaluator<I extends Rule, O> extends RuleEvaluator<I, O> {

  static class Metadata {
    private static final AtomicBoolean lockCreationContext = new AtomicBoolean(true);
    private static final ConcurrentHashMap<SailEvaluator<?,?>,Consumer<?>> listeners = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<SailEvaluator<?,?>,EvaluationContext> creationContexts = new ConcurrentHashMap<>();

    private static <T extends Rule, O> void listener(SailEvaluator<T,O> token, Consumer<O> listener) {
      listeners.put(token, listener);
    }

    private static <T extends Rule, O> Consumer<O> listener(SailEvaluator<T,O> token) {
      return (Consumer<O>)listeners.get(token);
    }

    private static void removeListeners(SailEvaluator token) {
      listeners.remove(token);
    }

    private static void creationContext(SailEvaluator token, EvaluationContext context) {
      if (creationContexts.containsKey(token) && lockCreationContext.get()) {
        throw new EvaluationError("Unable to override creation context: not allowed");
      }

      creationContexts.put(token, context);
    }

    private static EvaluationContext creationContext(SailEvaluator token) {
      if (!creationContexts.containsKey(token)) {
        throw new EvaluationError("Unable to return creation context: not stored yet");
      }

      return creationContexts.get(token);
    }

    public static void unlockCreationContexts() {
      lockCreationContext.set(false);
    }
  }

  default void listen(Consumer<O> listener) {
    Metadata.listener(this, listener);
    evaluate();
  }

  default void notify(O value) {
    Consumer<O> listener = Metadata.listener(this);
    if (listener != null) {
      listener.accept(value);
    }
  }

  default O evaluate() {
    throw new EvaluationError("Not implemented");
  }

  default O value() {
    EvaluationContext.currentToken(this);
    return evaluate();
  }

  default void setCreationContext(EvaluationContext creationContext) {
    Metadata.creationContext(this, creationContext);
  }

  default EvaluationContext creationContext() {
    return Metadata.creationContext(this);
  }

  default EvaluationContext<String> context() {
    return (EvaluationContext<String>)EvaluationContext.get();
  }

  default Optional<PartialToken<?>> metadata() {
    return base().metadata();
  }

}
