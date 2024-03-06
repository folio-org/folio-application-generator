package org.folio.app.generator.service;

import static org.folio.app.generator.utils.PluginUtils.createModuleDefinitionFromId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.MojoExecutionException;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.ModulesLoadResult;
import org.folio.app.generator.model.registry.ModuleRegistries;
import org.folio.app.generator.service.loader.ModuleDescriptorLoaderFacade;
import org.folio.app.generator.utils.JsonConverter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModuleDescriptorService {

  private final JsonConverter jsonConverter;
  private final ModuleRegistries moduleRegistries;
  private final ModuleDescriptorLoaderFacade moduleDescriptorLoaderFacade;

  /**
   * Loads module descriptors as {@link ModulesLoadResult} for a list with module definitions.
   *
   * @param modules - a {@link List} with {@link ModuleDefinition} module definitions
   * @return {@link ModulesLoadResult} with list of module definitions and module descriptors
   * @throws MojoExecutionException if not all modules have been loaded
   */
  public ModulesLoadResult loadModules(List<ModuleDefinition> modules) throws MojoExecutionException {
    var foundDescriptors = new LinkedHashMap<String, Map<String, Object>>();

    for (var module : modules) {
      var moduleId = module.getId();
      for (var registry : moduleRegistries.registries()) {
        if (foundDescriptors.containsKey(moduleId)) {
          continue;
        }

        moduleDescriptorLoaderFacade.find(registry, module).ifPresent(md -> foundDescriptors.put(moduleId, md));
      }
    }

    var allModuleIds = modules.stream().map(ModuleDefinition::getId).toList();
    var notFoundModuleIds = new LinkedHashSet<>(allModuleIds);
    notFoundModuleIds.removeAll(foundDescriptors.keySet());

    if (!notFoundModuleIds.isEmpty()) {
      var modulesString = notFoundModuleIds.stream().collect(Collectors.joining("\n  * ", "\n  * ", ""));
      throw new MojoExecutionException("Failed to load module descriptors: " + modulesString);
    }

    var loadedModuleDescriptors = new ArrayList<>(foundDescriptors.values());
    return new ModulesLoadResult(convertToArtifacts(loadedModuleDescriptors), loadedModuleDescriptors);
  }

  private ArrayList<ModuleDefinition> convertToArtifacts(Collection<Map<String, Object>> values)
    throws MojoExecutionException {
    var moduleDefinitions = new ArrayList<ModuleDefinition>();
    for (var value : values) {
      moduleDefinitions.add(convertToArtifact(value));
    }

    return moduleDefinitions;
  }

  public ModuleDefinition convertToArtifact(Map<String, Object> moduleDescriptor) throws MojoExecutionException {
    var idField = moduleDescriptor.get("id");
    if (!(idField instanceof String moduleId)) {
      throw new MojoExecutionException("Loaded module id is invalid: " + jsonConverter.toJsonString(moduleDescriptor));
    }

    return createModuleDefinitionFromId(moduleId).orElseThrow(() -> new MojoExecutionException(
      "Module cannot be created for a module descriptor:" + jsonConverter.toJsonString(moduleDescriptor)));
  }
}
