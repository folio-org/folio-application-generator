package org.folio.app.generator.service.artifact.existence;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.regex.Pattern;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.conditions.EcrCondition;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.artifact.ArtifactRegistry;
import org.folio.app.generator.model.types.ArtifactRegistryType;
import org.folio.app.generator.model.types.ErrorCategory;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ecr.model.EcrException;
import software.amazon.awssdk.services.ecr.model.ImageIdentifier;
import software.amazon.awssdk.services.ecr.model.ImageNotFoundException;
import software.amazon.awssdk.services.ecr.model.RepositoryNotFoundException;

@Component
@Conditional(EcrCondition.class)
public class EcrArtifactExistenceChecker implements ArtifactExistenceChecker {

  private static final Pattern ECR_REGISTRY_ID_PATTERN =
    Pattern.compile("^https?://(\\d+)\\.dkr\\.ecr\\.[^./]+\\.amazonaws\\.com(?:\\.cn)?(?:/.*)?$");

  private final EcrClient ecrClient;
  private final Log log;

  public EcrArtifactExistenceChecker(EcrClient ecrClient, Log log) {
    this.ecrClient = ecrClient;
    this.log = log;
  }

  @Override
  public ArtifactRegistryType getRegistryType() {
    return ArtifactRegistryType.AWS_ECR;
  }

  @Override
  public boolean exists(ModuleDefinition module, ArtifactRegistry registry) {
    var registryId = extractRegistryId(registry.getBaseUrl());
    var repositoryName = buildRepositoryName(registry.getNamespace(), module.getName());
    var imageTag = module.getVersion();

    log.debug(String.format("Checking ECR image existence: registryId=%s, repository=%s, tag=%s",
      registryId, repositoryName, imageTag));

    var request = DescribeImagesRequest.builder()
      .registryId(registryId)
      .repositoryName(repositoryName)
      .imageIds(ImageIdentifier.builder().imageTag(imageTag).build())
      .build();

    try {
      ecrClient.describeImages(request);
      log.debug(String.format("ECR image found: %s:%s", repositoryName, imageTag));
      return true;
    } catch (ImageNotFoundException e) {
      log.warn(String.format("ECR image not found: %s:%s (registryId: %s)",
        repositoryName, imageTag, registryId));
      return false;
    } catch (RepositoryNotFoundException e) {
      log.warn(String.format("ECR repository not found: %s (registryId: %s)",
        repositoryName, registryId));
      return false;
    } catch (EcrException e) {
      throw new ApplicationGeneratorException(
        String.format("ECR error %d for %s:%s (registryId: %s): %s",
          e.statusCode(), repositoryName, imageTag, registryId, e.getMessage()),
        ErrorCategory.INFRASTRUCTURE, e);
    }
  }

  private static String extractRegistryId(String baseUrl) {
    if (isBlank(baseUrl)) {
      throw new ApplicationGeneratorException(
        "ECR registry baseUrl is required (expected format: https://<accountId>.dkr.ecr.<region>.amazonaws.com)",
        ErrorCategory.CONFIGURATION_ERROR);
    }
    var matcher = ECR_REGISTRY_ID_PATTERN.matcher(baseUrl);
    if (!matcher.matches()) {
      throw new ApplicationGeneratorException(
        "Invalid ECR registry baseUrl: " + baseUrl
          + " (expected format: https://<accountId>.dkr.ecr.<region>.amazonaws.com)",
        ErrorCategory.CONFIGURATION_ERROR);
    }
    return matcher.group(1);
  }

  private static String buildRepositoryName(String namespace, String moduleName) {
    return isBlank(namespace) ? moduleName : namespace + "/" + moduleName;
  }
}
