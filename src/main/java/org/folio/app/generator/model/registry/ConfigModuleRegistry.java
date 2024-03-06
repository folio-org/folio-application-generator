package org.folio.app.generator.model.registry;

import lombok.Data;

@Data
public class ConfigModuleRegistry {

  /**
   * Registry type - s3 or okapi.
   */
  private String type;

  /**
   * Public url template, where module id must have {@code {id}} placeholder.
   */
  private String publicUrlTemplate;

  /**
   * A base URL for http configuration.
   */
  private String url;

  /**
   * A path to a folder in bucket for S3 configuration.
   */
  private String path;

  /**
   * A name of the S3 bucket (used in S3 configuration).
   */
  private String bucket;
}
