package org.folio.app.generator.service;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;
import org.folio.app.generator.model.registry.ConfigModuleRegistry;
import org.folio.app.generator.model.registry.ModuleRegistries;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.registry.OkapiModuleRegistry;
import org.folio.app.generator.model.registry.S3ModuleRegistry;
import org.folio.app.generator.model.registry.SimpleModuleRegistry;
import org.folio.app.generator.service.parsers.StringModuleRegistryParser;
import org.folio.app.generator.support.UnitTest;
import org.folio.app.generator.utils.PluginConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class ModuleRegistryProviderTest {

  private ModuleRegistryProvider moduleRegistryProvider;

  @BeforeEach
  void setUp() {
    this.moduleRegistryProvider = new ModuleRegistryProvider(new StringModuleRegistryParser());
  }

  @MethodSource("pluginConfigProvider")
  @ParameterizedTest(name = "[{index}] {0}")
  void getModuleRegistries_positive_parameterized(@SuppressWarnings("unused") String name,
    PluginConfig config, ModuleRegistries expected) {
    var result = moduleRegistryProvider.getModuleRegistries(config);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getModuleRegistries_negative_unsupportedType() {
    var config = PluginConfig.builder().registries(List.of(unknownModuleRegistry())).build();

    assertThatThrownBy(() -> moduleRegistryProvider.getModuleRegistries(config))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("""
        Invalid registries found, check documentation at README.md and provided registry list:
          * ConfigModuleRegistry(type=unknown, url=https://localhost:8000/registry, path=null, bucket=null)""");
  }

  @Test
  void getModuleRegistries_negative_invalidCommandLineRegistry() {
    var config = PluginConfig.builder().cmdRegistryString("unknown::test").build();

    assertThatThrownBy(() -> moduleRegistryProvider.getModuleRegistries(config))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("""
        Invalid registries found, check documentation at README.md and provided registry list:
          * CommandLineRegistry(stringValue=unknown::test)""");
  }

  @Test
  void getModuleRegistries_negative_invalidUrl() {
    var config = PluginConfig.builder().registries(List.of(okapiConfigRegistry("unknown-url"))).build();

    assertThatThrownBy(() -> moduleRegistryProvider.getModuleRegistries(config))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("""
        Invalid registries found, check documentation at README.md and provided registry list:
          * ConfigModuleRegistry(type=okapi, url=unknown-url, path=null, bucket=null)""");
  }

  @Test
  void getModuleRegistries_negative_nullBucketPath() {
    var registry = new ConfigModuleRegistry();
    registry.setType("s3");
    var config = PluginConfig.builder().registries(List.of(registry)).build();

    assertThatThrownBy(() -> moduleRegistryProvider.getModuleRegistries(config))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("""
        Invalid registries found, check documentation at README.md and provided registry list:
          * ConfigModuleRegistry(type=s3, url=null, path=null, bucket=null)""");
  }

  private static Stream<Arguments> pluginConfigProvider() {
    return Stream.of(
      arguments(
        "Config registry provided (override = false)",
        config(false, null, okapiConfigRegistry()),
        registries(List.of(okapiRegistry()), List.of(okapiRegistry()))),

      arguments(
        "Config registry (s3 with null path) provided (override = false)",
        config(false, null, s3ConfigRegistry(null)),
        registries(List.of(s3Registry("")), List.of(s3Registry("")))),

      arguments(
        "Config registry provided (override = true)",
        config(true, null, okapiConfigRegistry()),
        registries(emptyList(), emptyList())),

      arguments(
        "Command-line(s3) and config(okapi) registries provided (override = false)",
        config(false, "s3::test::/", okapiConfigRegistry()),
        registries(List.of(s3Registry(""), okapiRegistry()), List.of(s3Registry(""), okapiRegistry()))),

      arguments(
        "Command-line(s3) and config(okapi) registries provided (override = true)",
        config(true, "s3::test::test/", okapiConfigRegistry()),
        registries(List.of(s3Registry()), List.of(s3Registry()))),

      arguments(
        "Command-line(okapi) and config(s3) registries provided (override = false)",
        config(false, "okapi::https://localhost:9130", s3ConfigRegistry()),
        registries(List.of(okapiRegistry(), s3Registry()), List.of(okapiRegistry(), s3Registry()))),

      arguments(
        "Command-line(okapi) and config(s3) registries provided (override = true)",
        config(true, "okapi::https://localhost:9130", s3ConfigRegistry()),
        registries(List.of(okapiRegistry()), List.of(okapiRegistry()))),

      arguments("Config (be, ui) registries provided (override = false)",
        PluginConfig.builder()
          .overrideConfigRegistries(false)
          .beRegistries(List.of(s3ConfigRegistry("be/")))
          .uiRegistries(List.of(s3ConfigRegistry("ui/")))
          .build(),
        registries(List.of(s3Registry("be/")), List.of(s3Registry("ui/")))),

      arguments("Config (be, ui, global) registries provided (override = false)",
        PluginConfig.builder()
          .overrideConfigRegistries(false)
          .registries(List.of(okapiConfigRegistry()))
          .beRegistries(List.of(s3ConfigRegistry("be/")))
          .uiRegistries(List.of(s3ConfigRegistry("ui/")))
          .build(),
        registries(List.of(s3Registry("be/"), okapiRegistry()), List.of(s3Registry("ui/"), okapiRegistry()))),

      arguments("Command-line(be, ui) and config (be, ui) registries provided (override = false)",
        PluginConfig.builder()
          .overrideConfigRegistries(false)
          .beRegistries(List.of(s3ConfigRegistry("be/")))
          .uiRegistries(List.of(s3ConfigRegistry("ui/")))
          .beCmdRegistryString("s3::test::be/f/")
          .uiCmdRegistryString("s3::test::ui/f/")
          .build(),
        registries(List.of(s3Registry("be/f/"), s3Registry("be/")), List.of(s3Registry("ui/f/"), s3Registry("ui/")))),

      arguments("Command-line(be) and config (ui) registries provided (override = false)",
        PluginConfig.builder()
          .overrideConfigRegistries(false)
          .beCmdRegistryString("s3::test::be/f/")
          .uiRegistries(List.of(s3ConfigRegistry("ui/")))
          .build(),
        registries(List.of(s3Registry("be/f/")), List.of(s3Registry("ui/")))),

      arguments("Command-line(be, ui) and config (be, ui) registries provided (override = true)",
        PluginConfig.builder()
          .registries(emptyList())
          .overrideConfigRegistries(true)
          .beRegistries(List.of(s3ConfigRegistry("be/")))
          .uiRegistries(List.of(s3ConfigRegistry("ui/")))
          .beCmdRegistryString("s3::test::be/f/")
          .uiCmdRegistryString("s3::test::ui/f/")
          .build(),
        registries(List.of(s3Registry("be/f/")), List.of(s3Registry("ui/f/")))),

      arguments("Command-line(be, ui, global) and config (be, ui, global) registries provided (override = false)",
        PluginConfig.builder()
          .overrideConfigRegistries(false)
          .registries(List.of(okapiConfigRegistry()))
          .beRegistries(List.of(s3ConfigRegistry("be/")))
          .uiRegistries(List.of(s3ConfigRegistry("ui/")))
          .cmdRegistryString("s3::test::/global/")
          .beCmdRegistryString("s3::test::be/f/")
          .uiCmdRegistryString("s3::test::ui/f/")
          .build(),
        registries(
          List.of(s3Registry("be/f/"), s3Registry("global/"), s3Registry("be/"), okapiRegistry()),
          List.of(s3Registry("ui/f/"), s3Registry("global/"), s3Registry("ui/"), okapiRegistry()))),

      arguments("Config (be, ui, global) registries provided (override = false)",
        PluginConfig.builder()
          .overrideConfigRegistries(false)
          .registries(List.of(okapiConfigRegistry()))
          .beRegistries(List.of(s3ConfigRegistry("be/")))
          .uiRegistries(List.of(s3ConfigRegistry("ui/")))
          .build(),
        registries(List.of(s3Registry("be/"), okapiRegistry()), List.of(s3Registry("ui/"), okapiRegistry()))),

      arguments("Config (be, ui, global) registries provided (override = true)",
        PluginConfig.builder()
          .overrideConfigRegistries(true)
          .registries(List.of(okapiConfigRegistry()))
          .beRegistries(List.of(s3ConfigRegistry("be/")))
          .uiRegistries(List.of(s3ConfigRegistry("ui/")))
          .build(),
        registries(emptyList(), emptyList())),

      arguments(
        "Config registry provided simple (override = false)",
        config(false, null, simpleConfigRegistry()),
        registries(List.of(simpleRegistry()), List.of(simpleRegistry()))),

      arguments(
        "Config registry provided for simple (override = true)",
        config(true, null, simpleConfigRegistry()),
        registries(emptyList(), emptyList())),

      arguments(
        "Command-line(s3) and config(simple) registries provided (override = false)",
        config(false, "s3::test::/", simpleConfigRegistry()),
        registries(List.of(s3Registry(""), simpleRegistry()), List.of(s3Registry(""), simpleRegistry()))),

      arguments(
        "Command-line(s3) and config(simple) registries provided (override = true)",
        config(true, "s3::test::test/", simpleConfigRegistry()),
        registries(List.of(s3Registry()), List.of(s3Registry()))),

      arguments(
        "Command-line(simple) and config(s3) registries provided (override = false)",
        config(false, "simple::https://localhost:9130", s3ConfigRegistry()),
        registries(List.of(simpleRegistry(), s3Registry()), List.of(simpleRegistry(), s3Registry()))),

      arguments(
        "Command-line(simple) and config(s3) registries provided (override = true)",
        config(true, "simple::https://localhost:9130", s3ConfigRegistry()),
        registries(List.of(simpleRegistry()), List.of(simpleRegistry()))),

      arguments("Config (be, ui, global) registries provided with simple (override = false)",
        PluginConfig.builder()
          .overrideConfigRegistries(false)
          .registries(List.of(simpleConfigRegistry()))
          .beRegistries(List.of(s3ConfigRegistry("be/")))
          .uiRegistries(List.of(s3ConfigRegistry("ui/")))
          .build(),
        registries(List.of(s3Registry("be/"), simpleRegistry()), List.of(s3Registry("ui/"), simpleRegistry()))),

      arguments(
        "Command-line(be, ui, global) and config (be, ui, global) registries provided with simple (override = false)",
        PluginConfig.builder()
          .overrideConfigRegistries(false)
          .registries(List.of(simpleConfigRegistry()))
          .beRegistries(List.of(s3ConfigRegistry("be/")))
          .uiRegistries(List.of(s3ConfigRegistry("ui/")))
          .cmdRegistryString("s3::test::/global/")
          .beCmdRegistryString("s3::test::be/f/")
          .uiCmdRegistryString("s3::test::ui/f/")
          .build(),
        registries(
          List.of(s3Registry("be/f/"), s3Registry("global/"), s3Registry("be/"), simpleRegistry()),
          List.of(s3Registry("ui/f/"), s3Registry("global/"), s3Registry("ui/"), simpleRegistry()))),

      arguments("Config (be, ui, global) registries provided with simple (override = false)",
        PluginConfig.builder()
          .overrideConfigRegistries(false)
          .registries(List.of(simpleConfigRegistry()))
          .beRegistries(List.of(s3ConfigRegistry("be/")))
          .uiRegistries(List.of(s3ConfigRegistry("ui/")))
          .build(),
        registries(List.of(s3Registry("be/"), simpleRegistry()), List.of(s3Registry("ui/"), simpleRegistry()))),

      arguments("Config (be, ui, global) registries provided with simple (override = true)",
        PluginConfig.builder()
          .overrideConfigRegistries(true)
          .registries(List.of(simpleConfigRegistry()))
          .beRegistries(List.of(s3ConfigRegistry("be/")))
          .uiRegistries(List.of(s3ConfigRegistry("ui/")))
          .build(),
        registries(emptyList(), emptyList()))
    );
  }

  private static ModuleRegistries registries(List<ModuleRegistry> beRegistries, List<ModuleRegistry> uiRegistries) {
    return new ModuleRegistries(beRegistries, uiRegistries, List.of(), List.of());
  }

  private static PluginConfig config(boolean overrideRegistries, String cmdString, ConfigModuleRegistry... registries) {
    return PluginConfig.builder()
      .overrideConfigRegistries(overrideRegistries)
      .registries(List.of(registries))
      .cmdRegistryString(cmdString)
      .build();
  }

  private static ConfigModuleRegistry unknownModuleRegistry() {
    var configModuleRegistry = new ConfigModuleRegistry();
    configModuleRegistry.setType("unknown");
    configModuleRegistry.setUrl("https://localhost:8000/registry");
    return configModuleRegistry;
  }

  private static ModuleRegistry okapiRegistry() {
    return new OkapiModuleRegistry()
      .url("https://localhost:9130")
      .withGeneratedFields();
  }

  private static ModuleRegistry s3Registry() {
    return s3Registry("test/");
  }

  private static ModuleRegistry s3Registry(String path) {
    return new S3ModuleRegistry()
      .bucket("test")
      .path(path)
      .withGeneratedFields();
  }

  private static ModuleRegistry simpleRegistry() {
    return new SimpleModuleRegistry()
      .url("https://localhost:9130")
      .withGeneratedFields();
  }

  private static ConfigModuleRegistry okapiConfigRegistry() {
    return okapiConfigRegistry("https://localhost:9130");
  }

  private static ConfigModuleRegistry okapiConfigRegistry(String url) {
    var configModuleRegistry = new ConfigModuleRegistry();
    configModuleRegistry.setType("okapi");
    configModuleRegistry.setUrl(url);
    return configModuleRegistry;
  }

  private static ConfigModuleRegistry s3ConfigRegistry() {
    return s3ConfigRegistry("test/");
  }

  private static ConfigModuleRegistry s3ConfigRegistry(String path) {
    var configModuleRegistry = new ConfigModuleRegistry();
    configModuleRegistry.setType("s3");
    configModuleRegistry.setBucket("test");
    configModuleRegistry.setPath(path);
    return configModuleRegistry;
  }

  private static ConfigModuleRegistry simpleConfigRegistry() {
    return simpleConfigRegistry("https://localhost:9130");
  }

  private static ConfigModuleRegistry simpleConfigRegistry(String url) {
    var configModuleRegistry = new ConfigModuleRegistry();
    configModuleRegistry.setType("simple");
    configModuleRegistry.setUrl(url);
    return configModuleRegistry;
  }
}
