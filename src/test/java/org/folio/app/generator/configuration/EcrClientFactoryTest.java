package org.folio.app.generator.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrClient;

@UnitTest
class EcrClientFactoryTest {

  private EcrClientFactory factory;

  @BeforeEach
  void setUp() {
    factory = new EcrClientFactory();
  }

  @Test
  void forRegion_positive_returnsClient() {
    try (EcrClient client = factory.forRegion(Region.US_WEST_2)) {
      assertThat(client).isNotNull();
    }
  }

  @Test
  void forRegion_positive_cachesClientPerRegion() {
    try (EcrClient first = factory.forRegion(Region.US_WEST_2);
         EcrClient second = factory.forRegion(Region.US_WEST_2)) {
      assertThat(second).isSameAs(first);
    }
  }

  @Test
  void forRegion_positive_differentRegionsReturnDifferentInstances() {
    try (EcrClient usWest = factory.forRegion(Region.US_WEST_2);
         EcrClient euCentral = factory.forRegion(Region.EU_CENTRAL_1)) {
      assertThat(euCentral).isNotSameAs(usWest);
    }
  }
}
