package org.folio.app.generator.service.loader;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.folio.app.generator.utils.PluginUtils.createModuleDefinitionFromId;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

  private static final List<String> FILE_EXTENSIONS = List.of("json");

  private final Log log;
  private final S3Client s3Client;
  private final PluginConfig pluginConfig;
  private final JsonConverter jsonConverter;

  @Override
  public Optional<Map<String, Object>> findModuleDescriptor(ModuleRegistry registry, ModuleDefinition module) {
    var s3Registry = (S3ModuleRegistry) registry;
    var version = module.getVersion();
    var id = module.getId();
    var filter = "latest".equals(version) ? module.getName() : module.getId();
    var fullPrefix = s3Registry.getPath() + filter;

    return findLatestVersionByPrefix(module, s3Registry, fullPrefix)
      .flatMap(foundS3Object -> readS3Object(id, foundS3Object, s3Registry));
  }

  @Override
  public RegistryType getType() {
    return RegistryType.AWS_S3;
  }

  private Optional<S3Object> findLatestVersionByPrefix(ModuleDefinition module, S3ModuleRegistry mr, String prefix) {
    var request = buildListObjectsRequest(mr, prefix, null);
    var moduleId = module.getId();

    ListObjectsV2Response result;
    Pair<Semver, S3Object> maxValueHolder = null;

    do {
      try {
        result = s3Client.listObjectsV2(request);
      } catch (Exception e) {
        log.warn(format("Failed to find module descriptor '%s' in s3 bucket: %s", moduleId, getBucketPath(mr)), e);
        return Optional.empty();
      }

      var s3ObjectsByPrefix = result.contents();
      if (s3ObjectsByPrefix.isEmpty()) {
        log.warn(format("Module '%s' is not found in s3 bucket: %s", moduleId, getBucketPath(mr)));
        return Optional.empty();
      }

      for (var s3Object : s3ObjectsByPrefix) {
        var nextMaxValue = tryFindNextMaxValue(mr.getPath(), module, s3Object, maxValueHolder);
        if (nextMaxValue != null) {
          maxValueHolder = nextMaxValue;
        }
      }

      request = buildListObjectsRequest(mr, prefix, result.nextContinuationToken());
    } while (TRUE.equals(result.isTruncated()));

    return ofNullable(maxValueHolder).map(Pair::getRight);
  }

  private static Pair<Semver, S3Object> tryFindNextMaxValue(String prefix, ModuleDefinition module,
    S3Object s3Object, Pair<Semver, S3Object> mv) {
    var moduleNameAndVersionPair = parseS3ObjectKey(s3Object, prefix);
    if (moduleNameAndVersionPair == null) {
      return null;
    }

    if (!Objects.equals(moduleNameAndVersionPair.getLeft(), module.getName())) {
      return null;
    }

    var semver = moduleNameAndVersionPair.getRight();
    if (mv == null) {
      return Pair.of(semver, s3Object);
    }

    var maxSemver = mv.getLeft();
    if (semver.compareTo(maxSemver) > 0) {
      return Pair.of(semver, s3Object);
    }

    return null;
  }

  private Optional<Map<String, Object>> readS3Object(String id, S3Object object, S3ModuleRegistry mr) {
    var request = GetObjectRequest.builder()
      .bucket(mr.getBucket())
      .key(object.key())
      .build();

    try {
      var responseBytes = s3Client.getObject(request, ResponseTransformer.toBytes());
      var inputStream = responseBytes.asInputStream();
      var moduleDescriptor = jsonConverter.parse(inputStream, new TypeReference<Map<String, Object>>() {});
      log.info(format("Module descriptor '%s' loaded from s3 bucket: %s", id, getBucketPath(mr)));
      return Optional.ofNullable(moduleDescriptor);
    } catch (Exception e) {
      log.warn(format("Failed to load module descriptor '%s' from s3 bucket: %s", id, getBucketPath(mr)), e);
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
}
