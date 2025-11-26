package org.folio.app.generator.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class PreReleaseFilterTest {

  @Test
  void fromValue_positive_only() {
    var result = PreReleaseFilter.fromValue("only");

    assertThat(result).isEqualTo(PreReleaseFilter.ONLY);
  }

  @Test
  void fromValue_positive_true() {
    var result = PreReleaseFilter.fromValue("true");

    assertThat(result).isEqualTo(PreReleaseFilter.TRUE);
  }

  @Test
  void fromValue_positive_false() {
    var result = PreReleaseFilter.fromValue("false");

    assertThat(result).isEqualTo(PreReleaseFilter.FALSE);
  }

  @Test
  void fromValue_positive_caseInsensitive() {
    assertThat(PreReleaseFilter.fromValue("ONLY")).isEqualTo(PreReleaseFilter.ONLY);
    assertThat(PreReleaseFilter.fromValue("True")).isEqualTo(PreReleaseFilter.TRUE);
    assertThat(PreReleaseFilter.fromValue("FALSE")).isEqualTo(PreReleaseFilter.FALSE);
  }

  @Test
  void fromValue_positive_null() {
    var result = PreReleaseFilter.fromValue(null);

    assertThat(result).isNull();
  }

  @Test
  void fromValue_negative_invalidValue() {
    assertThatThrownBy(() -> PreReleaseFilter.fromValue("invalid"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid preRelease value: 'invalid'")
        .hasMessageContaining("Allowed values: 'only', 'true', 'false'");
  }

  @Test
  void getValue_positive() {
    assertThat(PreReleaseFilter.ONLY.getValue()).isEqualTo("only");
    assertThat(PreReleaseFilter.TRUE.getValue()).isEqualTo("true");
    assertThat(PreReleaseFilter.FALSE.getValue()).isEqualTo("false");
  }

  @Test
  void isPreRelease_positive() {
    assertThat(PreReleaseFilter.ONLY.isPreRelease()).isTrue();
    assertThat(PreReleaseFilter.TRUE.isPreRelease()).isTrue();
    assertThat(PreReleaseFilter.FALSE.isPreRelease()).isFalse();
  }

  @Test
  void fromVersion_positive_preReleaseVersion() {
    var result = PreReleaseFilter.fromVersion("1.0.0-SNAPSHOT");

    assertThat(result).isEqualTo(PreReleaseFilter.TRUE);
  }

  @Test
  void fromVersion_positive_releaseVersion() {
    var result = PreReleaseFilter.fromVersion("1.0.0");

    assertThat(result).isEqualTo(PreReleaseFilter.FALSE);
  }

  @Test
  void fromVersion_positive_null() {
    var result = PreReleaseFilter.fromVersion(null);

    assertThat(result).isEqualTo(PreReleaseFilter.FALSE);
  }

  @Test
  void fromVersion_positive_buildMetadata() {
    var result = PreReleaseFilter.fromVersion("1.0.0-SNAPSHOT.124");

    assertThat(result).isEqualTo(PreReleaseFilter.TRUE);
  }
}
