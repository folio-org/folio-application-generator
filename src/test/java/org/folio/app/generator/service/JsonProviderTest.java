package org.folio.app.generator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.model.UpdateResult;
import org.folio.app.generator.support.UnitTest;
import org.folio.app.generator.utils.JsonConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class JsonProviderTest {

  private static final String PATH = "src/test/resources/json/";
  private static final String APPLICATION_JSON = "{ \"id\": \"app-consortia-1.0.0-SNAPSHOT\" }\n";
  private final JsonConverter jsonConverter = mock(JsonConverter.class);
  private final JsonProvider jsonProvider = new JsonProvider(mock(Log.class), jsonConverter, getMavenProject());

  @Test
  @SneakyThrows
  void readJsonFromFile_positive_readWithoutSubstitution() {
    jsonProvider.readJsonFromFile(PATH + "application.json", ApplicationDescriptor.class, true);

    verify(jsonConverter).parse(APPLICATION_JSON, ApplicationDescriptor.class);
  }

  @Test
  @SneakyThrows
  void readJsonFromFile_positive_readWithSubstitution() {
    jsonProvider.readJsonFromFile(PATH + "application.json", ApplicationDescriptorTemplate.class, true);

    verify(jsonConverter).parse(APPLICATION_JSON, ApplicationDescriptorTemplate.class);
  }

  @Test
  void readJsonFromFile_negative_fileNotFound() {
    var message = assertThrows(MojoExecutionException.class,
      () -> jsonProvider.readJsonFromFile("invalid/path/application.json", ApplicationDescriptor.class, true))
      .getMessage();
    assertThat(message).contains("ApplicationDescriptor is not found");
  }

  @Test
  @SneakyThrows
  void writeApplication_positive() {
    var application = new ApplicationDescriptor().id("test-app");
    jsonProvider.writeApplication(application, PATH);

    verify(jsonConverter).writeValue(any(File.class), eq(application));
  }

  @Test
  @SneakyThrows
  void writeUpdateResult_positive() {
    var updateResult = new UpdateResult(
      List.of("mod-new-1.0.0"), List.of("mod-users (1.0.0 -> 2.0.0)"), List.of(), List.of(),
      List.of(), List.of(), List.of(), List.of());

    jsonProvider.writeUpdateResult(updateResult, PATH);

    verify(jsonConverter).writeValue(any(File.class), eq(updateResult));
  }

  @Test
  @SneakyThrows
  void writeUpdateResult_positive_createsDirectory() {
    var tempDir = Files.createTempDirectory("json-provider-test");
    var newDir = new File(tempDir.toFile(), "new-subdir");
    var updateResult = new UpdateResult(
      List.of(), List.of(), List.of(), List.of(),
      List.of(), List.of(), List.of(), List.of());

    jsonProvider.writeUpdateResult(updateResult, newDir.getAbsolutePath());

    verify(jsonConverter).writeValue(any(File.class), eq(updateResult));
    assertThat(newDir).exists();

    newDir.delete();
    tempDir.toFile().delete();
  }

  @Test
  @SneakyThrows
  void writeUpdateResult_negative_cannotCreateDirectory() {
    var tempFile = Files.createTempFile("test-file", ".tmp");
    var invalidPath = tempFile.toAbsolutePath() + "/subdir";
    var updateResult = new UpdateResult(
      List.of(), List.of(), List.of(), List.of(),
      List.of(), List.of(), List.of(), List.of());

    try {
      var exception = assertThrows(MojoExecutionException.class,
        () -> jsonProvider.writeUpdateResult(updateResult, invalidPath));

      assertThat(exception.getMessage()).contains("Could not create target directory");
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  private static MavenProject getMavenProject() {
    var project = new MavenProject();
    project.setName("TestProject");
    project.setVersion("1.0.0");
    project.setGroupId("com.example");
    project.setDescription("Description");
    return project;
  }
}
