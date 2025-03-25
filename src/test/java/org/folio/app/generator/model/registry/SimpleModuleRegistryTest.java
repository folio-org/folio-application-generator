package org.folio.app.generator.model.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.util.stream.Stream;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
public class SimpleModuleRegistryTest {

  private static final String MOCK_URL = "http://localhost";

  @InjectMocks private SimpleModuleRegistry simpleModuleRegistry;

  @Test
  void getUrl_positive() {
    simpleModuleRegistry.url(MOCK_URL);
    assertThat(getField(simpleModuleRegistry, "url")).isEqualTo(MOCK_URL);
  }

  @Test
  void getPublicUrl_positive() {
    simpleModuleRegistry.publicUrl(MOCK_URL);
    assertThat(getField(simpleModuleRegistry, "publicUrl")).isEqualTo(MOCK_URL);
  }

  @Test
  void isValid_positive_returnsTrue() {
    setField(simpleModuleRegistry, "url", MOCK_URL);
    assertTrue(simpleModuleRegistry.isValid());
  }

  @DisplayName("isValid_positive_returnsFalse")
  @ParameterizedTest(name = "[{index}] url = {0}")
  @ValueSource(strings = { "", "Malformed URL" })
  void isValid_positive_returnsFalse(String url) {
    setField(simpleModuleRegistry, "url", url);
    assertFalse(simpleModuleRegistry.isValid());
  }

  @DisplayName("parseRegistryString_parameterized")
  @ParameterizedTest(name = "[{index}] publicUrl = {0}, expectUrl = {1}")
  @MethodSource("isWithGeneratedFieldsSource")
  void isWithGeneratedFields_positive(String publicUrl, String expectUrl) {
    SimpleModuleRegistry smr = new SimpleModuleRegistry()
      .url(MOCK_URL)
      .publicUrl(publicUrl)
      .withGeneratedFields();

    assertThat(getField(smr, "publicUrl")).isEqualTo(expectUrl);
  }

  public static Stream<Arguments> isWithGeneratedFieldsSource() {
    return Stream.of(
      arguments(null, MOCK_URL + "/{id}"),
      arguments(MOCK_URL, MOCK_URL)
    );
  }
}
