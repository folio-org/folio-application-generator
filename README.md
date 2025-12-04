# folio-application-generator

Copyright (C) 2022-2024 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

### JSON-based configuration

#### Configuration example

```xml
<plugin>
  <groupId>org.folio</groupId>
  <artifactId>folio-application-generator</artifactId>
  <version>${folio-application-generator.version}</version>
  <executions>
    <execution>
      <goals>
        <goal>generateFromJson</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <templatePath>${basedir}/template.json</templatePath>
    <moduleRegistries>
      <registry>
        <type>okapi</type>
        <url>https://folio-registry.dev.folio.org</url>
      </registry>
    </moduleRegistries>
  </configuration>
</plugin>
```
#### moduleUrlsOnly mode

To build application descriptor without full module descriptors use the following configuration:
```xml
<configuration>
    <moduleUrlsOnly>true</moduleUrlsOnly>
    ...
  </configuration>
```

Or via command-line: `-DmoduleUrlsOnly=true`

**Behavior:**
- **Generate goals** (`generateFromJson`, `generateFromConfiguration`): Modules will include URLs pointing to their descriptors instead of embedding full descriptor content. The `moduleDescriptors` and `uiModuleDescriptors` arrays will be empty.
- **Update goals** (`updateFromJson`, `updateFromTemplate`): Changed modules will include URLs, while unchanged modules retain their existing descriptors from the original application descriptor. This optimizes updates by not re-fetching descriptors for modules that haven't changed.

#### Extending configuration with dedicated registries for BE and UI modules:

```xml
<plugin>
  <groupId>org.folio</groupId>
  <artifactId>folio-application-generator</artifactId>
  <version>${folio-application-generator.version}</version>
  <executions>
    <execution>
      <goals>
        <goal>generateFromJson</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <templatePath>${basedir}/template.json</templatePath>
    <moduleRegistries>
      <registry>
        <type>okapi</type>
        <url>https://folio-registry.sample.org</url>
      </registry>
    </moduleRegistries>
    <beModuleRegistries>
      <registry>
        <type>s3</type>
        <bucket>folio-module-registry</bucket>
        <path>be-modules/</path>
      </registry>
    </beModuleRegistries>
    <uiModuleRegistries>
      <registry>
        <type>okapi</type>
        <url>https://ui-folio-registry.sample.org</url>
      </registry>
    </uiModuleRegistries>
  </configuration>
</plugin>

```

#### JSON Template example

```json
{
  "name": "${project.name}",
  "version": "${project.version}",
  "description": "${project.description}",
  "dependencies": [
    {
      "name": "app-foo",
      "version": "0.5.X",
      "preRelease": "true"
    }
  ],
  "modules": [
    {
      "name": "mod-foo",
      "version": "latest",
      "preRelease": "only"
    },
    {
      "name": "mod-foo1",
      "version": "3.4.2"
    }
  ],
  "uiModules": [
    {
      "name": "folio_foo",
      "version": "^1.5.0",
      "preRelease": "false"
    }
  ]
}
```

#### Excluding JAR file from build results

JAR file can be excluded from `/target` folder by using the following command

```shell
mvn folio-application-generator:generateFromJson
```

### pom.xml-based configuration

#### Configuration example

```xml
<plugin>
  <groupId>org.folio</groupId>
  <artifactId>folio-application-generator</artifactId>
  <version>${folio-application-generator.version}</version>
  <executions>
    <execution>
      <goals>
        <goal>generateFromConfiguration</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <modules>
      <module>
        <name>mod-foo</name>
        <version>latest</version>
      </module>
    </modules>
    <uiModules>
      <module>
        <name>folio_foo</name>
        <version>latest</version>
      </module>
    </uiModules>
    <dependencies>
      <dependency>
        <name>app-foo</name>
        <version>0.5.X</version>
      </dependency>
    </dependencies>
    <moduleRegistries>
      <registry>
        <type>okapi</type>
        <url>https://folio-registry.dev.folio.org</url>
      </registry>
    </moduleRegistries>
  </configuration>
</plugin>
```

#### Excluding JAR file from build results

JAR file can be excluded from `/target` folder by using the following command

```shell
mvn folio-application-generator:generateFromConfiguration
```

### Update application descriptor

#### updateFromJson

Updates an existing application descriptor with specified module versions. The descriptor path can be specified via `appDescriptorPath` parameter.

```shell
mvn org.folio:folio-application-generator:updateFromJson \
  -DappDescriptorPath="${basedir}/application-descriptor.json" \
  -Dmodules="mod-consortia-keycloak-1.4.4" \
  -DuiModules="folio_consortia-settings:latest" \
  -Dregistries="okapi::https://folio-registry.dev.folio.org"
```

#### updateFromTemplate

Synchronizes an existing application descriptor based on a template file. This goal compares the template with the existing descriptor and applies updates according to the configured options.

```shell
mvn org.folio:folio-application-generator:updateFromTemplate \
  -DtemplatePath="${basedir}/template.json" \
  -DappDescriptorPath="${basedir}/application-descriptor.json" \
  -Dregistries="okapi::https://folio-registry.dev.folio.org"
```

#### Update configuration options

##### updateFromJson defaults

| Parameter             | Default | Description                                                                 |
|-----------------------|---------|-----------------------------------------------------------------------------|
| allowDowngrade        | false   | Allow downgrading module versions (by default, only upgrades are allowed)   |
| allowAddModules       | false   | Allow adding new modules not present in the original descriptor             |
| removeUnlistedModules | false   | Remove modules from descriptor that are not in the update list/template     |

##### updateFromTemplate defaults

| Parameter             | Default | Description                                                                 |
|-----------------------|---------|-----------------------------------------------------------------------------|
| allowDowngrade        | true    | Allow downgrading module versions                                           |
| allowAddModules       | true    | Allow adding new modules not present in the original descriptor             |
| removeUnlistedModules | true    | Remove modules from descriptor that are not in the template                 |

Example overriding defaults:
```shell
mvn org.folio:folio-application-generator:updateFromTemplate \
  -DtemplatePath="${basedir}/template.json" \
  -DallowDowngrade=false \
  -DallowAddModules=false \
  -DremoveUnlistedModules=false
```

### Validate Application's Modules Interface Integrity

The `validateIntegrity` goal is used to validate the integrity of application's modules.
This validation is performed in mgr-applications via specified URL and requires authentication via a token.

#### Parameters
- `baseUrl` (required): The base URL of the mgr-applications.
- `token` (required): The authentication token used to access the mgr-applications.

#### Restrictions
Both `baseUrl` and `token` parameters are mandatory. If either is missing, the plugin will throw a `MojoExecutionException`.

#### Usage
To run the `validateIntegrity` goal, use the following command:

```shell
mvn org.folio:folio-application-generator:0.0.1-SNAPSHOT:validateIntegrity \
-DbaseUrl="https://mgr-applications:8080" \
-Dtoken="your-authentication-token"
```

This command will generate application on a fly and validates the application's modules in mgr-applications.

### Validate Module Artifacts Existence

The `validateArtifacts` parameter enables validation of module artifacts in Docker and NPM registries before generating the application descriptor. When enabled, the plugin verifies that:
- Backend (BE) modules have corresponding Docker images in the configured Docker registries
- Frontend (UI) modules have corresponding NPM packages in the configured NPM registries

#### Default Registries

When no custom registries are specified, the following defaults are used:

**BE (Docker) registries:**
- `folioorg` (release artifacts)
- `folioci` (pre-release/snapshot artifacts)

**UI (NPM) registries:**
- `npm-folio` (release artifacts)
- `npm-folioci` (pre-release/snapshot artifacts)

#### Registry Resolution Order

The plugin selects registries based on module type (BE/UI) and version (release/pre-release):

**For BE module (release version):**
1. `beArtifactRegistries` (if provided)
2. `artifactRegistries` (unified fallback)

**For BE module (pre-release version):**
1. `bePreReleaseArtifactRegistries` (if provided)
2. `beArtifactRegistries` (if provided)
3. `artifactRegistries` (unified fallback)

**For UI module (release version):**
1. `uiArtifactRegistries` (if provided)
2. `artifactRegistries` (unified fallback)

**For UI module (pre-release version):**
1. `uiPreReleaseArtifactRegistries` (if provided)
2. `uiArtifactRegistries` (if provided)
3. `artifactRegistries` (unified fallback)

#### Usage

Enable artifact validation via command-line:
```shell
mvn org.folio:folio-application-generator:generateFromJson -DvalidateArtifacts=true
```

Or via pom.xml configuration:
```xml
<configuration>
  <validateArtifacts>true</validateArtifacts>
  ...
</configuration>
```

#### Custom Registries

Custom artifact registries can be specified via command-line:
```shell
mvn org.folio:folio-application-generator:generateFromJson \
  -DvalidateArtifacts=true \
  -DbeArtifactRegistries="folioorg" \
  -DbePreReleaseArtifactRegistries="folioci" \
  -DuiArtifactRegistries="npm-folio" \
  -DuiPreReleaseArtifactRegistries="npm-folioci"
```

For custom registry URLs:
```shell
mvn org.folio:folio-application-generator:generateFromJson \
  -DvalidateArtifacts=true \
  -DbeArtifactRegistries="https://private-registry.io/v2::my-namespace"
```

Or via pom.xml configuration:
```xml
<configuration>
  <validateArtifacts>true</validateArtifacts>

  <!-- BE release registries -->
  <beArtifactRegistries>
    <registry>
      <type>docker-hub</type>
      <namespace>folioorg</namespace>
    </registry>
  </beArtifactRegistries>

  <!-- BE pre-release registries -->
  <bePreReleaseArtifactRegistries>
    <registry>
      <type>docker-hub</type>
      <namespace>folioci</namespace>
    </registry>
  </bePreReleaseArtifactRegistries>

  <!-- UI release registries -->
  <uiArtifactRegistries>
    <registry>
      <type>folio-npm</type>
      <namespace>npm-folio</namespace>
    </registry>
  </uiArtifactRegistries>

  <!-- UI pre-release registries -->
  <uiPreReleaseArtifactRegistries>
    <registry>
      <type>folio-npm</type>
      <namespace>npm-folioci</namespace>
    </registry>
  </uiPreReleaseArtifactRegistries>
</configuration>
```

#### Unified Artifact Registries

For simpler configurations, use `artifactRegistries` as a fallback for both BE and UI:
```xml
<configuration>
  <validateArtifacts>true</validateArtifacts>
  <artifactRegistries>
    <registry>
      <type>docker-hub</type>
      <namespace>my-docker-namespace</namespace>
    </registry>
    <registry>
      <type>folio-npm</type>
      <namespace>my-npm-repo</namespace>
    </registry>
  </artifactRegistries>
</configuration>
```

#### Retry Mechanism

The artifact validation includes a retry mechanism for temporary service unavailability. HTTP status codes `429`, `502`, `503`, and `504` will trigger automatic retries (up to 5 attempts).

### Module-Registries order

#### Backend module registries

1. parsed registries from `beRegistries` command-line parameter (if present)
2. parsed registries from `registries` command-line parameter (if present)
3. Processed registries from `beModuleRegistries` plugin configuration (can be empty)
4. Processed registries from `moduleRegistries` plugin configuration (can be empty)

#### UI module registries

1. parsed registries from `uiRegistries` command-line parameter (if present)
2. parsed registries from `registries` command-line parameter (if present)
3. Processed registries from `uiModuleRegistries` plugin configuration (can be empty)
4. Processed registries from `moduleRegistries` plugin configuration (can be empty)

### Command-line parameters

These parameters can be specified in the job run using following notation

```shell
mvn install -DbuildNumber="123" -DawsRegion=us-east-1
```

| Parameter                      | Default Value                                   | Description                                                                                                                                                         |
|--------------------------------|-------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| awsRegion                      | us-east-1                                       | AWS Region for S3 client                                                                                                                                            |
| buildNumber                    |                                                 | Build number from CI tool (will be added for any '-SNAPSHOT' version of generated application                                                                       |
| registries                     |                                                 | Comma-separated list of custom module-descriptor registries in formats: `s3::{{bucket-name}}:{{path-to-folder}}`, `okapi::{{okapi-base}}`, `simple::{{okapi-base}}` |
| beRegistries                   |                                                 | Comma-separated list of custom back-end module-descriptor registries in the same format as `registries` parameter                                                   |
| uiRegistries                   |                                                 | Comma-separated list of custom ui module-descriptor registries in the same format as `registries` parameter                                                         |
| moduleUrlsOnly                 | false                                           | If `true` only URLs of modules will be included to the modules. Modules descriptors will be empty.                                                                  |
| appDescriptorPath              | `${project.artifactId}-${project.version}.json` | File path of the application descriptor to update                                                                                                                   |
| modules                        |                                                 | Comma-separated list of BE module ids to be updated in format: `module1-1.1.0,module2-2.1.0`                                                                        |
| uiModules                      |                                                 | Comma-separated list of UI module ids to be updated in the same format as `modules` parameter                                                                       |
| overrideConfigRegistries       |                                                 | Defines if only command-line specified registries must be used (applies to `registries`, `beRegistries` and `uiRegistries` params)                                  |
| allowDowngrade                 | false                                           | Allow downgrading module versions during update (applies to `updateFromJson` and `updateFromTemplate` goals)                                                        |
| allowAddModules                | false                                           | Allow adding new modules not present in the original descriptor (applies to `updateFromJson` and `updateFromTemplate` goals)                                        |
| removeUnlistedModules          | false                                           | Remove modules from descriptor that are not in the update list/template (applies to `updateFromJson` and `updateFromTemplate` goals)                                |
| templatePath                   | `${project.artifactId}.template.json`           | Path to the template file for `updateFromTemplate` goal                                                                                                             |
| validateArtifacts              | false                                           | If `true`, validates that Docker images (BE) and NPM packages (UI) exist before generating the descriptor                                                           |
| artifactRegistries             |                                                 | Comma-separated unified artifact registries (fallback for both BE and UI)                                                                                           |
| beArtifactRegistries           |                                                 | Comma-separated BE artifact registries for release versions (format: `namespace` or `url::namespace`)                                                               |
| uiArtifactRegistries           |                                                 | Comma-separated UI artifact registries for release versions (format: `repository` or `url::repository`)                                                             |
| bePreReleaseArtifactRegistries |                                                 | Comma-separated BE artifact registries for pre-release versions                                                                                                     |
| uiPreReleaseArtifactRegistries |                                                 | Comma-separated UI artifact registries for pre-release versions                                                                                                     |
