package com.onkiup.linker.evaluator.sail.token;

import java.util.Objects;

import com.onkiup.linker.evaluator.api.Reference;
import com.onkiup.linker.evaluator.common.EvaluationError;
import com.onkiup.linker.evaluator.sail.util.FakeAppianObject;
import com.onkiup.linker.grammar.sail.token.Namespace;
import com.onkiup.linker.grammar.sail.token.TypeReference;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

public class TypeReferenceEvaluator implements SailEvaluator<TypeReference, Class>, Reference<String, Class> {
  private static final String VANILLA_PACKAGE = "com.appiancorp.type.cdt.%s";

  @Override
  public String identity() {
    return base().name();
  }

  @Override
  public Class evaluate() {
    TypeReference base = this.base();
    Namespace ns = base.namespace();
    if (ns == null || ns.isAppian()) {
      String fullName = String.format(VANILLA_PACKAGE, base.name());
      try {
        return Class.forName(fullName);
      } catch (ClassNotFoundException e) {
        // Well, we should not be surprised by this :)
        try {
          return createFakeCdt(fullName);
        } catch (Exception ex) {
          throw new EvaluationError("Failed to create Appian vanilla CDT `" + fullName + "`", ex);
        }
      }
    } else if (Objects.equals("linker", ns.value())) {
      throw new EvaluationError("Not implemented");
    }
    throw new RuntimeException("Unsupported namespace");
  }

  private Class createFakeCdt(String name) throws NotFoundException, CannotCompileException {
    ClassPool pool = ClassPool.getDefault();
    CtClass result = pool.getOrNull(name);
    if (result == null) {
      result = pool.makeClass(name, pool.getCtClass(FakeAppianObject.class.getCanonicalName()));
    }
    return result.toClass();
  }

}
