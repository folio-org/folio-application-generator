package org.folio.app.generator.model;

import lombok.Data;

@Data
public class ModuleDefinition {

  /**
   * An artifact id.
   */
  private String id;

  /**
   * An artifact name.
   */
  private String name;

  /**
   * An artifact version.
   */
  private String version;

  /**
   * URL to the artifact.
   */
  private String url;

  /**
   * Sets id field and returns {@link ModuleDefinition}.
   *
   * @return modified {@link ModuleDefinition} value
   */
  public ModuleDefinition id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Sets name field and returns {@link ModuleDefinition}.
   *
   * @return modified {@link ModuleDefinition} value
   */
  public ModuleDefinition name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Sets version field and returns {@link ModuleDefinition}.
   *
   * @return modified {@link ModuleDefinition} value
   */
  public ModuleDefinition version(String version) {
    this.version = version;
    return this;
  }

  /**
   * Sets url field and returns {@link ModuleDefinition}.
   *
   * @return modified {@link ModuleDefinition} value
   */
  public ModuleDefinition url(String url) {
    this.url = url;
    return this;
  }
}
