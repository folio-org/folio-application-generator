package org.folio.app.generator.conditions;

import static org.folio.app.generator.model.types.RegistryType.SIMPLE;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

public class SimpleCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
    var simpleEnabled = context.getEnvironment().getProperty(SIMPLE.getPropertyName());
    return Boolean.parseBoolean(simpleEnabled);
  }
}
