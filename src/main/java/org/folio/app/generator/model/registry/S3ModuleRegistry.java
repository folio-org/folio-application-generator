package org.folio.app.generator.model.registry;

import static org.apache.commons.lang3.StringUtils.isBlank;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.folio.app.generator.model.types.RegistryType;

@Data
public class S3ModuleRegistry implements ModuleRegistry {

  @Setter(AccessLevel.NONE)
  private RegistryType type = RegistryType.AWS_S3;

  private String path;
  private String bucket;
  private String publicUrl;

  /**
   * Sets path field and returns {@link S3ModuleRegistry}.
   *
   * @return modified {@link S3ModuleRegistry} value
   */
  public S3ModuleRegistry path(String path) {
    this.path = path;
    return this;
  }

  /**
   * Sets bucket field and returns {@link S3ModuleRegistry}.
   *
   * @return modified {@link S3ModuleRegistry} value
   */
  public S3ModuleRegistry bucket(String bucket) {
    this.bucket = bucket;
    return this;
  }

  /**
   * Sets publicUrl field and returns {@link S3ModuleRegistry}.
   *
   * @return modified {@link S3ModuleRegistry} value
   */
  public S3ModuleRegistry publicUrl(String publicUrl) {
    this.publicUrl = publicUrl;
    return this;
  }

  @Override
  public boolean isValid() {
    return StringUtils.isNotBlank(bucket) && path != null;
  }

  @Override
  public S3ModuleRegistry withGeneratedFields() {
    if (isBlank(this.publicUrl)) {
      this.publicUrl = StringUtils.isEmpty(path)
        ? String.format("https://%s.s3.amazonaws.com/{id}", bucket)
        : String.format("https://%s.s3.amazonaws.com/%s{id}", bucket, path);
    }

    return this;
  }

  @Override
  public String getRegistryIdentifier() {
    return bucket + "/" + path;
  }
}
