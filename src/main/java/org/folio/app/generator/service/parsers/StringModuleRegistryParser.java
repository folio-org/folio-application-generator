package org.folio.app.generator.service.parsers;

import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.folio.app.generator.utils.PluginUtils.PATH_DELIMITER;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.registry.OkapiModuleRegistry;
import org.folio.app.generator.model.registry.S3ModuleRegistry;

public class StringModuleRegistryParser {

  private final Pattern okapiPattern1 = Pattern.compile("(okapi)::(.{1,1024})::(.{1,1024})");
  private final Pattern okapiPattern2 = Pattern.compile("(okapi)::(.{1,1024})");
  private final Pattern s3Pattern1 = Pattern.compile("(s3)::(.{1,1024})::(.{1,1024})::(.{1,1024})");
  private final Pattern s3Pattern2 = Pattern.compile("(s3)::(.{1,1024})::(.{1,1024})");

  private final List<Pair<Pattern, Function<String[], ModuleRegistry>>> patterns = List.of(
    Pair.of(okapiPattern1, StringModuleRegistryParser::parseOkapiString),
    Pair.of(okapiPattern2, StringModuleRegistryParser::parseOkapiString),
    Pair.of(s3Pattern1, StringModuleRegistryParser::parseAwsS3String),
    Pair.of(s3Pattern2, StringModuleRegistryParser::parseAwsS3String));

  /**
   * Parses module registry string to a {@link ModuleRegistry} object.
   *
   * @param sourceValue - {@link String} value to parse.
   * @return {@link Optional} of {@link ModuleRegistry} object, it will be null if source string is not compatible
   */
  public Optional<ModuleRegistry> parse(String sourceValue) {
    for (var patternPair : patterns) {
      var pattern = patternPair.getLeft();
      var matcher = pattern.matcher(sourceValue);
      if (matcher.matches()) {
        var stringParts = convertToStringPartsArray(matcher);
        return Optional.of(patternPair.getRight().apply(stringParts));
      }
    }
    return Optional.empty();
  }

  private static String[] convertToStringPartsArray(Matcher matcher) {
    var groupCount = matcher.groupCount();
    var pathParts = new String[groupCount];
    for (int i = 1; i <= groupCount; i++) {
      pathParts[i - 1] = matcher.group(i);
    }
    return pathParts;
  }

  private static ModuleRegistry parseOkapiString(String[] stringParts) {
    var baseUrl = checkAndGetUrl(stringParts[1]);
    var verifiedUrl = baseUrl.toString();

    var s3ModuleRegistry = new OkapiModuleRegistry();
    s3ModuleRegistry.setUrl(verifiedUrl);

    if (stringParts.length == 3) {
      s3ModuleRegistry.setPublicUrl(trim(stringParts[2]));
    }

    return s3ModuleRegistry.withGeneratedFields();
  }

  private static URL checkAndGetUrl(String probablyUrl) {
    try {
      return new URL(removeEnd(probablyUrl, PATH_DELIMITER));
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid url provided: " + probablyUrl, e);
    }
  }

  private static S3ModuleRegistry parseAwsS3String(String[] stringParts) {
    var s3ModuleRegistry = new S3ModuleRegistry();
    var bucket = stringParts[1];
    var path = removeEnd(removeStart(trim(stringParts[2]), PATH_DELIMITER), PATH_DELIMITER);

    s3ModuleRegistry.setBucket(trim(bucket));
    s3ModuleRegistry.setPath(path.isEmpty() ? path : path + PATH_DELIMITER);

    if (stringParts.length == 4) {
      s3ModuleRegistry.setPublicUrl(trim(stringParts[3]));
    }

    return s3ModuleRegistry.withGeneratedFields();
  }
}
