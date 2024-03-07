package org.folio.app.generator.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import org.folio.app.generator.conditions.AwsCondition;
import org.folio.app.generator.conditions.OkapiCondition;
import org.folio.app.generator.utils.PluginConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ComponentScan("org.folio.app.generator")
public class SpringConfiguration {

  @Bean(name = "objectMapper")
  public ObjectMapper objectMapper() {
    return new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
      .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, false);
  }

  @Bean(name = "httpClient")
  @Conditional(OkapiCondition.class)
  public HttpClient httpClient() {
    return HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(30))
      .build();
  }

  @Bean(name = "amazonS3Client")
  @Conditional(AwsCondition.class)
  public S3Client amazonS3Client(PluginConfig config) {
    return S3Client.builder()
      .region(config.getAwsRegion())
      .credentialsProvider(DefaultCredentialsProvider.create())
      .build();
  }
}
