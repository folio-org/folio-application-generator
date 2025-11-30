package org.folio.app.generator.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
  void satisfies_positive_preReleaseWithFlag() {
    var result = SemverUtils.satisfies("1.2.3-alpha", "^1.0.0", true);

    assertThat(result).isTrue();
  }

  @Test
  void satisfies_positive_versionWithBuildMetadata() {
    var result = SemverUtils.satisfies("1.2.3+build.123", "^1.0.0", false);

    assertThat(result).isTrue();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("provideUnsatisfiedVersions")
  void satisfies_negative(String testName, String version, String constraint, boolean includePreRelease) {
    var result = SemverUtils.satisfies(version, constraint, includePreRelease);

    assertThat(result).isFalse();
  }

  private static Stream<Arguments> provideUnsatisfiedVersions() {
    return Stream.of(
      Arguments.of("version below constraint", "0.9.0", "^1.0.0", false),
      Arguments.of("pre-release without flag", "1.2.3-alpha", "^1.0.0", false),
      Arguments.of("invalid version", "invalid", "^1.0.0", false),
      Arguments.of("null version", null, "^1.0.0", false)
    );
  }

  @Test
  void isPreRelease_positive_snapshotVersion() {
    var result = SemverUtils.isPreRelease("1.0.0-SNAPSHOT");

    assertThat(result).isTrue();
  }

  @Test
  void isPreRelease_positive_uiSnapshot() {
    var result = SemverUtils.isPreRelease("11.0.109900000000247");

    assertThat(result).isTrue();
  }

  @Test
  void isPreRelease_negative_stableVersion() {
    var result = SemverUtils.isPreRelease("1.0.0");

    assertThat(result).isFalse();
  }

  @Test
  void isPreRelease_negative_invalidVersion() {
    var result = SemverUtils.isPreRelease("invalid");

    assertThat(result).isFalse();
  }

  @Test
  void isPreRelease_negative_nullVersion() {
    var result = SemverUtils.isPreRelease(null);

    assertThat(result).isFalse();
  }
}
