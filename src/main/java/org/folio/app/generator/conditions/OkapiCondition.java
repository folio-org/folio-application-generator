package org.folio.app.generator.conditions;

import static org.folio.app.generator.model.types.RegistryType.OKAPI;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

public class OkapiCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
    var okapiEnabled = context.getEnvironment().getProperty(OKAPI.getPropertyName());
    return Boolean.parseBoolean(okapiEnabled);
  }
}
