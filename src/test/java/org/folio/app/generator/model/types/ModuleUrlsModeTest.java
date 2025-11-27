package org.folio.app.generator.model.types;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class ModuleUrlsModeTest {

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"false", "invalid", "unknown"})
  void fromString_positive_returnsFalse(String input) {
    var result = ModuleUrlsMode.fromString(input);

    assertThat(result).isEqualTo(ModuleUrlsMode.FALSE);
  }

  @Test
  void fromString_positive_true() {
    var result = ModuleUrlsMode.fromString("true");

    assertThat(result).isEqualTo(ModuleUrlsMode.TRUE);
  }

  @Test
  void fromString_positive_both() {
    var result = ModuleUrlsMode.fromString("both");

    assertThat(result).isEqualTo(ModuleUrlsMode.BOTH);
  }

  @Test
  void fromString_positive_caseInsensitive() {
    assertThat(ModuleUrlsMode.fromString("FALSE")).isEqualTo(ModuleUrlsMode.FALSE);
    assertThat(ModuleUrlsMode.fromString("True")).isEqualTo(ModuleUrlsMode.TRUE);
    assertThat(ModuleUrlsMode.fromString("BOTH")).isEqualTo(ModuleUrlsMode.BOTH);
  }

  @Test
  void needDescriptorUrl_positive_true() {
    assertThat(ModuleUrlsMode.TRUE.needDescriptorUrl()).isTrue();
  }

  @Test
  void needDescriptorUrl_positive_both() {
    assertThat(ModuleUrlsMode.BOTH.needDescriptorUrl()).isTrue();
  }

  @Test
  void needDescriptorUrl_negative_false() {
    assertThat(ModuleUrlsMode.FALSE.needDescriptorUrl()).isFalse();
  }

  @Test
  void needFullDescriptor_positive_false() {
    assertThat(ModuleUrlsMode.FALSE.needFullDescriptor()).isTrue();
  }

  @Test
  void needFullDescriptor_positive_both() {
    assertThat(ModuleUrlsMode.BOTH.needFullDescriptor()).isTrue();
  }

  @Test
  void needFullDescriptor_negative_true() {
    assertThat(ModuleUrlsMode.TRUE.needFullDescriptor()).isFalse();
  }

  @Test
  void needBoth_positive_both() {
    assertThat(ModuleUrlsMode.BOTH.needBoth()).isTrue();
  }

  @Test
  void needBoth_negative_false() {
    assertThat(ModuleUrlsMode.FALSE.needBoth()).isFalse();
  }

  @Test
  void needBoth_negative_true() {
    assertThat(ModuleUrlsMode.TRUE.needBoth()).isFalse();
  }
}
