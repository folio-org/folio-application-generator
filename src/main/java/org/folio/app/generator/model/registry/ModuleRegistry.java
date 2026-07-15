package org.folio.app.generator.model.registry;

import java.util.Map;
import org.folio.app.generator.model.types.RegistryType;

public interface ModuleRegistry {

  /**
   * Retrieves registry type.
   */
  RegistryType getType();

  /**
   * Retrieves custom HTTP headers to be sent with every request to this registry.
   *
   * @return map of header name to value, never {@code null} (empty when none configured)
   */
  Map<String, String> getHeaders();

  /**
   * Retrieves public URL template.
   *
   * <p>
   * Example: {@code http://localhost:3000/${id}}
   * </p>
   */
  String getPublicUrl();

  /**
   * Self-validation method.
   *
   * @return true - if registry is valid, false - otherwise
   */
  default boolean isValid() {
    return true;
  }

  /**
   * A general method that tells for the implementation to generate fields, if it's required.
   *
   * @return new or current object as {@link ModuleRegistry}
   */
  default ModuleRegistry withGeneratedFields() {
    return this;
  }

  /**
   * Returns a unique identifier for this registry instance.
   * Used for logging to distinguish between multiple registries of the same type.
   *
   * @return registry identifier (e.g., URL for Okapi/Simple, bucket/path for S3)
   */
  String getRegistryIdentifier();

  /**
   * Indicates whether this registry instance is used as a fallback source.
   *
   * <p>Fallback registries are consulted only when a module cannot be resolved from the primary registries.
   * The flag is also consulted by artifact validation: when {@code validateFallbackArtifacts} is {@code false},
   * candidates produced from a fallback registry bypass artifact existence checks.</p>
   *
   * @return {@code true} if this registry is used as fallback, {@code false} otherwise
   */
  boolean isFallback();

  /**
   * Marks this registry instance as a fallback source.
   *
   * @param fallback {@code true} to mark this registry as fallback
   */
  void setFallback(boolean fallback);
}
