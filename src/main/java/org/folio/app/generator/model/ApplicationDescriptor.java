package org.folio.app.generator.model;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ApplicationDescriptor {

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
   * A description of application.
   */
  private String description;

  /**
   * A list with modules to be resolved.
   */
  private List<ModuleDefinition> modules;

  /**
   * A list with UI modules to be resolved.
   */
  private List<ModuleDefinition> uiModules;

  /**
   * A list with application dependencies (they are not resolved by plugin).
   */
  private List<Dependency> dependencies;

  /**
   * List with module descriptor.
   */
  private List<Map<String, Object>> moduleDescriptors;

  /**
   * List with UI module descriptors.
   */
  private List<Map<String, Object>> uiModuleDescriptors;

  /**
   * Sets id field and returns {@link ApplicationDescriptor}.
   *
   * @return modified {@link ApplicationDescriptor} value
   */
  public ApplicationDescriptor id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Sets name field and returns {@link ApplicationDescriptor}.
   *
   * @return modified {@link ApplicationDescriptor} value
   */
  public ApplicationDescriptor name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Sets version field and returns {@link ApplicationDescriptor}.
   *
   * @return modified {@link ApplicationDescriptor} value
   */
  public ApplicationDescriptor version(String version) {
    this.version = version;
    return this;
  }

  /**
   * Sets description field and returns {@link ApplicationDescriptor}.
   *
   * @return modified {@link ApplicationDescriptor} value
   */
  public ApplicationDescriptor description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Sets modules field and returns {@link ApplicationDescriptor}.
   *
   * @return modified {@link ApplicationDescriptor} value
   */
  public ApplicationDescriptor modules(List<ModuleDefinition> modules) {
    this.modules = modules;
    return this;
  }

  /**
   * Sets uiModules field and returns {@link ApplicationDescriptor}.
   *
   * @return modified {@link ApplicationDescriptor} value
   */
  public ApplicationDescriptor uiModules(List<ModuleDefinition> uiModules) {
    this.uiModules = uiModules;
    return this;
  }

  /**
   * Sets dependencies field and returns {@link ApplicationDescriptor}.
   *
   * @return modified {@link ApplicationDescriptor} value
   */
  public ApplicationDescriptor dependencies(List<Dependency> dependencies) {
    this.dependencies = dependencies;
    return this;
  }

  /**
   * Sets moduleDescriptors field and returns {@link ApplicationDescriptor}.
   *
   * @return modified {@link ApplicationDescriptor} value
   */
  public ApplicationDescriptor moduleDescriptors(List<Map<String, Object>> moduleDescriptors) {
    this.moduleDescriptors = moduleDescriptors;
    return this;
  }

  /**
   * Sets uiModuleDescriptors field and returns {@link ApplicationDescriptor}.
   *
   * @return modified {@link ApplicationDescriptor} value
   */
  public ApplicationDescriptor uiModuleDescriptors(List<Map<String, Object>> uiModuleDescriptors) {
    this.uiModuleDescriptors = uiModuleDescriptors;
    return this;
  }
}
