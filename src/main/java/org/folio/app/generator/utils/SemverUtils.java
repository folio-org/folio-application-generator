package org.folio.app.generator.utils;

import lombok.experimental.UtilityClass;
import org.semver4j.RangesList;
import org.semver4j.RangesListFactory;
import org.semver4j.Semver;

/**
 * Utility class for handling FOLIO module version semantics.
 * Provides normalization for UI module versions with large patch numbers (npmSnapshot format)
 * that exceed Integer.MAX_VALUE and cannot be parsed by standard semver4j.
 *
 * <p>UI modules in FOLIO use timestamp-based patch numbers (e.g., 11.0.109900000000247)
 * that represent snapshots. These are normalized to pre-release format (e.g., 11.0.0-109900000000247)
 * to make them compatible with semver4j while maintaining semantic correctness.
 *
 * <p>This aligns with Okapi's hasNpmSnapshot() logic where patch numbers with 5+ digits
 * are treated as snapshots/pre-release versions.
 */
@UtilityClass
public class SemverUtils {

  /**
   * Threshold for detecting UI snapshot versions (npmSnapshot in Okapi terms).
   * Patch numbers with this many digits or more are considered snapshots.
   * Matches Okapi's SemVer.hasNpmSnapshot() logic: returns true if patch.length() >= 5
   */
  private static final int UI_SNAPSHOT_THRESHOLD = 5;

  /**
   * Normalize FOLIO module version for semver4j compatibility.
   * Converts UI module snapshots (huge patch numbers) to pre-release format.
   *
   * <p>Examples:
   * <ul>
   *   <li>"11.0.109900000000247" → "11.0.0-109900000000247" (UI snapshot)</li>
   *   <li>"9.0.10990" → "9.0.0-10990" (UI snapshot, 5 digits)</li>
   *   <li>"19.6.361" → "19.6.361" (backend module, 3 digits - unchanged)</li>
   *   <li>"19.6.0-SNAPSHOT.361" → "19.6.0-SNAPSHOT.361" (already pre-release - unchanged)</li>
   *   <li>"19.6.0" → "19.6.0" (stable - unchanged)</li>
   * </ul>
   *
   * @param version the version string to normalize
   * @return normalized version string compatible with semver4j
   */
  public static String normalizeVersion(String version) {
    if (version == null || version.contains("-") || version.contains("+")) {
      return version;
    }

    String[] parts = version.split("\\.");
    if (parts.length != 3) {
      return version;
    }

    String patch = parts[2];
    if (!patch.matches("\\d+")) {
      return version;
    }

    if (patch.length() >= UI_SNAPSHOT_THRESHOLD) {
      return parts[0] + "." + parts[1] + ".0-" + patch;
    }

    return version;
  }

  /**
   * Parse version with automatic UI snapshot normalization.
   *
   * <p>This method normalizes the version before parsing, allowing UI module versions
   * with large patch numbers to be parsed correctly.
   *
   * @param version the version string to parse
   * @return Semver instance with a normalized version
   * @throws IllegalArgumentException if the version cannot be parsed
   */
  public static Semver parse(String version) {
    return Semver.parse(normalizeVersion(version));
  }

  /**
   * Check if version satisfies constraint, with automatic normalization.
   *
   * <p>The version string is normalized before checking against the constraint,
   * enabling constraint matching for UI module versions. The constraint itself
   * is NOT normalized as it represents a range expression, not a version.
   *
   * <p>Example:
   * <pre>{@code
   * boolean matches = SemverUtils.satisfies(
   *   "11.0.109900000000247",  // UI snapshot version
   *   "^11.0.0",                // Caret constraint
   *   true                      // Include pre-release
   * );
   * // Result: true (version normalized to 11.0.0-109900000000247, matches ^11.0.0 with pre-release)
   * }</pre>
   *
   * @param version the version string to check
   * @param constraint the version constraint expression
   * @param includePreRelease whether to include pre-release versions in range matching
   * @return true if version satisfies the constraint, false otherwise
   */
  public static boolean satisfies(String version, String constraint, boolean includePreRelease) {
    Semver semver = parse(version);
    RangesList ranges = RangesListFactory.create(constraint, includePreRelease);
    return ranges.isSatisfiedBy(semver);
  }
}
