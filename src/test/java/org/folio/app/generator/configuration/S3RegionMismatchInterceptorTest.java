package org.folio.app.generator.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;

@UnitTest
@ExtendWith(MockitoExtension.class)
class S3RegionMismatchInterceptorTest {

  @Mock private Log log;
  @Mock private Context.AfterUnmarshalling context;
  @Mock private SdkHttpResponse httpResponse;
  @Mock private SdkRequest sdkRequest;
  @Mock private ExecutionAttributes executionAttributes;

  private S3RegionMismatchInterceptor interceptor;

  @BeforeEach
  void setUp() {
    interceptor = new S3RegionMismatchInterceptor(log, Region.US_EAST_1);
  }

  @Test
  void afterUnmarshalling_positive_warnsOnMismatch() {
    stubResponseHeader("us-west-2");
    stubBucket("bucket-in-uswest2");

    interceptor.afterUnmarshalling(context, executionAttributes);

    var messageCaptor = ArgumentCaptor.forClass(String.class);
    verify(log).warn(messageCaptor.capture());
    assertThat(messageCaptor.getValue())
      .contains("bucket-in-uswest2")
      .contains("us-west-2")
      .contains("us-east-1")
      .contains("<awsRegion>us-west-2</awsRegion>");
  }

  @Test
  void afterUnmarshalling_positive_noWarnOnMatch() {
    stubResponseHeader("us-east-1");

    interceptor.afterUnmarshalling(context, executionAttributes);

    verify(log, never()).warn(any(String.class));
  }

  @Test
  void afterUnmarshalling_positive_noWarnWhenHeaderAbsent() {
    when(context.httpResponse()).thenReturn(httpResponse);
    when(httpResponse.firstMatchingHeader("x-amz-bucket-region")).thenReturn(Optional.empty());

    interceptor.afterUnmarshalling(context, executionAttributes);

    verify(log, never()).warn(any(String.class));
  }

  @Test
  void afterUnmarshalling_positive_warnsOncePerBucket() {
    stubResponseHeader("us-west-2");
    stubBucket("same-bucket");

    interceptor.afterUnmarshalling(context, executionAttributes);
    interceptor.afterUnmarshalling(context, executionAttributes);

    verify(log, times(1)).warn(contains("same-bucket"));
  }

  @Test
  void afterUnmarshalling_positive_warnsSeparatelyForDifferentBuckets() {
    stubResponseHeader("us-west-2");
    when(sdkRequest.getValueForField("Bucket", String.class))
      .thenReturn(Optional.of("first-bucket"))
      .thenReturn(Optional.of("second-bucket"));
    when(context.request()).thenReturn(sdkRequest);

    interceptor.afterUnmarshalling(context, executionAttributes);
    interceptor.afterUnmarshalling(context, executionAttributes);

    verify(log).warn(contains("first-bucket"));
    verify(log).warn(contains("second-bucket"));
    verify(log, times(2)).warn(any(String.class));
  }

  @Test
  void afterUnmarshalling_positive_noWarnWhenBucketFieldMissing() {
    stubResponseHeader("us-west-2");
    when(sdkRequest.getValueForField("Bucket", String.class)).thenReturn(Optional.empty());
    when(context.request()).thenReturn(sdkRequest);

    interceptor.afterUnmarshalling(context, executionAttributes);

    verify(log, never()).warn(any(String.class));
  }

  private void stubResponseHeader(String bucketRegion) {
    when(context.httpResponse()).thenReturn(httpResponse);
    when(httpResponse.firstMatchingHeader("x-amz-bucket-region")).thenReturn(Optional.of(bucketRegion));
  }

  private void stubBucket(String bucket) {
    when(context.request()).thenReturn(sdkRequest);
    when(sdkRequest.getValueForField("Bucket", String.class)).thenReturn(Optional.of(bucket));
  }
}
