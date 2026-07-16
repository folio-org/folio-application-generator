package org.folio.app.generator.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.folio.app.generator.support.UnitTest;
import org.folio.app.generator.utils.PluginConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.s3.S3Client;

@UnitTest
class SpringConfigurationTest {

  private SpringConfiguration configuration;

  @BeforeEach
  void setUp() {
    configuration = new SpringConfiguration();
  }

  @Test
  void httpClient_positive_isCreated() {
    var httpClient = configuration.httpClient();

    assertThat(httpClient).isNotNull();
  }

  @Test
  void amazonS3Client_positive_withoutEndpointOverride() {
    var config = PluginConfig.builder().awsRegion(Region.US_EAST_1).build();

    try (S3Client client = configuration.amazonS3Client(config)) {
      assertThat(client).isNotNull();
    }
  }

  @Test
  void amazonS3Client_positive_withEndpointOverride() {
    var config = PluginConfig.builder()
      .awsRegion(Region.US_EAST_1)
      .awsEndpointOverride(URI.create("http://localhost:4566"))
      .build();

    try (S3Client client = configuration.amazonS3Client(config)) {
      assertThat(client).isNotNull();
    }
  }

  @Test
  void ecrClient_positive_isCreated() {
    var config = PluginConfig.builder().awsRegion(Region.US_WEST_2).build();

    try (EcrClient client = configuration.ecrClient(config)) {
      assertThat(client).isNotNull();
    }
  }
}
