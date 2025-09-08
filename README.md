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
To build application descriptor without module descriptors use the following configuration
```xml
<configuration>
    <moduleUrlsOnly>true</moduleUrlsOnly>
    ...
  </configuration>
```
By setting `moduleUrlsOnly` to `true`, the generated application descriptor will include only the URLs of the modules, leaving the module descriptors empty.

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
      "version": "0.5.X"
    }
  ],
  "modules": [
    {
      "name": "mod-foo",
      "version": "latest"
    }
  ],
  "uiModules": [
    {
      "name": "folio_foo",
      "version": "latest"
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

To run the operation an app descriptor needs to be specified via `appDescriptorPath` parameter or by default locate in
`${basedir}/application-descriptor.json`
```shell
mvn org.folio:folio-application-generator:0.0.1-SNAPSHOT:updateFromJson
-Dmodules="mod-consortia-keycloak-1.4.4" -DuiModules="folio_consortia-settings:latest" -Dregistries="okapi::https://folio-registry.dev.folio.org"
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

| Parameter                | Default Value | Description
|--------------------------|---------------|-------------------------------------------------------------------------------------------------------------------------------------------
| awsRegion                | us-east-1     | AWS Region for S3 client
| buildNumber              |               | Build number from CI tool (will be added for any '-SNAPSHOT' version of generated application
| registries               |               | Comma-separated list of custom module-descriptor registries in formats: `s3::{{bucket-name}}:{{path-to-folder}}`, `okapi::{{okapi-base}}`, `simple::{{okapi-base}}`
| beRegistries             |               | Comma-separated list of custom back-end module-descriptor registries in the same format as `registries` parameter
| uiRegistries             |               | Comma-separated list of custom ui module-descriptor registries in the same format as `registries` parameter
| moduleUrlsOnly           | false         | If `true` only URLs of modules will be included to the modules. Modules descriptors will be empty.
| appDescriptorPath        |               | File path of the application descriptor to update
| modules                  |               | Comma-separated list of BE module ids to be updated in format: `module1-1.1.0,module2-2.1.0`
| uiModules                |               | Comma-separated list of UI module ids to be updated in the same format as `modules` parameter
| overrideConfigRegistries |               | Defines if only command-line specified registries must be used (applies to `registries`, `beRegistries` and `uiRegistries` params)
