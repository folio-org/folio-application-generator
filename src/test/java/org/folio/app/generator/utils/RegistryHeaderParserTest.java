package org.folio.app.generator.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.folio.app.generator.model.types.RegistryType;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class RegistryHeaderParserTest {

  @Test
  void parseHeaders_singleHeader() {
    assertThat(RegistryHeaderParser.parseHeaders("X-Okapi-Token:secret"))
      .containsExactlyEntriesOf(Map.of("X-Okapi-Token", "secret"));
  }

  @Test
  void parseHeaders_multipleHeaders() {
    var result = RegistryHeaderParser.parseHeaders("X-Okapi-Token:secret;X-App:folio");
    assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of("X-Okapi-Token", "secret", "X-App", "folio"));
  }

  @Test
  void parseHeaders_valueWithColonsKeptAfterFirstSplit() {
    assertThat(RegistryHeaderParser.parseHeaders("Authorization:Bearer:multi:colon"))
      .containsExactlyEntriesOf(Map.of("Authorization", "Bearer:multi:colon"));
  }

  @Test
  void parseHeaders_trimsWhitespace() {
    assertThat(RegistryHeaderParser.parseHeaders("  X-Okapi-Token : secret  "))
      .containsExactlyEntriesOf(Map.of("X-Okapi-Token", "secret"));
  }

  @Test
  void parseHeaders_skipsMalformedEntries() {
    var result = RegistryHeaderParser.parseHeaders("NoColon;:blankName;X-Ok:v");
    assertThat(result).containsExactlyEntriesOf(Map.of("X-Ok", "v"));
  }

  @Test
  void parseHeaders_blankReturnsEmpty() {
    assertThat(RegistryHeaderParser.parseHeaders("   ")).isEmpty();
    assertThat(RegistryHeaderParser.parseHeaders(null)).isEmpty();
  }

  @Test
  void parseScoped_blankIsEmpty() {
    var scoped = RegistryHeaderParser.parseScoped("  ");
    assertThat(scoped.isEmpty()).isTrue();
    assertThat(scoped.forType(RegistryType.OKAPI)).isEmpty();
  }

  @Test
  void parseScoped_globalAppliesToEveryType() {
    var scoped = RegistryHeaderParser.parseScoped("X-Okapi-Token:secret;X-App:folio");

    assertThat(scoped.global()).containsExactlyInAnyOrderEntriesOf(
      Map.of("X-Okapi-Token", "secret", "X-App", "folio"));
    assertThat(scoped.forType(RegistryType.OKAPI))
      .containsExactlyInAnyOrderEntriesOf(Map.of("X-Okapi-Token", "secret", "X-App", "folio"));
    assertThat(scoped.forType(RegistryType.SIMPLE))
      .containsExactlyInAnyOrderEntriesOf(Map.of("X-Okapi-Token", "secret", "X-App", "folio"));
  }

  @Test
  void parseScoped_typePrefixAppliesOnlyToThatType() {
    var scoped = RegistryHeaderParser.parseScoped("okapi::X-Okapi-Token:secret");

    assertThat(scoped.global()).isEmpty();
    assertThat(scoped.forType(RegistryType.OKAPI)).containsExactlyEntriesOf(Map.of("X-Okapi-Token", "secret"));
    assertThat(scoped.forType(RegistryType.SIMPLE)).isEmpty();
  }

  @Test
  void parseScoped_mixedGlobalAndTypeScoped() {
    var scoped = RegistryHeaderParser.parseScoped("X-App:folio,okapi::X-Okapi-Token:secret");

    assertThat(scoped.forType(RegistryType.OKAPI))
      .containsExactlyInAnyOrderEntriesOf(Map.of("X-App", "folio", "X-Okapi-Token", "secret"));
    assertThat(scoped.forType(RegistryType.SIMPLE)).containsExactlyEntriesOf(Map.of("X-App", "folio"));
  }

  @Test
  void parseScoped_typeSpecificOverridesGlobalForSameName() {
    var scoped = RegistryHeaderParser.parseScoped("X-Token:global,okapi::X-Token:scoped");

    assertThat(scoped.forType(RegistryType.OKAPI)).containsExactlyEntriesOf(Map.of("X-Token", "scoped"));
    assertThat(scoped.forType(RegistryType.SIMPLE)).containsExactlyEntriesOf(Map.of("X-Token", "global"));
  }

  @Test
  void parseScoped_unknownPrefixTreatedAsGlobalHeaderWithColonValue() {
    var scoped = RegistryHeaderParser.parseScoped("Authorization:Bearer::x");

    assertThat(scoped.global()).containsExactlyEntriesOf(Map.of("Authorization", "Bearer::x"));
  }

  @Test
  void parseScoped_ignoresEmptyGroups() {
    var scoped = RegistryHeaderParser.parseScoped(",X-App:folio, ,okapi::X-Okapi-Token:secret,");

    assertThat(scoped.forType(RegistryType.OKAPI))
      .containsExactlyInAnyOrderEntriesOf(Map.of("X-App", "folio", "X-Okapi-Token", "secret"));
    assertThat(scoped.forType(RegistryType.SIMPLE)).containsExactlyEntriesOf(Map.of("X-App", "folio"));
  }
}
