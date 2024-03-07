package org.folio.app.generator.model;

import java.util.Map;

public record ModuleLoadResult(ModuleDefinition artifact, Map<String, Object> descriptor) {}
