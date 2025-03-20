package org.folio.app.generator.service.parsers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Optional;
import java.util.stream.Stream;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.registry.OkapiModuleRegistry;
import org.folio.app.generator.model.registry.S3ModuleRegistry;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class StringModuleRegistryParserTest {

  @InjectMocks private StringModuleRegistryParser moduleRegistryProcessor;

  @DisplayName("parseRegistryString_parameterized")
  @ParameterizedTest(name = "[{index}] sourceString = {0}")
  @MethodSource("registryStringDataSource")
  void parseModuleRegistryString_parameterized(String value, ModuleRegistry expected) {
    var moduleRegistry = moduleRegistryProcessor.parse(value);
    assertThat(moduleRegistry).isEqualTo(Optional.ofNullable(expected));
  }

  @Test
  void parseModuleRegistryString_invalidUrl() {
    var sourceString = "okapi::invalid-url";
    assertThatThrownBy(() -> moduleRegistryProcessor.parse(sourceString))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid url provided: invalid-url");
  }

  public static Stream<Arguments> registryStringDataSource() {
    return Stream.of(
      arguments("", null),
      arguments(null, null),
      arguments("  ", null),
      arguments("http://localhost:3000", null),
      arguments("okapi::http://localhost:3000", okapiModuleRegistry("http://localhost:3000")),
      arguments("okapi::  http://localhost:3000", okapiModuleRegistry("http://localhost:3000")),
      arguments("okapi::http://localhost:3000/", okapiModuleRegistry("http://localhost:3000")),
      arguments("okapi::https://test-okapi.sample", okapiModuleRegistry("https://test-okapi.sample")),
      arguments("okapi::https://test-okapi.sample/", okapiModuleRegistry("https://test-okapi.sample")),
      arguments("  okapi::http://localhost:3000  ", okapiModuleRegistry("http://localhost:3000")),

      arguments("okapi::http://localhost:3000::https://test-okapi.sample/${id}",
        okapiModuleRegistry("http://localhost:3000", "https://test-okapi.sample/${id}")),
      arguments("okapi::  http://localhost:3000  :: https://test-okapi.sample/${id}  ",
        okapiModuleRegistry("http://localhost:3000", "https://test-okapi.sample/${id}")),
      arguments("okapi::https://test-okapi.sample::https://test-host.sample/okapi/${id}",
        okapiModuleRegistry("https://test-okapi.sample", "https://test-host.sample/okapi/${id}")),

      arguments("okapi::http://localhost:3000::https://test-simple.sample/${id}",
        okapiModuleRegistry("http://localhost:3000", "https://test-simple.sample/${id}")),
      arguments("okapi::  http://localhost:3000  :: https://test-simple.sample/${id}  ",
        okapiModuleRegistry("http://localhost:3000", "https://test-simple.sample/${id}")),
      arguments("okapi::https://test-simple.sample::https://test-host.sample/simple/${id}",
        okapiModuleRegistry("https://test-simple.sample", "https://test-host.sample/simple/${id}")),

      arguments("s3::test-bucket::/", s3ModuleRegistry("test-bucket", "")),
      arguments("s3::test-bucket::test", s3ModuleRegistry("test-bucket", "test/")),
      arguments("s3::test-bucket::/test", s3ModuleRegistry("test-bucket", "test/")),
      arguments("s3::  test-bucket  ::  /test", s3ModuleRegistry("test-bucket", "test/")),
      arguments("s3::foo-bucket::/foo/bar", s3ModuleRegistry("foo-bucket", "foo/bar/")),
      arguments("s3::foo-bucket::/foo/bar/", s3ModuleRegistry("foo-bucket", "foo/bar/")),

      arguments("s3::test-bucket::/foo/bar/::https://s3-alias.sample/${id}",
        s3ModuleRegistry("test-bucket", "foo/bar/", "https://s3-alias.sample/${id}"))
    );
  }

  private static Object okapiModuleRegistry(String url) {
    return okapiModuleRegistry(url, url + "/_/proxy/modules/{id}");
  }

  private static Object okapiModuleRegistry(String url, String publicUrlTemplate) {
    return new OkapiModuleRegistry()
      .url(url)
      .publicUrl(publicUrlTemplate);
  }

  private static S3ModuleRegistry s3ModuleRegistry(String bucket, String path) {
    var publicUrlTemplate = path.isEmpty()
      ? String.format("https://%s.s3.amazonaws.com/{id}", bucket)
      : String.format("https://%s.s3.amazonaws.com/%s{id}", bucket, path);
    return s3ModuleRegistry(bucket, path, publicUrlTemplate);
  }

  private static S3ModuleRegistry s3ModuleRegistry(String bucket, String path, String publicUrl) {
    return new S3ModuleRegistry()
      .bucket(bucket)
      .path(path)
      .publicUrl(publicUrl);
  }
}
