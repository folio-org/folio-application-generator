package org.folio.app.generator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.maven.plugin.MojoExecutionException;
import org.folio.app.generator.configuration.ApplicationContextBuilder;
import org.folio.app.generator.service.ModuleRegistryProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntegrityValidatorMojoTest {

  @Mock
  private ModuleRegistryProvider mockRegistryProvider;
  @Mock
  private ApplicationContextBuilder mockContextBuilder;

  private IntegrityValidatorMojo generator;

  @BeforeEach
  void setUp() {
    generator = new IntegrityValidatorMojo(mockRegistryProvider, mockContextBuilder);
  }

  @Test
  void execute_shouldRunWithoutErrors() {
    generator.baseUrl = "baseUrl";
    generator.token = "token";
    assertDoesNotThrow(() -> generator.execute());
  }

  @Test
  void execute_shouldRunWithErrorsWithoutToken() {
    generator.baseUrl = "baseUrl";
    assertThrows(MojoExecutionException.class, () -> generator.execute());
  }

  @Test
  void execute_shouldRunWithErrorsWithoutUrl() {
    generator.token = "token";
    assertThrows(MojoExecutionException.class, () -> generator.execute());
  }
}

