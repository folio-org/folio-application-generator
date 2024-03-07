package org.folio.app.generator.conditions;

import static org.folio.app.generator.model.types.RegistryType.AWS_S3;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

public class AwsCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
    var s3Enabled = context.getEnvironment().getProperty(AWS_S3.getPropertyName());
    return Boolean.parseBoolean(s3Enabled);
  }
}
