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
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.utils.JsonConverter;

public class JsonTemplateProvider {

  private final Log log;
  private final JsonConverter jsonConverter;
  private final Map<String, String> substitutionMap;

  public JsonTemplateProvider(Log log, JsonConverter jsonConverter, MavenProject mavenProject) {
    this.log = log;
    this.jsonConverter = jsonConverter;
    this.substitutionMap = Map.of(
      "project.name", mavenProject.getName(),
      "project.version", mavenProject.getVersion(),
      "project.groupId", mavenProject.getGroupId(),
      "project.description", mavenProject.getDescription());
  }

  /**
   * Reads and substitutes variables in source template file.
   *
   * @param path - template file path
   * @return application descriptor template as {@link ApplicationDescriptorTemplate} object
   * @throws MojoExecutionException for any issues related to read and parse operations
   */
  public ApplicationDescriptorTemplate readTemplate(String path) throws MojoExecutionException {
    var templateFile = new File(path);
    log.debug("Template file location: " + templateFile.getAbsolutePath());
    if (!templateFile.exists() && templateFile.isDirectory()) {
      throw new MojoExecutionException("Template is not found: " + templateFile.getAbsolutePath());
    }

    try {
      return readJson(templateFile);
    } catch (IOException e) {
      throw new MojoExecutionException("Failed to read file: " + templateFile.getAbsolutePath(), e);
    }
  }

  private ApplicationDescriptorTemplate readJson(File f) throws IOException {
    var templateString = Files.readString(f.toPath(), StandardCharsets.UTF_8);
    var stringSubstitutor = new StringSubstitutor(substitutionMap);
    var json = stringSubstitutor.replace(templateString);
    return jsonConverter.parse(json, ApplicationDescriptorTemplate.class);
  }
}
