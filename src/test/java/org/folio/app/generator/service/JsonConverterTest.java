package org.folio.app.generator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.SneakyThrows;
import org.folio.app.generator.configuration.SpringConfiguration;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.utils.JsonConverter;
import org.junit.jupiter.api.Test;

public class JsonConverterTest {

  private static final String RESOURCES_PATH = "src/test/resources/json/";
  private static final Path APPLICATION_WITH_UNKNOWN_PLATFORM_FIELD = Path.of(RESOURCES_PATH +
    "application-with-unknown-platform-field.json");
  private final JsonConverter jsonConverter = new JsonConverter(new SpringConfiguration().objectMapper());

  @Test
  @SneakyThrows
  void parseApplicationDescriptorTemplate_positive_withUnknownPlatformField() {
    var content = Files.readString(APPLICATION_WITH_UNKNOWN_PLATFORM_FIELD, StandardCharsets.UTF_8);
    var applicationDescriptorTemplate = jsonConverter.parse(content, ApplicationDescriptorTemplate.class);

    assertNotNull(applicationDescriptorTemplate);
    assertThat(applicationDescriptorTemplate.getId()).isEqualTo("app-consortia-1.0.0-SNAPSHOT");
  }

  @Test
  @SneakyThrows
  void parseApplicationDescriptor_positive_withUnknownPlatformField() {
    var content = Files.readString(APPLICATION_WITH_UNKNOWN_PLATFORM_FIELD, StandardCharsets.UTF_8);
    var applicationDescriptor = jsonConverter.parse(content, ApplicationDescriptor.class);

    assertNotNull(applicationDescriptor);
    assertThat(applicationDescriptor.getId()).isEqualTo("app-consortia-1.0.0-SNAPSHOT");
  }
}
