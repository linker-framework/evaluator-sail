package com.onkiup.linker.evaluator.sail.builtin;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import com.onkiup.linker.evaluator.common.AbstractContext;
import com.onkiup.linker.evaluator.sail.SailRef;
import com.onkiup.linker.evaluator.sail.util.LisaError;

public class BuiltinContext extends AbstractContext<String> {

  private static final Reflections reflections = new Reflections(new ConfigurationBuilder()
        .setUrls(ClasspathHelper.forClassLoader(LisaError.class.getClassLoader()))
        .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner(), new MethodAnnotationsScanner(), new FieldAnnotationsScanner())
      );

  private static final ConcurrentHashMap<String, Object> members = new ConcurrentHashMap<>();

  static {
    reflections.getFieldsAnnotatedWith(SailRef.class).forEach(field -> {
      SailRef ref = field.getAnnotation(SailRef.class);
      register(ref, field);
    });

    reflections.getMethodsAnnotatedWith(SailRef.class).forEach(method -> {
      SailRef ref = method.getAnnotation(SailRef.class);
      register(ref, method);
    });

    reflections.getTypesAnnotatedWith(SailRef.class).forEach(type -> {
      SailRef ref = type.getAnnotation(SailRef.class);
      register( ref, type);
    });
  }

  public BuiltinContext() {
    super(null, null);
  }

  private static void register(SailRef ref, Object value) {
    String[] names = ref.value();
    boolean readOnly = ref.readOnly();
    for (String name: names) {
      members.put(name, value);
    }
  }

  @Override
  public Optional<?> resolveLocally(String key) {
    if (members.containsKey(key)) {
      return Optional.ofNullable(members.get(key));
    }

    return Optional.empty();
  }

  @Override
  public void store(String key, Object value, boolean modifiable, boolean override) {
    throw new UnsupportedOperationException("Unable to store values into immutable built-in context");
  }

  @Override
  public Map<String,Object> members() {
    return members;
  }
}

