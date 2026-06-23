package org.folio.app.generator.utils;

import static org.apache.commons.lang3.StringUtils.trim;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.folio.app.generator.model.types.RegistryType;

/**
 * Parses custom registry HTTP header strings.
 *
 * <p>A header list has the form {@code Name:Value;Name2:Value2} (split on the first {@code :}, so
 * values may contain {@code :}). The scoped form additionally supports an optional {@code type::}
 * prefix and comma-separated groups: {@code X-App:folio,okapi::X-Okapi-Token:secret}.</p>
 */
public final class RegistryHeaderParser {

  private static final String GROUP_DELIMITER = ",";
  private static final String SCOPE_DELIMITER = "::";
  private static final String HEADER_PAIR_DELIMITER = ";";
  private static final String HEADER_KEY_VALUE_DELIMITER = ":";

  private RegistryHeaderParser() {}

  /**
   * Parses a {@code Name:Value;Name2:Value2} header list into an ordered map.
   *
   * @param headersString - header list string, may be {@code null}/blank
   * @return ordered map of header name to value (empty when nothing parseable)
   */
  public static Map<String, String> parseHeaders(String headersString) {
    var headers = new LinkedHashMap<String, String>();
    if (StringUtils.isBlank(headersString)) {
      return headers;
    }

    for (var pair : headersString.split(HEADER_PAIR_DELIMITER)) {
      var delimiterIndex = pair.indexOf(HEADER_KEY_VALUE_DELIMITER);
      if (delimiterIndex > 0) {
        var key = trim(pair.substring(0, delimiterIndex));
        var headerValue = trim(pair.substring(delimiterIndex + 1));
        if (StringUtils.isNotEmpty(key)) {
          headers.put(key, headerValue);
        }
      }
    }

    return headers;
  }

  /**
   * Parses the {@code registryHeaders} parameter grammar (optional {@code type::} prefix per
   * comma-separated group) into global and per-type header maps.
   *
   * @param value - the {@code registryHeaders} value, may be {@code null}/blank
   * @return {@link ScopedHeaders} holding the global map and per-type maps
   */
  public static ScopedHeaders parseScoped(String value) {
    var global = new LinkedHashMap<String, String>();
    var byType = new EnumMap<RegistryType, Map<String, String>>(RegistryType.class);
    if (StringUtils.isBlank(value)) {
      return new ScopedHeaders(global, byType);
    }

    for (var group : value.split(GROUP_DELIMITER)) {
      var trimmedGroup = trim(group);
      if (StringUtils.isEmpty(trimmedGroup)) {
        continue;
      }

      var type = scopeOf(trimmedGroup);
      var headerList = type == null
        ? trimmedGroup
        : trimmedGroup.substring(trimmedGroup.indexOf(SCOPE_DELIMITER) + SCOPE_DELIMITER.length());

      var parsed = parseHeaders(headerList);
      if (type == null) {
        global.putAll(parsed);
      } else {
        byType.computeIfAbsent(type, key -> new LinkedHashMap<>()).putAll(parsed);
      }
    }

    return new ScopedHeaders(global, byType);
  }

  private static RegistryType scopeOf(String group) {
    var scopeIndex = group.indexOf(SCOPE_DELIMITER);
    if (scopeIndex <= 0) {
      return null;
    }

    var prefix = trim(group.substring(0, scopeIndex));
    for (var registryType : RegistryType.values()) {
      if (registryType.getValue().equalsIgnoreCase(prefix)) {
        return registryType;
      }
    }
    return null;
  }

  /**
   * Holds parsed {@code registryHeaders}: a global header map plus per-type overrides.
   */
  public record ScopedHeaders(Map<String, String> global, Map<RegistryType, Map<String, String>> byType) {

    /**
     * Returns the headers applicable to a registry of the given type: the global headers, with any
     * type-specific entries layered on top (type-specific wins on a name collision).
     */
    public Map<String, String> forType(RegistryType type) {
      var result = new LinkedHashMap<>(global);
      var typed = byType.get(type);
      if (typed != null) {
        result.putAll(typed);
      }
      return result;
    }

    public boolean isEmpty() {
      return global.isEmpty() && byType.values().stream().allMatch(Map::isEmpty);
    }
  }
}
