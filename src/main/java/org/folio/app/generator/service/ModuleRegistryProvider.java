package org.folio.app.generator.service;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.folio.app.generator.utils.PluginUtils.PATH_DELIMITER;
import static org.folio.app.generator.utils.PluginUtils.collectToBulletedList;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.MojoExecutionException;
import org.folio.app.generator.model.registry.ConfigModuleRegistry;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.registry.OkapiModuleRegistry;
import org.folio.app.generator.model.registry.S3ModuleRegistry;
import org.folio.app.generator.service.parsers.StringModuleRegistryParser;
import org.folio.app.generator.utils.PluginConfig;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class ModuleRegistryProvider {

  private final StringModuleRegistryParser moduleRegistryParser;
  private final List<String> supportedRegistryTypes = List.of("s3", "okapi");

  /**
   * Provides list of validated registries to load module descriptors.
   *
   * @param pluginConfig -  initial plugin configuration as {@link PluginConfig} object
   * @return {@link List} with {@link ModuleRegistry} objects
   * @throws MojoExecutionException for any errors found during registry list validations
   */
  public List<ModuleRegistry> validateAndGetModuleRegistries(PluginConfig pluginConfig) throws MojoExecutionException {
    var cmdRegistryString = pluginConfig.getCmdRegistryString();
    var invalidRegistries = new ArrayList<String>();
    var commandLineRegistries = getCommandLineRegistries(cmdRegistryString, invalidRegistries);

    if (pluginConfig.isOverrideConfigRegistries() && !commandLineRegistries.isEmpty()) {
      handleInvalidRegistries(invalidRegistries);
      return commandLineRegistries;
    }

    var resultModuleRegistries = new ArrayList<>(commandLineRegistries);
    resultModuleRegistries.addAll(validateAndGetConfigRegistries(pluginConfig, invalidRegistries));
    handleInvalidRegistries(invalidRegistries);
    return resultModuleRegistries;
  }

  private List<ModuleRegistry> getCommandLineRegistries(String cmdRegistryString, ArrayList<String> invalidRegistries) {
    if (isBlank(cmdRegistryString)) {
      return emptyList();
    }

    var commandLineRegistries = new ArrayList<ModuleRegistry>();
    var moduleRegistryStrings = cmdRegistryString.split(",");

    for (var value : moduleRegistryStrings) {
      var moduleRegistry = moduleRegistryParser.parse(value);
      moduleRegistry.ifPresentOrElse(commandLineRegistries::add, () -> invalidRegistries.add(value));
    }

    return commandLineRegistries;
  }

  private List<ModuleRegistry> validateAndGetConfigRegistries(PluginConfig config, List<String> invalidRegistryList) {
    var result = new ArrayList<ModuleRegistry>();
    for (var configRegistry : config.getRegistries()) {
      if (!supportedRegistryTypes.contains(configRegistry.getType())) {
        invalidRegistryList.add(configRegistry.toString());
        continue;
      }

      var registry = toModuleRegistry(configRegistry);

      if (!registry.isValid()) {
        invalidRegistryList.add(configRegistry.toString());
      }

      result.add(registry.withGeneratedFields());
    }

    return result;
  }

  private static void handleInvalidRegistries(List<String> invalidRegistries) throws MojoExecutionException {
    if (!invalidRegistries.isEmpty()) {
      var invalidRegistryString = collectToBulletedList(invalidRegistries);
      throw new MojoExecutionException("Invalid registries found, "
        + "check documentation at README.md and provided registry list:" + invalidRegistryString);
    }
  }

  private static ModuleRegistry toModuleRegistry(ConfigModuleRegistry registry) {
    if ("s3".equals(registry.getType())) {
      var path = removeEnd(removeStart(trim(registry.getPath()), PATH_DELIMITER), PATH_DELIMITER);
      return new S3ModuleRegistry()
        .path(path.isEmpty() ? path : path + PATH_DELIMITER)
        .bucket(trim(registry.getBucket()))
        .publicUrl(trim(registry.getPublicUrlTemplate()));
    }

    return new OkapiModuleRegistry()
      .url(removeEnd(trim(registry.getUrl()), PATH_DELIMITER))
      .publicUrl(trim(registry.getPublicUrlTemplate()));
  }
}
