package org.folio.app.generator.conditions;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.context.annotation.Conditional;

public class HttpCondition extends AnyNestedCondition  {

  HttpCondition() {
    super(ConfigurationPhase.REGISTER_BEAN);
  }

  @Conditional(OkapiCondition.class)
  static class Okapi { }

  @Conditional(SimpleCondition.class)
  static class Simple { }
}
