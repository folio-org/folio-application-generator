package org.folio.app.generator.model.registry;

import java.util.List;
import lombok.Data;
import lombok.ToString;

@Data
public class ConfigModuleRegistry {

  /**
   * Registry type - s3 or okapi.
   */
  private String type;

  /**
   * Public url template, where module id must have {@code {id}} placeholder.
   */
  @ToString.Exclude
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

  /**
   * Custom HTTP headers sent with every request to this registry.
   *
   * <p>Each {@code <header>} has a {@code <name>} and {@code <value>}; both support Maven property
   * and {@code ${env.*}} interpolation, so a secret header name and value can be injected at build
   * time.</p>
   */
  @ToString.Exclude
  private List<ConfigHeader> headers;
}
