package org.folio.app.generator;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.maven.plugin.MojoExecutionException;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class AbstractGeneratorMojoTest {

  @Test
  void toMojoExecutionException_positive_returnsSameInstanceForMojoExecutionException() {
    var original = new MojoExecutionException("Test mojo error");

    var result = AbstractGeneratorMojo.toMojoExecutionException(original);

    assertThat(result).isSameAs(original);
  }

  @Test
  void toMojoExecutionException_positive_wrapsRuntimeException() {
    var original = new RuntimeException("Test runtime error");

    var result = AbstractGeneratorMojo.toMojoExecutionException(original);

    assertThat(result).isNotSameAs(original);
    assertThat(result).isInstanceOf(MojoExecutionException.class);
    assertThat(result.getMessage()).isEqualTo("Test runtime error");
    assertThat(result.getCause()).isSameAs(original);
  }

  @Test
  void toMojoExecutionException_positive_wrapsCheckedException() {
    var original = new Exception("Test checked exception");

    var result = AbstractGeneratorMojo.toMojoExecutionException(original);

    assertThat(result).isNotSameAs(original);
    assertThat(result).isInstanceOf(MojoExecutionException.class);
    assertThat(result.getMessage()).isEqualTo("Test checked exception");
    assertThat(result.getCause()).isSameAs(original);
  }
}
