package org.folio.app.generator.service.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.PreReleaseFilter;
import org.folio.app.generator.model.registry.S3ModuleRegistry;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.model.types.RegistryType;
import org.folio.app.generator.support.UnitTest;
import org.folio.app.generator.utils.PluginConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

@UnitTest
@ExtendWith(MockitoExtension.class)
class S3ModuleVersionResolverTest {

  private static final String S3_BUCKET = "test-bucket";
  private static final String S3_PATH = "modules/";

  @InjectMocks private S3ModuleVersionResolver resolver;
  @Mock private Log log;
  @Mock private S3Client s3Client;
  @Spy private final PluginConfig pluginConfig = PluginConfig.builder().awsS3BatchSize(5).build();

  @Test
  void getType_positive() {
    assertThat(resolver.getType()).isEqualTo(RegistryType.AWS_S3);
  }

  @Test
  void getAvailableVersions_positive_multipleVersions() {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var request = listObjectsRequest("modules/mod-foo-", null);
    var response = listObjectsResponse(
      s3Object("modules/mod-foo-1.2.0.json"),
      s3Object("modules/mod-foo-1.1.0.json"),
      s3Object("modules/mod-foo-1.0.0.json"));

    when(s3Client.listObjectsV2(request)).thenReturn(response);

    var result = resolver.getAvailableVersions(s3Registry(), dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.2.0", "1.1.0", "1.0.0");
  }

  @Test
  void getAvailableVersions_positive_singleVersion() {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var request = listObjectsRequest("modules/mod-foo-", null);
    var response = listObjectsResponse(s3Object("modules/mod-foo-1.0.0.json"));

    when(s3Client.listObjectsV2(request)).thenReturn(response);

    var result = resolver.getAvailableVersions(s3Registry(), dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.0.0");
  }

  @Test
  void getAvailableVersions_positive_preReleaseTrue() {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.TRUE);
    var request = listObjectsRequest("modules/mod-foo-", null);
    var response = listObjectsResponse(
      s3Object("modules/mod-foo-1.2.0-SNAPSHOT.json"),
      s3Object("modules/mod-foo-1.1.0.json"));

    when(s3Client.listObjectsV2(request)).thenReturn(response);

    var result = resolver.getAvailableVersions(s3Registry(), dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.2.0-SNAPSHOT", "1.1.0");
  }

  @Test
  void getAvailableVersions_positive_preReleaseFalseFiltersSnapshots() {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var request = listObjectsRequest("modules/mod-foo-", null);
    var response = listObjectsResponse(
      s3Object("modules/mod-foo-1.2.0-SNAPSHOT.json"),
      s3Object("modules/mod-foo-1.1.0.json"),
      s3Object("modules/mod-foo-1.0.0.json"));

    when(s3Client.listObjectsV2(request)).thenReturn(response);

    var result = resolver.getAvailableVersions(s3Registry(), dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.1.0", "1.0.0");
  }

  @Test
  void getAvailableVersions_positive_preReleaseOnlyFiltersStable() {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.ONLY);
    var request = listObjectsRequest("modules/mod-foo-", null);
    var response = listObjectsResponse(
      s3Object("modules/mod-foo-1.2.0-SNAPSHOT.json"),
      s3Object("modules/mod-foo-1.1.0-alpha.json"),
      s3Object("modules/mod-foo-1.0.0.json"));

    when(s3Client.listObjectsV2(request)).thenReturn(response);

    var result = resolver.getAvailableVersions(s3Registry(), dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.2.0-SNAPSHOT", "1.1.0-alpha");
  }

  @Test
  void getAvailableVersions_negative_moduleNotFound() {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var request = listObjectsRequest("modules/mod-foo-", null);
    var response = listObjectsResponse();

    when(s3Client.listObjectsV2(request)).thenReturn(response);

    var result = resolver.getAvailableVersions(s3Registry(), dependency, ModuleType.BE);

    assertThat(result).isEmpty();
    verify(log).warn("Module 'mod-foo' is not found in s3 bucket: test-bucket/modules/");
  }

  @Test
  void getAvailableVersions_negative_s3Exception() {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var request = listObjectsRequest("modules/mod-foo-", null);
    var exception = SdkClientException.create("Connection refused");

    when(s3Client.listObjectsV2(request)).thenThrow(exception);

    var result = resolver.getAvailableVersions(s3Registry(), dependency, ModuleType.BE);

    assertThat(result).isEmpty();
    verify(log).warn("Failed to list versions for module 'mod-foo' in s3 bucket: test-bucket/modules/", exception);
  }

  @Test
  void getAvailableVersions_positive_pagination() {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);

    var response1 = listObjectsResponse("ct1",
      s3Object("modules/mod-foo-1.0.0.json"),
      s3Object("modules/mod-foo-1.1.0.json"));

    var response2 = listObjectsResponse(
      s3Object("modules/mod-foo-1.2.0.json"));

    when(s3Client.listObjectsV2(listObjectsRequest("modules/mod-foo-", null))).thenReturn(response1);
    when(s3Client.listObjectsV2(listObjectsRequest("modules/mod-foo-", "ct1"))).thenReturn(response2);

    var result = resolver.getAvailableVersions(s3Registry(), dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.2.0", "1.1.0", "1.0.0");
  }

  @Test
  void getAvailableVersions_positive_nullPreReleaseDefaultsToFalse() {
    var dependency = new Dependency("mod-foo", "^1.0.0", null);
    var request = listObjectsRequest("modules/mod-foo-", null);
    var response = listObjectsResponse(
      s3Object("modules/mod-foo-1.1.0-SNAPSHOT.json"),
      s3Object("modules/mod-foo-1.0.0.json"));

    when(s3Client.listObjectsV2(request)).thenReturn(response);

    var result = resolver.getAvailableVersions(s3Registry(), dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.0.0");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("provideInvalidS3Objects")
  void getAvailableVersions_positive_filtersInvalidEntries(String testName, String invalidObjectKey) {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var request = listObjectsRequest("modules/mod-foo-", null);
    var response = listObjectsResponse(
      s3Object("modules/mod-foo-1.0.0.json"),
      s3Object(invalidObjectKey));

    when(s3Client.listObjectsV2(request)).thenReturn(response);

    var result = resolver.getAvailableVersions(s3Registry(), dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.0.0");
  }

  private static Stream<Arguments> provideInvalidS3Objects() {
    return Stream.of(
      Arguments.of("invalid module ID", "modules/mod-foo-invalid"),
      Arguments.of("different module with same prefix", "modules/mod-foo-storage-1.0.0.json"),
      Arguments.of("invalid semver version", "modules/mod-foo-1.x.0.json")
    );
  }

  private static ListObjectsV2Request listObjectsRequest(String prefix, String nct) {
    return ListObjectsV2Request.builder()
      .bucket(S3_BUCKET)
      .prefix(prefix)
      .maxKeys(5)
      .continuationToken(nct)
      .build();
  }

  private static ListObjectsV2Response listObjectsResponse(S3Object... s3Objects) {
    return listObjectsResponse(null, s3Objects);
  }

  private static ListObjectsV2Response listObjectsResponse(String nct, S3Object... s3Objects) {
    return ListObjectsV2Response.builder()
      .contents(s3Objects)
      .nextContinuationToken(nct)
      .isTruncated(nct != null)
      .build();
  }

  private static S3Object s3Object(String key) {
    return S3Object.builder()
      .key(key)
      .build();
  }

  private static S3ModuleRegistry s3Registry() {
    return new S3ModuleRegistry()
      .path(S3_PATH)
      .bucket(S3_BUCKET)
      .withGeneratedFields();
  }
}
