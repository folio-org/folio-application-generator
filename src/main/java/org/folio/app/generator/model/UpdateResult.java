package org.folio.app.generator.model;

import java.util.List;

public record UpdateResult(
  List<String> beAdded,
  List<String> beUpgraded,
  List<String> beDowngraded,
  List<String> beRemoved,
  List<String> uiAdded,
  List<String> uiUpgraded,
  List<String> uiDowngraded,
  List<String> uiRemoved
) {}
