package org.folio.app.generator.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.SerializationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JsonConverter {

  private final ObjectMapper objectMapper;

  public <T> T parse(File file, Class<T> targetClass) {
    try {
      return objectMapper.readValue(file, targetClass);
    } catch (IOException e) {
      throw new SerializationException("Failed to read value from file: " + file.getAbsolutePath(), e);
    }
  }

  public <T> T parse(String json, Class<T> targetClass) {
    try {
      return objectMapper.readValue(json, targetClass);
    } catch (IOException e) {
      throw new SerializationException("Failed to read value from string", e);
    }
  }

  public <T> T parse(InputStream inputStream, TypeReference<T> typeReference) {
    try {
      return objectMapper.readValue(inputStream, typeReference);
    } catch (IOException e) {
      throw new SerializationException("Failed to parse value from input stream", e);
    }
  }

  public void writeValue(File file, Object value) {
    try {
      objectMapper.writeValue(file, value);
    } catch (IOException e) {
      throw new SerializationException("Failed to write value to file: " + file.getAbsolutePath(), e);
    }
  }

  public String toJsonString(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new SerializationException("Failed to convert value to json", e);
    }
  }
}
