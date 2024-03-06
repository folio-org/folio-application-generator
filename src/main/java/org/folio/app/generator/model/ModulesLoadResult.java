package org.folio.app.generator.model;

import java.util.List;
import java.util.Map;

public record ModulesLoadResult(List<ModuleDefinition> artifacts, List<Map<String, Object>> descriptors) {}
