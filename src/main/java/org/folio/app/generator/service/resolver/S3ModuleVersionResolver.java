package org.folio.app.generator.service.resolver;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static org.folio.app.generator.utils.PluginUtils.createModuleDefinitionFromId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.conditions.AwsCondition;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.PreReleaseFilter;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.registry.S3ModuleRegistry;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.model.types.RegistryType;
import org.folio.app.generator.utils.PluginConfig;
import org.semver4j.Semver;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

@Component
@Conditional(AwsCondition.class)
@RequiredArgsConstructor
public class S3ModuleVersionResolver implements ModuleVersionResolver {

  private static final List<String> FILE_EXTENSIONS = List.of("json");

  private final Log log;
  private final S3Client s3Client;
  private final PluginConfig pluginConfig;

  @Override
  public Optional<List<String>> getAvailableVersions(ModuleRegistry registry, Dependency dependency, ModuleType type) {
    var s3Registry = (S3ModuleRegistry) registry;
    var moduleName = dependency.getName();
    var preReleaseFilter = dependency.getPreRelease();
    var prefix = s3Registry.getPath() + moduleName + "-";

    var request = buildListObjectsRequest(s3Registry, prefix, null);
    ListObjectsV2Response result;
    var collected = new ArrayList<Pair<String, Semver>>();

    do {
      try {
        result = s3Client.listObjectsV2(request);
      } catch (Exception e) {
        log.warn(format("Failed to list versions for module '%s' in s3 bucket: %s",
              moduleName, getBucketPath(s3Registry)), e);
        return Optional.empty();
      }

      for (S3Object s3Object : result.contents()) {
        Pair<String, Semver> parsed = parseS3ObjectKey(s3Object, s3Registry.getPath());

        // Defensive check: parsed.getRight() != null is currently unreachable since parseS3ObjectKey
        // filters out null Semver values (line 111), but kept for safety in case implementation changes
        if (parsed != null && parsed.getLeft().equals(moduleName)
            && parsed.getRight() != null && matchesPreReleaseFilter(parsed.getRight(), preReleaseFilter)) {
          collected.add(parsed);
        }
      }

      request = buildListObjectsRequest(s3Registry, prefix, result.nextContinuationToken());
    } while (TRUE.equals(result.isTruncated()));

    if (collected.isEmpty()) {
      log.warn(format("Module '%s' is not found in s3 bucket: %s", moduleName, getBucketPath(s3Registry)));
      return Optional.empty();
    }

    log.debug(format("Found %d versions for module '%s' in s3 bucket: %s", collected.size(),
          moduleName, getBucketPath(s3Registry)));

    collected.sort(Comparator.comparing(Pair<String, Semver>::getRight).reversed());
    return Optional.of(collected.stream().map(p -> p.getRight().getVersion()).toList());
  }

  @Override
  public RegistryType getType() {
    return RegistryType.AWS_S3;
  }

  private ListObjectsV2Request buildListObjectsRequest(S3ModuleRegistry registry, String prefix, String nct) {
    return ListObjectsV2Request.builder()
      .bucket(registry.getBucket())
      .prefix(prefix)
      .maxKeys(pluginConfig.getAwsS3BatchSize())
      .continuationToken(nct)
      .build();
  }

  private static Pair<String, Semver> parseS3ObjectKey(S3Object s3Object, String pathPrefix) {
    var s3ObjectKey = s3Object.key();
    var fileName = s3ObjectKey.substring(pathPrefix.length());

    var moduleDescriptorId = FILE_EXTENSIONS.stream()
      .filter(extension -> fileName.endsWith("." + extension))
      .findFirst()
      .map(extension -> fileName.substring(0, fileName.length() - extension.length() - 1))
      .orElse(fileName);

    return createModuleDefinitionFromId(moduleDescriptorId)
      .map(md -> Pair.of(md.getName(), Semver.parse(md.getVersion())))
      .filter(pair -> pair.getRight() != null)
      .orElse(null);
  }

  private static String getBucketPath(S3ModuleRegistry s3ModuleRegistry) {
    return s3ModuleRegistry.getBucket() + "/" + s3ModuleRegistry.getPath();
  }

  private static boolean matchesPreReleaseFilter(Semver semver, PreReleaseFilter filter) {
    var effective = filter == null ? PreReleaseFilter.TRUE : filter;
    var isPreRelease = !semver.getPreRelease().isEmpty();

    return switch (effective) {
      case TRUE -> true;
      case ONLY -> isPreRelease;
      case FALSE -> !isPreRelease;
    };
  }
}
