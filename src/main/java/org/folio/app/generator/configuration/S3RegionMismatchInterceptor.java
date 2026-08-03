package org.folio.app.generator.configuration;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.maven.plugin.logging.Log;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.regions.Region;

/**
 * Logs a WARN the first time a request to an S3 bucket is served from a region that differs
 * from the plugin's configured {@code awsRegion}.
 *
 * <p>With {@code crossRegionAccessEnabled(true)} on the S3 client, region mismatches no longer
 * fail with a hard SigV4 error — the SDK transparently discovers the bucket's real region from
 * a {@code PermanentRedirect} response and re-signs the request. This interceptor preserves the
 * observability of that scenario: the first time we detect a mismatch for a bucket we emit one
 * WARN line pointing at the correct {@code awsRegion} value to set. Subsequent calls to the
 * same bucket are silent within a single Maven invocation.
 */
public class S3RegionMismatchInterceptor implements ExecutionInterceptor {

  private static final String BUCKET_REGION_HEADER = "x-amz-bucket-region";

  private final Log log;
  private final Region configuredRegion;
  private final Set<String> warnedBuckets = ConcurrentHashMap.newKeySet();

  public S3RegionMismatchInterceptor(Log log, Region configuredRegion) {
    this.log = log;
    this.configuredRegion = configuredRegion;
  }

  @Override
  public void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes attrs) {
    var bucketRegion = context.httpResponse()
      .firstMatchingHeader(BUCKET_REGION_HEADER)
      .orElse(null);
    if (bucketRegion == null || bucketRegion.equals(configuredRegion.id())) {
      return;
    }
    var bucket = context.request().getValueForField("Bucket", String.class).orElse(null);
    if (bucket == null || !warnedBuckets.add(bucket)) {
      return;
    }
    log.warn(String.format(
      "S3 bucket '%s' resides in region '%s' but plugin awsRegion is '%s'. "
        + "AWS SDK cross-region access re-signed and retried the request against the correct region. "
        + "To skip the redirect penalty on future runs, set <awsRegion>%s</awsRegion> in pom.xml "
        + "or pass -DawsRegion=%s on the command line.",
      bucket, bucketRegion, configuredRegion.id(), bucketRegion, bucketRegion));
  }
}
