package org.folio.app.generator.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.UpdateResult;
import org.folio.app.generator.utils.JsonConverter;
import org.springframework.stereotype.Component;

@Component
public class JsonProvider {

  private final Log log;
  private final JsonConverter jsonConverter;
  private final Map<String, String> substitutionMap;

  public JsonProvider(Log log, JsonConverter jsonConverter, MavenProject mavenProject) {
    this.log = log;
    this.jsonConverter = jsonConverter;
    this.substitutionMap = Map.of(
      "project.name", mavenProject.getName(),
      "project.version", mavenProject.getVersion(),
      "project.groupId", mavenProject.getGroupId(),
      "project.description", mavenProject.getDescription());
  }

  /**
   * Reads and substitutes variables depending on condition in source file.
   *
   * @param path - file path
   * @param clazz - object type to return
   * @param useSubstitution - defines whether apply substitution in the read content or not
   * @return application descriptor template as {@link T} object
   * @throws MojoExecutionException for any issues related to read and parse operations
   */
  public <T> T readJsonFromFile(String path, Class<T> clazz, boolean useSubstitution) throws MojoExecutionException {
    var file = new File(path);
    log.debug(clazz.getSimpleName() + " file location: " + file.getAbsolutePath());

    if (!file.exists() || file.isDirectory()) {
      throw new MojoExecutionException(clazz.getSimpleName() + " is not found: " + file.getAbsolutePath());
    }

    try {
      var content = Files.readString(file.toPath(), StandardCharsets.UTF_8)
        .replace("\r\n", "\n")
        .replace("\r", "\n");
      if (useSubstitution) {
        content = performSubstitution(content);
      }
      return jsonConverter.parse(content, clazz);
    } catch (IOException e) {
      throw new MojoExecutionException("Failed to read file: " + file.getAbsolutePath(), e);
    }
  }

  /**
   * Writes {@link ApplicationDescriptor} object to the specific directory.
   *
   * @param application - object to store
   * @param path - file path where to store object
   * @throws MojoExecutionException for any issues related to parse and write operations
   */
  public void writeApplication(ApplicationDescriptor application, String path)
    throws MojoExecutionException {
    var file = new File(path);
    if (!file.exists() && !file.mkdirs()) {
      throw new MojoExecutionException("Could not create target directory: " + file);
    }

    var applicationFile = new File(file, application.getId() + ".json");
    jsonConverter.writeValue(applicationFile, application);
  }

  public void writeUpdateResult(UpdateResult updateResult, String path) throws MojoExecutionException {
    var file = new File(path);
    if (!file.exists() && !file.mkdirs()) {
      throw new MojoExecutionException("Could not create target directory: " + file);
    }
    if (!file.canWrite()) {
      throw new MojoExecutionException("Target directory is not writable: " + file);
    }

    var updateResultFile = new File(file, "update-result.json");
    jsonConverter.writeValue(updateResultFile, updateResult);
    log.info("Update result saved to: " + updateResultFile.getAbsolutePath());
  }

  private String performSubstitution(String content) {
    var stringSubstitutor = new StringSubstitutor(substitutionMap);
    return stringSubstitutor.replace(content);
  }
}
