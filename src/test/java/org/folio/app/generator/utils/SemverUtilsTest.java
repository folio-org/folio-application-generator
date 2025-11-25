package org.folio.app.generator.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class SemverUtilsTest {

  @Test
  void normalizeVersion_positive_uiSnapshotLargePatch() {
    var result = SemverUtils.normalizeVersion("11.0.109900000000247");

    assertThat(result).isEqualTo("11.0.0-109900000000247");
  }

  @Test
  void normalizeVersion_positive_backendModuleThreeDigits() {
    var result = SemverUtils.normalizeVersion("19.6.361");

    assertThat(result).isEqualTo("19.6.361");
  }

  @Test
  void normalizeVersion_positive_alreadyPreRelease() {
    var result = SemverUtils.normalizeVersion("19.6.0-SNAPSHOT.361");

    assertThat(result).isEqualTo("19.6.0-SNAPSHOT.361");
  }

  @Test
  void normalizeVersion_positive_versionWithBuildMetadata() {
    var result = SemverUtils.normalizeVersion("1.2.3+20230101");

    assertThat(result).isEqualTo("1.2.3+20230101");
  }

  @Test
  void normalizeVersion_positive_nullVersion() {
    var result = SemverUtils.normalizeVersion(null);

    assertThat(result).isNull();
  }

  @Test
  void normalizeVersion_positive_patchWithNonDigits() {
    var result = SemverUtils.normalizeVersion("1.2.3a");

    assertThat(result).isEqualTo("1.2.3a");
  }

  @Test
  void satisfies_positive_caretConstraintWithStableVersion() {
    var result = SemverUtils.satisfies("1.2.3", "^1.0.0", false);

    assertThat(result).isTrue();
  }

  @Test
  void satisfies_positive_uiSnapshotWithRangeConstraint() {
    var result = SemverUtils.satisfies("11.1.109900000000247", "^11.0.0", true);

    assertThat(result).isTrue();
  }

  @Test
  void satisfies_negative_versionBelowConstraint() {
    var result = SemverUtils.satisfies("0.9.0", "^1.0.0", false);

    assertThat(result).isFalse();
  }

  @Test
  void satisfies_positive_tildeConstraint() {
    var result = SemverUtils.satisfies("1.2.5", "~1.2.0", false);

    assertThat(result).isTrue();
  }

  @Test
  void satisfies_positive_exactVersion() {
    var result = SemverUtils.satisfies("1.2.3", "1.2.3", false);

    assertThat(result).isTrue();
  }

  @Test
  void satisfies_positive_greaterOrEqualConstraint() {
    var result = SemverUtils.satisfies("2.0.0", ">=1.0.0", false);

    assertThat(result).isTrue();
  }

  @Test
  void satisfies_negative_preReleaseWithoutFlag() {
    var result = SemverUtils.satisfies("1.2.3-alpha", "^1.0.0", false);

    assertThat(result).isFalse();
  }

  @Test
  void satisfies_positive_preReleaseWithFlag() {
    var result = SemverUtils.satisfies("1.2.3-alpha", "^1.0.0", true);

    assertThat(result).isTrue();
  }

  @Test
  void satisfies_negative_invalidVersion() {
    var result = SemverUtils.satisfies("invalid", "^1.0.0", false);

    assertThat(result).isFalse();
  }

  @Test
  void satisfies_negative_nullVersion() {
    var result = SemverUtils.satisfies(null, "^1.0.0", false);

    assertThat(result).isFalse();
  }

  @Test
  void satisfies_positive_versionWithBuildMetadata() {
    var result = SemverUtils.satisfies("1.2.3+build.123", "^1.0.0", false);

    assertThat(result).isTrue();
  }
}
