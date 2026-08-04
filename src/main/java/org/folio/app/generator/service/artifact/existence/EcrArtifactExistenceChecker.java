package org.folio.app.generator.service.artifact.existence;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.conditions.EcrCondition;
import org.folio.app.generator.configuration.EcrClientFactory;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.artifact.ArtifactRegistry;
import org.folio.app.generator.model.types.ArtifactRegistryType;
import org.folio.app.generator.model.types.ErrorCategory;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ecr.model.EcrException;
import software.amazon.awssdk.services.ecr.model.ImageIdentifier;
import software.amazon.awssdk.services.ecr.model.ImageNotFoundException;
import software.amazon.awssdk.services.ecr.model.RepositoryNotFoundException;

@Component
@Conditional(EcrCondition.class)
public class EcrArtifactExistenceChecker implements ArtifactExistenceChecker {

  private static final Pattern ECR_URL_PATTERN =
    Pattern.compile("^https?://(\\d+)\\.dkr\\.ecr\\.([^./]+)\\.amazonaws\\.com(?:\\.cn)?(?:/.*)?$");

  private final EcrClientFactory ecrClientFactory;
  private final Log log;

  public EcrArtifactExistenceChecker(EcrClientFactory ecrClientFactory, Log log) {
    this.ecrClientFactory = ecrClientFactory;
    this.log = log;
  }

  @Override
  public ArtifactRegistryType getRegistryType() {
    return ArtifactRegistryType.AWS_ECR;
  }

  @Override
  public boolean exists(ModuleDefinition module, ArtifactRegistry registry) {
    var endpoint = parseEndpoint(registry.getBaseUrl());
    var repositoryName = buildRepositoryName(registry.getNamespace(), module.getName());
    var imageTag = module.getVersion();

    log.debug(String.format("Checking ECR image existence: registryId=%s, region=%s, repository=%s, tag=%s",
      endpoint.registryId(), endpoint.region().id(), repositoryName, imageTag));

    var request = DescribeImagesRequest.builder()
      .registryId(endpoint.registryId())
      .repositoryName(repositoryName)
      .imageIds(ImageIdentifier.builder().imageTag(imageTag).build())
      .build();

    var client = ecrClientFactory.forRegion(endpoint.region());
    try {
      client.describeImages(request);
      log.debug(String.format("ECR image found: %s:%s", repositoryName, imageTag));
      return true;
    } catch (ImageNotFoundException e) {
      log.warn(String.format("ECR image not found: %s:%s (registryId: %s)",
        repositoryName, imageTag, endpoint.registryId()));
      return false;
    } catch (RepositoryNotFoundException e) {
      log.warn(String.format("ECR repository not found: %s (registryId: %s)",
        repositoryName, endpoint.registryId()));
      return false;
    } catch (EcrException e) {
      throw new ApplicationGeneratorException(
        String.format("ECR error %d for %s:%s (registryId: %s): %s",
          e.statusCode(), repositoryName, imageTag, endpoint.registryId(), e.getMessage()),
        ErrorCategory.INFRASTRUCTURE, e);
    }
  }

  private static EcrEndpoint parseEndpoint(String baseUrl) {
    if (isBlank(baseUrl)) {
      throw new ApplicationGeneratorException(
        "ECR registry baseUrl is required (expected format: https://<accountId>.dkr.ecr.<region>.amazonaws.com)",
        ErrorCategory.CONFIGURATION_ERROR);
    }
    Matcher matcher = ECR_URL_PATTERN.matcher(baseUrl);
    if (!matcher.matches()) {
      throw new ApplicationGeneratorException(
        "Invalid ECR registry baseUrl: " + baseUrl
          + " (expected format: https://<accountId>.dkr.ecr.<region>.amazonaws.com)",
        ErrorCategory.CONFIGURATION_ERROR);
    }
    return new EcrEndpoint(matcher.group(1), Region.of(matcher.group(2)));
  }

  private static String buildRepositoryName(String namespace, String moduleName) {
    return isBlank(namespace) ? moduleName : namespace + "/" + moduleName;
  }

  private record EcrEndpoint(String registryId, Region region) { }
}
