package org.folio.app.generator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class ApplicationDescriptorTemplate {

  /**
   * An application id, generated from name and version.
   */
  private String id;

  /**
   * A name of application.
   */
  private String name;

  /**
   * A version of application.
   */
  private String version;

  /**
   * A version of application.
   */
  private String description;

  /**
   * A list with modules to be resolved.
   */
  @JsonProperty("modules")
  private List<Dependency> modules;

  /**
   * A list with UI modules to be resolved.
   */
  @JsonProperty("uiModules")
  private List<Dependency> uiModules;

  /**
   * A list with application dependencies (they are not resolved by plugin).
   */
  @JsonProperty("dependencies")
  private List<Dependency> dependencies;

  /**
   * Sets id field and returns {@link ApplicationDescriptorTemplate}.
   *
   * @return modified {@link ApplicationDescriptorTemplate} value
   */
  public ApplicationDescriptorTemplate id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Sets name field and returns {@link ApplicationDescriptorTemplate}.
   *
   * @return modified {@link ApplicationDescriptorTemplate} value
   */
  public ApplicationDescriptorTemplate name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Sets version field and returns {@link ApplicationDescriptorTemplate}.
   *
   * @return modified {@link ApplicationDescriptorTemplate} value
   */
  public ApplicationDescriptorTemplate version(String version) {
    this.version = version;
    return this;
  }

  /**
   * Sets modules field and returns {@link ApplicationDescriptorTemplate}.
   *
   * @return modified {@link ApplicationDescriptorTemplate} value
   */
  public ApplicationDescriptorTemplate modules(List<Dependency> modules) {
    this.modules = modules;
    return this;
  }

  /**
   * Sets uiModules field and returns {@link ApplicationDescriptorTemplate}.
   *
   * @return modified {@link ApplicationDescriptorTemplate} value
   */
  public ApplicationDescriptorTemplate uiModules(List<Dependency> uiModules) {
    this.uiModules = uiModules;
    return this;
  }

  /**
   * Sets dependencies field and returns {@link ApplicationDescriptorTemplate}.
   *
   * @return modified {@link ApplicationDescriptorTemplate} value
   */
  public ApplicationDescriptorTemplate dependencies(List<Dependency> dependencies) {
    this.dependencies = dependencies;
    return this;
  }
}
