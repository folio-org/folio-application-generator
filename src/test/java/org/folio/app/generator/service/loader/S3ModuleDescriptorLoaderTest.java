package org.folio.app.generator.service.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.ResponseBytes.fromByteArray;
import static software.amazon.awssdk.core.sync.ResponseTransformer.toBytes;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.S3ModuleRegistry;
import org.folio.app.generator.model.types.RegistryType;
import org.folio.app.generator.support.UnitTest;
import org.folio.app.generator.utils.JsonConverter;
import org.folio.app.generator.utils.PluginConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

@UnitTest
@ExtendWith(MockitoExtension.class)
class S3ModuleDescriptorLoaderTest {

  private static final String S3_BUCKET = "test-bucket";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @InjectMocks private S3ModuleDescriptorLoader loader;
  @Mock private Log log;
  @Mock private S3Client s3Client;
  @Mock private JsonConverter jsonConverter;
  @Spy private final PluginConfig pluginConfig = PluginConfig.builder().awsS3BatchSize(5).build();

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(log, jsonConverter, pluginConfig);
  }

  @Test
  void getType_positive() {
    assertThat(loader.getType()).isEqualTo(RegistryType.AWS_S3);
  }

  @Test
  void findModuleDescriptor_positive_emptyResult() {
    var request = listObjectsRequest("mod-foo-1.0.0", null);
    var listObjectsResponse = listObjectsResponse();

    when(s3Client.listObjectsV2(request)).thenReturn(listObjectsResponse);

    var result = loader.findModuleDescriptor(s3Registry(), fooModule("1.0.0"));

    assertThat(result).isEmpty();
    verify(log).warn("Module 'mod-foo-1.0.0' is not found in s3 bucket: test-bucket/");
    verify(pluginConfig).getAwsS3BatchSize();
  }

  @Test
  void findModuleDescriptor_positive_singleModuleDescriptor() {
    var objectKey = "mod-foo-1.0.0.json";
    var request = listObjectsRequest("mod-foo-1.0.0", null);
    var listObjectsResponse = listObjectsResponse(s3Object(objectKey));
    var expectedModuleDescriptor = fooModuleDescriptor("1.0.0");
    var s3ObjectResponse = getObjectResponse(expectedModuleDescriptor);

    when(s3Client.listObjectsV2(request)).thenReturn(listObjectsResponse);
    when(s3Client.getObject(getObjectRequest(objectKey), toBytes())).thenReturn(s3ObjectResponse);
    when(jsonConverter.parse(any(InputStream.class), any())).thenReturn(expectedModuleDescriptor);

    var result = loader.findModuleDescriptor(s3Registry(), fooModule("1.0.0"));

    assertThat(result).contains(expectedModuleDescriptor);
    verify(log).info("Module descriptor 'mod-foo-1.0.0' loaded from s3 bucket: test-bucket/");
    verify(pluginConfig, times(2)).getAwsS3BatchSize();
  }

  @Test
  void findModuleDescriptor_negative_listRequestFailed() {
    var request = listObjectsRequest("mod-foo-1.0.0", null);
    var exception = SdkClientException.create("error");

    when(s3Client.listObjectsV2(request)).thenThrow(exception);

    var result = loader.findModuleDescriptor(s3Registry(), fooModule("1.0.0"));

    assertThat(result).isEmpty();
    verify(log).warn("Failed to find module descriptor 'mod-foo-1.0.0' in s3 bucket: test-bucket/", exception);
    verify(pluginConfig).getAwsS3BatchSize();
  }

  @Test
  void findModuleDescriptor_negative_s3ObjectReadingFailed() {
    var objectKey = "mod-foo-1.0.0.json";
    var request = listObjectsRequest("mod-foo-1.0.0", null);
    var listObjectsResponse = listObjectsResponse(s3Object(objectKey));

    when(s3Client.listObjectsV2(request)).thenReturn(listObjectsResponse);
    var exception = SdkClientException.create("error");
    when(s3Client.getObject(getObjectRequest(objectKey), toBytes())).thenThrow(exception);

    var result = loader.findModuleDescriptor(s3Registry(), fooModule("1.0.0"));

    assertThat(result).isEmpty();
    verify(log).warn("Failed to load module descriptor 'mod-foo-1.0.0' from s3 bucket: test-bucket/", exception);
    verify(pluginConfig, times(2)).getAwsS3BatchSize();
  }

  @Test
  void findModuleDescriptor_positive_singlePageWithSingleModuleDescriptor() {
    var objectKey = "mod-foo-1.0.0.json";
    var request = listObjectsRequest("mod-foo-1.0.0", null);
    var listObjectsResponse = listObjectsResponse(s3Object(objectKey));
    var expectedModuleDescriptor = fooModuleDescriptor("1.0.0");
    var s3ObjectResponse = getObjectResponse(expectedModuleDescriptor);

    when(s3Client.listObjectsV2(request)).thenReturn(listObjectsResponse);
    when(s3Client.getObject(getObjectRequest(objectKey), toBytes())).thenReturn(s3ObjectResponse);
    when(jsonConverter.parse(any(InputStream.class), any())).thenReturn(expectedModuleDescriptor);

    var result = loader.findModuleDescriptor(s3Registry(), fooModule("1.0.0"));

    assertThat(result).contains(expectedModuleDescriptor);
    verify(log).info("Module descriptor 'mod-foo-1.0.0' loaded from s3 bucket: test-bucket/");
    verify(pluginConfig, times(2)).getAwsS3BatchSize();
  }

  @Test
  void findModuleDescriptor_positive_multiplePages() {
    var objectKey = "mod-foo-1.0.0.json";
    var request = listObjectsRequest("mod-foo-1.0.0", null);
    var listObjectsResponse = listObjectsResponse(s3Object(objectKey));
    var expectedModuleDescriptor = fooModuleDescriptor("1.0.0");
    var s3ObjectResponse = getObjectResponse(expectedModuleDescriptor);

    when(s3Client.listObjectsV2(request)).thenReturn(listObjectsResponse);
    when(s3Client.getObject(getObjectRequest(objectKey), toBytes())).thenReturn(s3ObjectResponse);
    when(jsonConverter.parse(any(InputStream.class), any())).thenReturn(expectedModuleDescriptor);

    var result = loader.findModuleDescriptor(s3Registry(), fooModule("1.0.0"));

    assertThat(result).contains(expectedModuleDescriptor);
    verify(log).info("Module descriptor 'mod-foo-1.0.0' loaded from s3 bucket: test-bucket/");
    verify(pluginConfig, times(2)).getAwsS3BatchSize();
  }

  @Test
  void findModuleDescriptor_positive_multiplePagesWithSingleModuleDescriptor() {
    var listObjectsResponse1 = listObjectsResponse("ct1",
      s3Object("mod-foo-1.0.0-SNAPSHOT.1"),
      s3Object("mod-foo-1.0.0-SNAPSHOT.2"),
      s3Object("mod-foo-1.0.0-SNAPSHOT.3"));

    var listObjectsResponse2 = listObjectsResponse("ct2",
      s3Object("mod-foo-1.0.0-SNAPSHOT.4"),
      s3Object("mod-foo-1.0.0-SNAPSHOT.5"),
      s3Object("mod-foo-1.0.0-SNAPSHOT.6"));

    var listObjectsResponse3 = listObjectsResponse(
      s3Object("mod-foo-1.0.0-SNAPSHOT.7"),
      s3Object("mod-foo-1.0.0-SNAPSHOT.8"),
      s3Object("mod-foo-1.0.0-SNAPSHOT.9"));

    var objectKey = "mod-foo-1.0.0-SNAPSHOT.9";
    var expectedModuleDescriptor = fooModuleDescriptor("1.0.0-SNAPSHOT.9");
    var s3ObjectResponse = getObjectResponse(expectedModuleDescriptor);

    when(s3Client.listObjectsV2(listObjectsRequest("mod-foo", null))).thenReturn(listObjectsResponse1);
    when(s3Client.listObjectsV2(listObjectsRequest("mod-foo", "ct1"))).thenReturn(listObjectsResponse2);
    when(s3Client.listObjectsV2(listObjectsRequest("mod-foo", "ct2"))).thenReturn(listObjectsResponse3);
    when(s3Client.getObject(getObjectRequest(objectKey), toBytes())).thenReturn(s3ObjectResponse);
    when(jsonConverter.parse(any(InputStream.class), any())).thenReturn(expectedModuleDescriptor);

    var result = loader.findModuleDescriptor(s3Registry(), fooModule("latest"));

    assertThat(result).contains(expectedModuleDescriptor);

    verify(log).info("Module descriptor 'mod-foo-latest' loaded from s3 bucket: test-bucket/");
    verify(pluginConfig, times(4)).getAwsS3BatchSize();
  }

  @Test
  void findModuleDescriptor_positive_latestVersionSearch() {
    var request = listObjectsRequest("mod-foo", null);
    var listObjectsResponse = listObjectsResponse(
      s3Object("mod-foo-test"),
      s3Object("mod-foo-1.0.0.15"),
      s3Object("mod-foo-1.0.0-SNAPSHOT.4"),
      s3Object("mod-foo-1.0.0"),
      s3Object("mod-foo-1.0.0-SNAPSHOT.2"),
      s3Object("mod-foo-1.0.0-SNAPSHOT.3"),
      s3Object("mod-foo-1.0.0-SNAPSHOT.1.json"),
      s3Object("mod-foo-1.1.0-SNAPSHOT.6"),
      s3Object("mod-foo-1.1.0-SNAPSHOT.12"),
      s3Object("mod-foo-1.1.0"),
      s3Object("mod-foo-1.2.0-SNAPSHOT.198.json"),
      s3Object("mod-foo-1.2.0"),
      s3Object("mod-foo-1.2.0-SNAPSHOT.192"),
      s3Object("mod-foo-1.3.0-SNAPSHOT.287.json"));
    var expectedModuleDescriptor = fooModuleDescriptor("1.3.0-SNAPSHOT.287");
    var s3ObjectResponse = getObjectResponse(expectedModuleDescriptor);
    var objectKey = "mod-foo-1.3.0-SNAPSHOT.287.json";

    when(s3Client.listObjectsV2(request)).thenReturn(listObjectsResponse);
    when(s3Client.getObject(getObjectRequest(objectKey), toBytes())).thenReturn(s3ObjectResponse);
    when(jsonConverter.parse(any(InputStream.class), any())).thenReturn(expectedModuleDescriptor);

    var result = loader.findModuleDescriptor(s3Registry(), fooModule("latest"));

    assertThat(result).contains(expectedModuleDescriptor);

    verify(log).info("Module descriptor 'mod-foo-latest' loaded from s3 bucket: test-bucket/");
    verify(pluginConfig, times(2)).getAwsS3BatchSize();
  }

  @Test
  void findModuleDescriptor_positive_specificVersion() {
    var request = listObjectsRequest("mod-foo-1.0.0", null);
    var listObjectsResponse = listObjectsResponse(
      s3Object("mod-foo-1.0.0-SNAPSHOT.4"),
      s3Object("mod-foo-1.0.0-SNAPSHOT.2"),
      s3Object("mod-foo-1.0.0.1"),
      s3Object("mod-foo-1.0.0"),
      s3Object("mod-foo-1.0.0.18"),
      s3Object("mod-foo-1.0.0-SNAPSHOT.3"),
      s3Object("mod-foo-1.0.0-SNAPSHOT.1"));
    var expectedModuleDescriptor = fooModuleDescriptor("1.0.0");
    var s3ObjectResponse = getObjectResponse(expectedModuleDescriptor);
    var objectKey = "mod-foo-1.0.0";

    when(s3Client.listObjectsV2(request)).thenReturn(listObjectsResponse);
    when(s3Client.getObject(getObjectRequest(objectKey), toBytes())).thenReturn(s3ObjectResponse);
    when(jsonConverter.parse(any(InputStream.class), any())).thenReturn(expectedModuleDescriptor);

    var result = loader.findModuleDescriptor(s3Registry(), fooModule("1.0.0"));

    assertThat(result).contains(expectedModuleDescriptor);

    verify(log).info("Module descriptor 'mod-foo-1.0.0' loaded from s3 bucket: test-bucket/");
    verify(pluginConfig, times(2)).getAwsS3BatchSize();
  }

  @Test
  void findModuleDescriptor_positive_snapshotVersion() {
    var request = listObjectsRequest("mod-foo-1.0.0-SNAPSHOT", null);
    var listObjectsResponse = listObjectsResponse(
      s3Object("mod-foo-1.0.0-SNAPSHOT.4"),
      s3Object("mod-foo-1.0.0-SNAPSHOT.2"),
      s3Object("mod-foo-1.0.0-SNAPSHOT.3"),
      s3Object("mod-foo-1.0.0-SNAPSHOT.1"));
    var expectedModuleDescriptor = fooModuleDescriptor("1.0.0-SNAPSHOT.4");
    var s3ObjectResponse = getObjectResponse(expectedModuleDescriptor);
    var objectKey = "mod-foo-1.0.0-SNAPSHOT.4";

    when(s3Client.listObjectsV2(request)).thenReturn(listObjectsResponse);
    when(s3Client.getObject(getObjectRequest(objectKey), toBytes())).thenReturn(s3ObjectResponse);
    when(jsonConverter.parse(any(InputStream.class), any())).thenReturn(expectedModuleDescriptor);

    var result = loader.findModuleDescriptor(s3Registry(), fooModule("1.0.0-SNAPSHOT"));

    assertThat(result).contains(expectedModuleDescriptor);

    verify(log).info("Module descriptor 'mod-foo-1.0.0-SNAPSHOT' loaded from s3 bucket: test-bucket/");
    verify(pluginConfig, times(2)).getAwsS3BatchSize();
  }

  @Test
  void findModuleDescriptor_positive_latestVersionForModuleWithSamePrefix() {
    var request = listObjectsRequest("mod-foo", null);
    var listObjectsResponse = listObjectsResponse(
      s3Object("mod-foo-storage-1.2.0-SNAPSHOT.8"),
      s3Object("mod-foo-storage-1.2.0-SNAPSHOT.9"),
      s3Object("mod-foo-1.0.0-SNAPSHOT.1"),
      s3Object("mod-foo-1.0.0-SNAPSHOT.2"),
      s3Object("mod-foo-1.0.0-SNAPSHOT.3"));
    var expectedModuleDescriptor = fooModuleDescriptor("1.0.0-SNAPSHOT.3");
    var s3ObjectResponse = getObjectResponse(expectedModuleDescriptor);
    var objectKey = "mod-foo-1.0.0-SNAPSHOT.3";

    when(s3Client.listObjectsV2(request)).thenReturn(listObjectsResponse);
    when(s3Client.getObject(getObjectRequest(objectKey), toBytes())).thenReturn(s3ObjectResponse);
    when(jsonConverter.parse(any(InputStream.class), any())).thenReturn(expectedModuleDescriptor);

    var result = loader.findModuleDescriptor(s3Registry(), fooModule("latest"));

    assertThat(result).contains(expectedModuleDescriptor);

    verify(log).info("Module descriptor 'mod-foo-latest' loaded from s3 bucket: test-bucket/");
    verify(pluginConfig, times(2)).getAwsS3BatchSize();
  }

  private static Map<String, Object> fooModuleDescriptor(String version) {
    return Map.of(
      "id", "mod-foo" + "-" + version,
      "name", "Test name for module: " + "mod-foo",
      "description", "A description for module: " + "mod-foo"
    );
  }

  private static ModuleDefinition fooModule(String version) {
    return new ModuleDefinition().id("mod-foo-" + version).name("mod-foo").version(version);
  }

  private static GetObjectRequest getObjectRequest(String key) {
    return GetObjectRequest.builder()
      .key(key)
      .bucket(S3_BUCKET)
      .build();
  }

  @SneakyThrows
  private static ResponseBytes<GetObjectResponse> getObjectResponse(Object content) {
    var byteContent = OBJECT_MAPPER.writeValueAsBytes(content);
    return fromByteArray(GetObjectResponse.builder().build(), byteContent);
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
      .path("")
      .bucket(S3_BUCKET)
      .withGeneratedFields();
  }
}
