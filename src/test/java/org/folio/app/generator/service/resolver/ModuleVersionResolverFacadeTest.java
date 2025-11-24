package org.folio.app.generator.service.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.PreReleaseFilter;
import org.folio.app.generator.model.registry.OkapiModuleRegistry;
import org.folio.app.generator.model.registry.S3ModuleRegistry;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.model.types.RegistryType;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ModuleVersionResolverFacadeTest {

  @Mock private Log log;
  @Mock private ModuleVersionResolver okapiResolver;
  @Mock private ModuleVersionResolver s3Resolver;

  private ModuleVersionResolverFacade facade;

  @BeforeEach
  void setUp() {
    when(okapiResolver.getType()).thenReturn(RegistryType.OKAPI);
    when(s3Resolver.getType()).thenReturn(RegistryType.AWS_S3);
    facade = new ModuleVersionResolverFacade(log, List.of(okapiResolver, s3Resolver));
  }

  @Test
  void getAvailableVersions_positive() {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var registry = okapiRegistry();
    var versions = List.of("1.2.0", "1.1.0", "1.0.0");

    when(okapiResolver.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.of(versions));

    var result = facade.getAvailableVersions(registry, dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.2.0", "1.1.0", "1.0.0");
  }

  @Test
  void getAvailableVersions_positive_s3Registry() {
    var dependency = new Dependency("mod-bar", "~2.0.0", PreReleaseFilter.TRUE);
    var registry = s3Registry();
    var versions = List.of("2.0.5", "2.0.1");

    when(s3Resolver.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.of(versions));

    var result = facade.getAvailableVersions(registry, dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("2.0.5", "2.0.1");
  }

  @Test
  void getAvailableVersions_positive_uiModule() {
    var dependency = new Dependency("folio_app", "^1.0.0", null);
    var registry = okapiRegistry();
    var versions = List.of("1.0.10010000000123");

    when(okapiResolver.getAvailableVersions(registry, dependency, ModuleType.UI))
        .thenReturn(Optional.of(versions));

    var result = facade.getAvailableVersions(registry, dependency, ModuleType.UI);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.0.10010000000123");
  }

  @Test
  void getAvailableVersions_positive_resolverReturnsEmpty() {
    var dependency = new Dependency("mod-unknown", "^1.0.0", PreReleaseFilter.FALSE);
    var registry = okapiRegistry();

    when(okapiResolver.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.empty());

    var result = facade.getAvailableVersions(registry, dependency, ModuleType.BE);

    assertThat(result).isEmpty();
  }

  @Test
  void getAvailableVersions_negative_noResolver() {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var registry = new UnknownModuleRegistry();

    var result = facade.getAvailableVersions(registry, dependency, ModuleType.BE);

    assertThat(result).isEmpty();
    verify(log).warn("Failed to find module version resolver for registry: UnknownModuleRegistry");
  }

  @Test
  void constructor_positive_emptyResolversList() {
    facade = new ModuleVersionResolverFacade(log, List.of());

    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var result = facade.getAvailableVersions(okapiRegistry(), dependency, ModuleType.BE);

    assertThat(result).isEmpty();
    verify(log).warn("Failed to find module version resolver for registry: OkapiModuleRegistry");
  }

  private static OkapiModuleRegistry okapiRegistry() {
    return new OkapiModuleRegistry().url("http://localhost").withGeneratedFields();
  }

  private static S3ModuleRegistry s3Registry() {
    return new S3ModuleRegistry().bucket("test-bucket").path("modules/").withGeneratedFields();
  }

  private static final class UnknownModuleRegistry extends OkapiModuleRegistry {

    @Override
    public RegistryType getType() {
      return null;
    }
  }
}
