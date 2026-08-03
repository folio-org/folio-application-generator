package org.folio.app.generator.configuration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.folio.app.generator.conditions.EcrCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrClient;

/**
 * Supplies {@link EcrClient} instances keyed by AWS region, with per-region caching.
 *
 * <p>ECR endpoints are region-specific by URL construction ({@code
 * <account>.dkr.ecr.<region>.amazonaws.com}) and the AWS SDK does not offer a cross-region
 * access flag for ECR. To support builds that reference ECR registries across
 * different regions in a single Maven invocation, this factory caches one {@link EcrClient}
 * per region on first request, all sharing the ambient {@link DefaultCredentialsProvider}.
 */
@Component
@Conditional(EcrCondition.class)
public class EcrClientFactory {

  private final ConcurrentMap<Region, EcrClient> clientsByRegion = new ConcurrentHashMap<>();

  public EcrClient forRegion(Region region) {
    return clientsByRegion.computeIfAbsent(region, r -> EcrClient.builder()
      .region(r)
      .credentialsProvider(DefaultCredentialsProvider.create())
      .build());
  }
}
