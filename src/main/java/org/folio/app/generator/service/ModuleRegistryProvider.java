package org.folio.app.generator.service;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.folio.app.generator.utils.PluginUtils.PATH_DELIMITER;
import static org.folio.app.generator.utils.PluginUtils.collectToBulletedList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.app.generator.model.registry.ConfigModuleRegistry;
import org.folio.app.generator.model.registry.ModuleRegistries;
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
   * @param config -  initial plugin configuration as {@link PluginConfig} object
   * @return {@link List} with {@link ModuleRegistry} objects
   */
  public ModuleRegistries getModuleRegistries(PluginConfig config) {
    var processor = new ModuleRegistryProcessor(config);
    handleInvalidRegistries(processor.getInvalidRegistries());
    return new ModuleRegistries(processor.getBeRegistries(), processor.getUiRegistries());
  }

  private ModuleRegistryProcessingResult processCommandLineRegistries(String registryString) {
    if (isBlank(registryString)) {
      return new ModuleRegistryProcessingResult(emptyList(), emptyList());
    }

    var invalidRegistries = new ArrayList<String>();
    var commandLineRegistries = new ArrayList<ModuleRegistry>();
    var moduleRegistryStrings = registryString.split(",");

    for (var value : moduleRegistryStrings) {
      var moduleRegistry = moduleRegistryParser.parse(value);
      moduleRegistry.ifPresentOrElse(commandLineRegistries::add, () ->
        invalidRegistries.add(String.format("CommandLineRegistry(stringValue=%s)", value)));
    }

    return new ModuleRegistryProcessingResult(commandLineRegistries, invalidRegistries);
  }

  private ModuleRegistryProcessingResult processConfigurationRegistries(List<ConfigModuleRegistry> registries) {
    if (registries == null || registries.isEmpty()) {
      return new ModuleRegistryProcessingResult(emptyList(), emptyList());
    }

    var invalidRegistries = new ArrayList<String>();
    var result = new ArrayList<ModuleRegistry>();
    for (var configRegistry : registries) {
      if (!supportedRegistryTypes.contains(lowerCase(configRegistry.getType()))) {
        invalidRegistries.add(configRegistry.toString());
        continue;
      }

      var registry = toModuleRegistry(configRegistry);

      if (!registry.isValid()) {
        invalidRegistries.add(configRegistry.toString());
      } else {
        result.add(registry.withGeneratedFields());
      }
    }

    return new ModuleRegistryProcessingResult(result, invalidRegistries);
  }

  private static void handleInvalidRegistries(List<String> invalidRegistries) {
    if (!invalidRegistries.isEmpty()) {
      var invalidRegistryString = collectToBulletedList(invalidRegistries);
      throw new IllegalArgumentException("Invalid registries found, "
        + "check documentation at README.md and provided registry list:" + invalidRegistryString);
    }
  }

  private static ModuleRegistry toModuleRegistry(ConfigModuleRegistry registry) {
    if ("s3".equals(registry.getType())) {
      var path = defaultIfBlank(registry.getPath(), "");
      path = removeEnd(removeStart(path, PATH_DELIMITER), PATH_DELIMITER);
      return new S3ModuleRegistry()
        .path(StringUtils.isEmpty(path) ? path : path + PATH_DELIMITER)
        .bucket(trim(registry.getBucket()))
        .publicUrl(trim(registry.getPublicUrlTemplate()));
    }

    return new OkapiModuleRegistry()
      .url(removeEnd(trim(registry.getUrl()), PATH_DELIMITER))
      .publicUrl(trim(registry.getPublicUrlTemplate()));
  }

  @SafeVarargs
  private static <T> List<T> merge(Collection<T>... collections) {
    return Arrays.stream(collections)
      .flatMap(Collection::stream)
      .toList();
  }

  private final class ModuleRegistryProcessor {

    @Getter private final List<String> invalidRegistries;
    @Getter private final List<ModuleRegistry> beRegistries;
    @Getter private final List<ModuleRegistry> uiRegistries;

    private final PluginConfig pluginConfig;
    private final List<ModuleRegistry> registries;
    private final List<ModuleRegistry> cmdRegistries;

    ModuleRegistryProcessor(PluginConfig config) {
      this.pluginConfig = config;
      this.invalidRegistries = new ArrayList<>();

      var registriesProcessingResult = processConfigurationRegistries(config.getRegistries());
      var cmdRegistriesProcessingResult = processCommandLineRegistries(config.getCmdRegistryString());

      this.invalidRegistries.addAll(cmdRegistriesProcessingResult.invalidRegistries());
      this.invalidRegistries.addAll(registriesProcessingResult.invalidRegistries());

      this.registries = registriesProcessingResult.registries();
      this.cmdRegistries = cmdRegistriesProcessingResult.registries();

      this.beRegistries = processModuleRegistries(config.getBeCmdRegistryString(), config.getBeRegistries());
      this.uiRegistries = processModuleRegistries(config.getUiCmdRegistryString(), config.getUiRegistries());
    }

    private List<ModuleRegistry> processModuleRegistries(String registryStr,
      List<ConfigModuleRegistry> moduleRegistries) {
      var cmdRegistriesResultForType = processCommandLineRegistries(registryStr);
      invalidRegistries.addAll(cmdRegistriesResultForType.invalidRegistries());
      var cmdRegistriesForType = cmdRegistriesResultForType.registries();
      if (pluginConfig.isOverrideConfigRegistries()) {
        return merge(cmdRegistriesForType, cmdRegistries);
      }

      var registriesResultForType = processConfigurationRegistries(moduleRegistries);
      invalidRegistries.addAll(cmdRegistriesResultForType.invalidRegistries());
      return merge(cmdRegistriesForType, cmdRegistries, registriesResultForType.registries(), registries);
    }
  }

  private record ModuleRegistryProcessingResult(List<ModuleRegistry> registries, List<String> invalidRegistries) {}
}
