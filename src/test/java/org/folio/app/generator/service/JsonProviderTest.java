package org.folio.app.generator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.model.ExecutionResult;
import org.folio.app.generator.model.UpdateResult;
import org.folio.app.generator.model.types.ErrorCategory;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;
import org.folio.app.generator.support.UnitTest;
import org.folio.app.generator.utils.JsonConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class JsonProviderTest {

  private static final String PATH = "src/test/resources/json/";
  private static final String APPLICATION_JSON = "{ \"id\": \"app-consortia-1.0.0-SNAPSHOT\" }";
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
    var message = assertThrows(ApplicationGeneratorException.class,
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
      var exception = assertThrows(ApplicationGeneratorException.class,
        () -> jsonProvider.writeUpdateResult(updateResult, invalidPath));

      assertThat(exception.getMessage()).contains("Could not create target directory");
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  @Test
  @SneakyThrows
  void writeUpdateResult_negative_directoryNotWritable() {
    var tempDir = Files.createTempDirectory("json-provider-readonly-test");
    var updateResult = new UpdateResult(
      List.of(), List.of(), List.of(), List.of(),
      List.of(), List.of(), List.of(), List.of());

    try {
      var permissionChanged = tempDir.toFile().setWritable(false);
      assumeTrue(permissionChanged && !tempDir.toFile().canWrite(),
        "Skipping test: cannot make directory non-writable on this system");

      var targetPath = tempDir.toAbsolutePath().toString();
      var exception = assertThrows(ApplicationGeneratorException.class,
        () -> jsonProvider.writeUpdateResult(updateResult, targetPath));

      assertThat(exception.getMessage()).contains("Target directory is not writable");
    } finally {
      tempDir.toFile().setWritable(true);
      Files.deleteIfExists(tempDir);
    }
  }

  @Test
  @SneakyThrows
  void readJsonFromFile_negative_pathIsDirectory() {
    var tempDir = Files.createTempDirectory("json-provider-dir-test");

    try {
      var tempPath = tempDir.toAbsolutePath().toString();
      var exception = assertThrows(ApplicationGeneratorException.class,
        () -> jsonProvider.readJsonFromFile(tempPath, ApplicationDescriptor.class, true));

      assertThat(exception.getMessage()).contains("ApplicationDescriptor is not found");
      assertThat(exception.getCategory()).isEqualTo(ErrorCategory.CONFIGURATION_ERROR);
    } finally {
      Files.deleteIfExists(tempDir);
    }
  }

  @Test
  @SneakyThrows
  void readJsonFromFile_negative_ioExceptionOnRead() {
    var tempDir = Files.createTempDirectory("json-provider-io-test");
    var testFile = new File(tempDir.toFile(), "test.json");
    testFile.createNewFile();

    try {
      var permissionChanged = testFile.setReadable(false);
      assumeTrue(permissionChanged && !testFile.canRead(),
        "Skipping test: cannot make file non-readable on this system");

      var exception = assertThrows(ApplicationGeneratorException.class,
        () -> jsonProvider.readJsonFromFile(testFile.getAbsolutePath(),
          ApplicationDescriptor.class, false));

      assertThat(exception.getMessage()).contains("Failed to read file:");
      assertThat(exception.getCategory()).isEqualTo(ErrorCategory.CONFIGURATION_ERROR);
    } finally {
      testFile.setReadable(true);
      Files.deleteIfExists(testFile.toPath());
      Files.deleteIfExists(tempDir);
    }
  }

  @Test
  @SneakyThrows
  void writeApplication_negative_cannotCreateDirectory() {
    var tempFile = Files.createTempFile("test-file", ".tmp");
    var invalidPath = tempFile.toAbsolutePath() + "/subdir";
    var application = new ApplicationDescriptor().id("test-app");

    try {
      var exception = assertThrows(ApplicationGeneratorException.class,
        () -> jsonProvider.writeApplication(application, invalidPath));

      assertThat(exception.getMessage()).contains("Could not create target directory");
      assertThat(exception.getCategory()).isEqualTo(ErrorCategory.CONFIGURATION_ERROR);
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  @Test
  @SneakyThrows
  void writeExecutionResult_positive() {
    var executionResult = ExecutionResult.success("generateFromJson", "app-platform", "1.0.0");

    jsonProvider.writeExecutionResult(executionResult, PATH);

    verify(jsonConverter).writeValue(any(File.class), eq(executionResult));
  }

  @Test
  @SneakyThrows
  void writeExecutionResult_positive_createsDirectory() {
    var tempDir = Files.createTempDirectory("json-provider-exec-test");
    var newDir = new File(tempDir.toFile(), "exec-subdir");
    var executionResult = ExecutionResult.started("generateFromJson", "app-platform");

    jsonProvider.writeExecutionResult(executionResult, newDir.getAbsolutePath());

    verify(jsonConverter).writeValue(any(File.class), eq(executionResult));
    assertThat(newDir).exists();

    newDir.delete();
    tempDir.toFile().delete();
  }

  @Test
  @SneakyThrows
  void writeExecutionResult_negative_cannotCreateDirectory() {
    var tempFile = Files.createTempFile("test-file", ".tmp");
    var invalidPath = tempFile.toAbsolutePath() + "/subdir";
    var executionResult = ExecutionResult.started("generateFromJson", "app-platform");

    try {
      var exception = assertThrows(ApplicationGeneratorException.class,
        () -> jsonProvider.writeExecutionResult(executionResult, invalidPath));

      assertThat(exception.getMessage()).contains("Could not create target directory");
      assertThat(exception.getCategory()).isEqualTo(ErrorCategory.CONFIGURATION_ERROR);
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  @Test
  @SneakyThrows
  void writeExecutionResult_negative_directoryNotWritable() {
    var tempDir = Files.createTempDirectory("json-provider-exec-readonly-test");
    var executionResult = ExecutionResult.started("generateFromJson", "app-platform");

    try {
      var permissionChanged = tempDir.toFile().setWritable(false);
      assumeTrue(permissionChanged && !tempDir.toFile().canWrite(),
        "Skipping test: cannot make directory non-writable on this system");

      var exception = assertThrows(ApplicationGeneratorException.class,
        () -> jsonProvider.writeExecutionResult(executionResult, tempDir.toAbsolutePath().toString()));

      assertThat(exception.getMessage()).contains("Target directory is not writable");
      assertThat(exception.getCategory()).isEqualTo(ErrorCategory.CONFIGURATION_ERROR);
    } finally {
      tempDir.toFile().setWritable(true);
      Files.deleteIfExists(tempDir);
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
