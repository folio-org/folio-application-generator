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
  "platform": "base",
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

Updates an existing application descriptor with specified module versions. The descriptor needs to be specified via `appDescriptorPath` parameter or by default located in `${basedir}/application-descriptor.json`.

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

Both `updateFromJson` and `updateFromTemplate` goals support the following configuration options:

| Parameter             | Default | Description                                                                 |
|-----------------------|---------|-----------------------------------------------------------------------------|
| allowDowngrade        | false   | Allow downgrading module versions (by default, only upgrades are allowed)   |
| allowAddModules       | false   | Allow adding new modules not present in the original descriptor             |
| removeUnlistedModules | false   | Remove modules from descriptor that are not in the update list/template     |

Example with options:
```shell
mvn org.folio:folio-application-generator:updateFromTemplate \
  -DtemplatePath="${basedir}/template.json" \
  -DallowDowngrade=true \
  -DallowAddModules=true \
  -DremoveUnlistedModules=true
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

| Parameter                | Default Value | Description                                                                                                                                                         |
|--------------------------|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| awsRegion                | us-east-1     | AWS Region for S3 client                                                                                                                                            |
| buildNumber              |               | Build number from CI tool (will be added for any '-SNAPSHOT' version of generated application                                                                       |
| registries               |               | Comma-separated list of custom module-descriptor registries in formats: `s3::{{bucket-name}}:{{path-to-folder}}`, `okapi::{{okapi-base}}`, `simple::{{okapi-base}}` |
| beRegistries             |               | Comma-separated list of custom back-end module-descriptor registries in the same format as `registries` parameter                                                   |
| uiRegistries             |               | Comma-separated list of custom ui module-descriptor registries in the same format as `registries` parameter                                                         |
| moduleUrlsOnly           | false         | If `true` only URLs of modules will be included to the modules. Modules descriptors will be empty.                                                                  |
| appDescriptorPath        |               | File path of the application descriptor to update                                                                                                                   |
| modules                  |               | Comma-separated list of BE module ids to be updated in format: `module1-1.1.0,module2-2.1.0`                                                                        |
| uiModules                |               | Comma-separated list of UI module ids to be updated in the same format as `modules` parameter                                                                       |
| overrideConfigRegistries |               | Defines if only command-line specified registries must be used (applies to `registries`, `beRegistries` and `uiRegistries` params)                                  |
| allowDowngrade           | false         | Allow downgrading module versions during update (applies to `updateFromJson` and `updateFromTemplate` goals)                                                        |
| allowAddModules          | false         | Allow adding new modules not present in the original descriptor (applies to `updateFromJson` and `updateFromTemplate` goals)                                        |
| removeUnlistedModules    | false         | Remove modules from descriptor that are not in the update list/template (applies to `updateFromJson` and `updateFromTemplate` goals)                                |
| templatePath             |               | Path to the template file for `updateFromTemplate` goal (default: `${basedir}/template.json`)                                                                       |
