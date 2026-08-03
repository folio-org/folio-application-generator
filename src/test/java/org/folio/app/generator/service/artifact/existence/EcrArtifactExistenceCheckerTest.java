package org.folio.app.generator.service.artifact.existence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.artifact.EcrArtifactRegistry;
import org.folio.app.generator.model.types.ArtifactRegistryType;
import org.folio.app.generator.model.types.ErrorCategory;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ecr.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ecr.model.EcrException;
import software.amazon.awssdk.services.ecr.model.ImageNotFoundException;
import software.amazon.awssdk.services.ecr.model.RepositoryNotFoundException;

@UnitTest
@ExtendWith(MockitoExtension.class)
class EcrArtifactExistenceCheckerTest {

  private static final String ECR_BASE_URL = "https://123456789012.dkr.ecr.us-west-2.amazonaws.com";
  private static final String REGISTRY_ID = "123456789012";

  @Mock private EcrClient ecrClient;
  @Mock private Log log;

  private EcrArtifactExistenceChecker checker;

  @BeforeEach
  void setUp() {
    checker = new EcrArtifactExistenceChecker(ecrClient, log);
  }

  @Test
  void getRegistryType_positive() {
    assertThat(checker.getRegistryType()).isEqualTo(ArtifactRegistryType.AWS_ECR);
  }

  @Test
  void exists_positive_imageFound() {
    var module = new ModuleDefinition().name("mod-users").version("1.2.0-SNAPSHOT.5dc446");
    var registry = new EcrArtifactRegistry().baseUrl(ECR_BASE_URL).namespace("folio");

    when(ecrClient.describeImages(any(DescribeImagesRequest.class)))
      .thenReturn(DescribeImagesResponse.builder().build());

    assertThat(checker.exists(module, registry)).isTrue();
    verify(log).debug("ECR image found: folio/mod-users:1.2.0-SNAPSHOT.5dc446");
  }

  @Test
  void exists_positive_noNamespace() {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new EcrArtifactRegistry().baseUrl(ECR_BASE_URL);

    when(ecrClient.describeImages(any(DescribeImagesRequest.class)))
      .thenReturn(DescribeImagesResponse.builder().build());

    assertThat(checker.exists(module, registry)).isTrue();
    verify(log).debug("ECR image found: mod-users:1.0.0");
  }

  @Test
  void exists_negative_imageNotFound() {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new EcrArtifactRegistry().baseUrl(ECR_BASE_URL).namespace("folio");

    when(ecrClient.describeImages(any(DescribeImagesRequest.class)))
      .thenThrow(ImageNotFoundException.builder().message("Image not found").build());

    assertThat(checker.exists(module, registry)).isFalse();
    verify(log).warn("ECR image not found: folio/mod-users:1.0.0 (registryId: " + REGISTRY_ID + ")");
  }

  @Test
  void exists_negative_repositoryNotFound() {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new EcrArtifactRegistry().baseUrl(ECR_BASE_URL).namespace("folio");

    when(ecrClient.describeImages(any(DescribeImagesRequest.class)))
      .thenThrow(RepositoryNotFoundException.builder().message("Repository not found").build());

    assertThat(checker.exists(module, registry)).isFalse();
    verify(log).warn("ECR repository not found: folio/mod-users (registryId: " + REGISTRY_ID + ")");
  }

  @Test
  void exists_negative_ecrException_throwsInfrastructureError() {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new EcrArtifactRegistry().baseUrl(ECR_BASE_URL).namespace("folio");

    var ecrException = (EcrException) EcrException.builder()
      .statusCode(500)
      .message("Internal server error")
      .awsErrorDetails(AwsErrorDetails.builder().errorCode("InternalServerError").build())
      .build();
    when(ecrClient.describeImages(any(DescribeImagesRequest.class))).thenThrow(ecrException);

    assertThatThrownBy(() -> checker.exists(module, registry))
      .isInstanceOf(ApplicationGeneratorException.class)
      .hasMessageContaining("ECR error 500 for folio/mod-users:1.0.0")
      .satisfies(e -> assertThat(((ApplicationGeneratorException) e).getCategory())
        .isEqualTo(ErrorCategory.INFRASTRUCTURE));
  }

  @Test
  void exists_negative_blankBaseUrl_throwsConfigurationError() {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new EcrArtifactRegistry().namespace("folio");

    assertThatThrownBy(() -> checker.exists(module, registry))
      .isInstanceOf(ApplicationGeneratorException.class)
      .hasMessageContaining("ECR registry baseUrl is required")
      .satisfies(e -> assertThat(((ApplicationGeneratorException) e).getCategory())
        .isEqualTo(ErrorCategory.CONFIGURATION_ERROR));
  }

  @Test
  void exists_negative_invalidBaseUrl_throwsConfigurationError() {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new EcrArtifactRegistry().baseUrl("https://docker.io/v2").namespace("folio");

    assertThatThrownBy(() -> checker.exists(module, registry))
      .isInstanceOf(ApplicationGeneratorException.class)
      .hasMessageContaining("Invalid ECR registry baseUrl")
      .satisfies(e -> assertThat(((ApplicationGeneratorException) e).getCategory())
        .isEqualTo(ErrorCategory.CONFIGURATION_ERROR));
  }
}
