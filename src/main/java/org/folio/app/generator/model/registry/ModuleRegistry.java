package org.folio.app.generator.model.registry;

import org.folio.app.generator.model.types.RegistryType;

public interface ModuleRegistry {

  /**
   * Retrieves registry type.
   */
  RegistryType getType();

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
}
