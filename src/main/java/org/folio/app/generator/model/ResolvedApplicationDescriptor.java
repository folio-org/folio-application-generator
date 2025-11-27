package org.folio.app.generator.model;

import static org.folio.app.generator.utils.PluginUtils.emptyIfNull;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResolvedApplicationDescriptor {

  private final String id;
  private final String name;
  private final String version;
  private final String description;
  private final String platform;
  private final List<Dependency> dependencies;
  private final List<ModuleDefinition> modules;
  private final List<ModuleDefinition> uiModules;
  private final List<Map<String, Object>> moduleDescriptors;
  private final List<Map<String, Object>> uiModuleDescriptors;

  /**
   * Creates ApplicationDescriptor in full format (with moduleDescriptors, without URLs in modules).
   *
   * @return {@link ApplicationDescriptor} in full format
   */
  public ApplicationDescriptor toFullDescriptor() {
    return new ApplicationDescriptor()
      .id(id)
      .name(name)
      .version(version)
      .description(description)
      .platform(platform)
      .dependencies(dependencies)
      .modules(copyModules(modules, false))
      .uiModules(copyModules(uiModules, false))
      .moduleDescriptors(moduleDescriptors)
      .uiModuleDescriptors(uiModuleDescriptors);
  }

  /**
   * Creates ApplicationDescriptor in URL-only format (without moduleDescriptors, with URLs in modules).
   *
   * @return {@link ApplicationDescriptor} in URL-only format
   */
  public ApplicationDescriptor toUrlOnlyDescriptor() {
    return new ApplicationDescriptor()
      .id(id)
      .name(name)
      .version(version)
      .description(description)
      .platform(platform)
      .dependencies(dependencies)
      .modules(copyModules(modules, true))
      .uiModules(copyModules(uiModules, true));
  }

  private List<ModuleDefinition> copyModules(List<ModuleDefinition> modules, boolean includeUrls) {
    return emptyIfNull(modules).stream()
      .map(md -> {
        var copy = new ModuleDefinition().id(md.getId()).name(md.getName()).version(md.getVersion());
        if (includeUrls) {
          copy.setUrl(md.getUrl());
        }
        return copy;
      })
      .toList();
  }
}
