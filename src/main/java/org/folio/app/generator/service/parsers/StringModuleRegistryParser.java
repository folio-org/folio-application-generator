package org.folio.app.generator.service.parsers;

import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.folio.app.generator.utils.PluginUtils.PATH_DELIMITER;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.registry.OkapiModuleRegistry;
import org.folio.app.generator.model.registry.S3ModuleRegistry;
import org.folio.app.generator.model.registry.SimpleModuleRegistry;

public class StringModuleRegistryParser {

  private static final String HEADERS_MARKER = "::headers=";
  private static final String HEADER_PAIR_DELIMITER = ";";
  private static final String HEADER_KEY_VALUE_DELIMITER = ":";

  private final Pattern okapiPattern1 = Pattern.compile("(okapi)::(.{1,1024})::(.{1,1024})");
  private final Pattern okapiPattern2 = Pattern.compile("(okapi)::(.{1,1024})");
  private final Pattern s3Pattern1 = Pattern.compile("(s3)::(.{1,1024})::(.{1,1024})::(.{1,1024})");
  private final Pattern s3Pattern2 = Pattern.compile("(s3)::(.{1,1024})::(.{1,1024})");
  private final Pattern simplePattern1 = Pattern.compile("(simple)::(.{1,1024})::(.{1,1024})");
  private final Pattern simplePattern2 = Pattern.compile("(simple)::(.{1,1024})");

  private final List<Pair<Pattern, BiFunction<String[], Map<String, String>, ModuleRegistry>>> patterns = List.of(
    Pair.of(okapiPattern1, StringModuleRegistryParser::parseOkapiString),
    Pair.of(okapiPattern2, StringModuleRegistryParser::parseOkapiString),
    Pair.of(s3Pattern1, StringModuleRegistryParser::parseAwsS3String),
    Pair.of(s3Pattern2, StringModuleRegistryParser::parseAwsS3String),
    Pair.of(simplePattern1, StringModuleRegistryParser::parseSimpleString),
    Pair.of(simplePattern2, StringModuleRegistryParser::parseSimpleString));

  /**
   * Parses module registry string to a {@link ModuleRegistry} object.
   *
   * @param registryString - {@link String} value to parse.
   * @return {@link Optional} of {@link ModuleRegistry} object, it will be null if source string is not compatible
   */
  public Optional<ModuleRegistry> parse(String registryString) {
    if (StringUtils.isBlank(registryString)) {
      return Optional.empty();
    }

    var value = registryString.trim();

    var headers = new LinkedHashMap<String, String>();
    var headersMarkerIndex = value.indexOf(HEADERS_MARKER);
    if (headersMarkerIndex >= 0) {
      headers.putAll(parseHeaders(value.substring(headersMarkerIndex + HEADERS_MARKER.length())));
      value = value.substring(0, headersMarkerIndex);
    }

    for (var patternPair : patterns) {
      var pattern = patternPair.getLeft();
      var matcher = pattern.matcher(value);
      if (matcher.matches()) {
        var stringParts = convertToStringPartsArray(matcher);
        return Optional.of(patternPair.getRight().apply(stringParts, headers));
      }
    }

    return Optional.empty();
  }

  private static Map<String, String> parseHeaders(String headersString) {
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

  private static String[] convertToStringPartsArray(Matcher matcher) {
    var groupCount = matcher.groupCount();
    var pathParts = new String[groupCount];
    for (int i = 1; i <= groupCount; i++) {
      pathParts[i - 1] = matcher.group(i);
    }
    return pathParts;
  }

  private static ModuleRegistry parseOkapiString(String[] stringParts, Map<String, String> headers) {
    var baseUrl = checkAndGetUrl(stringParts[1]);
    var verifiedUrl = baseUrl.toString();

    var registry = new OkapiModuleRegistry();
    registry.setUrl(verifiedUrl);
    registry.setHeaders(headers);

    if (stringParts.length == 3) {
      registry.setPublicUrl(trim(stringParts[2]));
    }

    return registry.withGeneratedFields();
  }

  private static ModuleRegistry parseSimpleString(String[] stringParts, Map<String, String> headers) {
    var baseUrl = checkAndGetUrl(stringParts[1]);
    var verifiedUrl = baseUrl.toString();

    var registry = new SimpleModuleRegistry();
    registry.setUrl(verifiedUrl);
    registry.setHeaders(headers);

    if (stringParts.length == 3) {
      registry.setPublicUrl(trim(stringParts[2]));
    }

    return registry.withGeneratedFields();
  }

  private static URL checkAndGetUrl(String probablyUrl) {
    try {
      return new URL(removeEnd(probablyUrl, PATH_DELIMITER));
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid url provided: " + probablyUrl, e);
    }
  }

  private static S3ModuleRegistry parseAwsS3String(String[] stringParts, Map<String, String> headers) {
    var s3ModuleRegistry = new S3ModuleRegistry();
    var bucket = stringParts[1];
    var path = removeEnd(removeStart(trim(stringParts[2]), PATH_DELIMITER), PATH_DELIMITER);

    s3ModuleRegistry.setBucket(trim(bucket));
    s3ModuleRegistry.setPath(path.isEmpty() ? path : path + PATH_DELIMITER);
    s3ModuleRegistry.setHeaders(headers);

    if (stringParts.length == 4) {
      s3ModuleRegistry.setPublicUrl(trim(stringParts[3]));
    }

    return s3ModuleRegistry.withGeneratedFields();
  }
}
