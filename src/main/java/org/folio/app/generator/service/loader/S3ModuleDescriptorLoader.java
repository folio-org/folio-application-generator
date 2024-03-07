package org.folio.app.generator.service.loader;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static org.folio.app.generator.utils.PluginUtils.createModuleDefinitionFromId;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.conditions.AwsCondition;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.registry.S3ModuleRegistry;
import org.folio.app.generator.model.types.RegistryType;
import org.folio.app.generator.utils.JsonConverter;
import org.folio.app.generator.utils.PluginConfig;
import org.semver4j.Semver;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

@Component
@RequiredArgsConstructor
@Conditional(AwsCondition.class)
public class S3ModuleDescriptorLoader implements ModuleDescriptorLoader {

  private final Log log;
  private final S3Client s3Client;
  private final PluginConfig pluginConfig;
  private final JsonConverter jsonConverter;

  @Override
  public Optional<Map<String, Object>> findModuleDescriptor(ModuleRegistry registry, ModuleDefinition module) {
    var s3Registry = (S3ModuleRegistry) registry;
    var version = module.getVersion();
    var filter = "latest".equals(version) ? module.getName() : module.getId();
    var fullPrefix = s3Registry.getPath() + filter;

    return findLatestVersionByPrefix(s3Registry, fullPrefix)
      .flatMap(foundS3Object -> readS3Object(foundS3Object, s3Registry));
  }

  @Override
  public RegistryType getType() {
    return RegistryType.AWS_S3;
  }

  private Optional<S3Object> findLatestVersionByPrefix(S3ModuleRegistry s3Registry, String prefix) {
    var request = buildListObjectsRequest(s3Registry, prefix, null);

    ListObjectsV2Response result;
    Pair<Semver, S3Object> maxValueHolder = null;

    do {
      result = s3Client.listObjectsV2(request);
      var s3ObjectsByPrefix = result.contents();
      if (s3ObjectsByPrefix.isEmpty()) {
        return Optional.empty();
      }

      for (var s3Object : s3ObjectsByPrefix) {
        var nextMaxValue = tryFindNextMaxValue(s3Registry.getPath(), s3Object, maxValueHolder);
        if (nextMaxValue != null) {
          maxValueHolder = nextMaxValue;
        }
      }

      request = buildListObjectsRequest(s3Registry, prefix, result.nextContinuationToken());
    } while (TRUE.equals(result.isTruncated()));

    return ofNullable(maxValueHolder).map(Pair::getRight);
  }

  private static Pair<Semver, S3Object> tryFindNextMaxValue(String prefix, S3Object o, Pair<Semver, S3Object> mv) {
    var currentSemver = parseModuleVersion(o, prefix);
    if (mv == null) {
      return Pair.of(currentSemver, o);
    }

    if (currentSemver == null) {
      return null;
    }

    var maxSemver = mv.getLeft();
    if (maxSemver == null || currentSemver.compareTo(maxSemver) > 0) {
      return Pair.of(currentSemver, o);
    }

    return null;
  }

  private Optional<Map<String, Object>> readS3Object(S3Object object, S3ModuleRegistry registry) {
    var request = GetObjectRequest.builder()
      .bucket(registry.getBucket())
      .key(object.key())
      .build();

    var responseBytes = s3Client.getObject(request, ResponseTransformer.toBytes());
    try {
      return Optional.ofNullable(jsonConverter.parse(responseBytes.asInputStream(), new TypeReference<>() {}));
    } catch (Exception exception) {
      log.warn("Failed to get content from s3 object by key: " + object.key(), exception);
      return Optional.empty();
    }
  }

  private ListObjectsV2Request buildListObjectsRequest(S3ModuleRegistry registry, String prefix, String nct) {
    return ListObjectsV2Request.builder()
      .bucket(registry.getBucket())
      .prefix(prefix)
      .maxKeys(pluginConfig.getAwsS3BatchSize())
      .continuationToken(nct)
      .build();
  }

  private static Semver parseModuleVersion(S3Object s3Object, String pathPrefix) {
    var s3ObjectKey = s3Object.key();
    var fileName = s3ObjectKey.substring(pathPrefix.length());
    if (!fileName.endsWith(".json")) {
      return null;
    }

    return createModuleDefinitionFromId(fileName.substring(0, fileName.length() - 5))
      .map(ModuleDefinition::getVersion)
      .map(Semver::parse)
      .orElse(null);
  }
}
