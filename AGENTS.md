# Agent Guidelines for folio-application-generator

This document provides essential information for AI coding agents working in this repository.

## Project Overview

**folio-application-generator** is a Maven plugin for generating FOLIO application descriptors from JSON templates or POM configurations. It's written in Java 17, uses Spring for dependency injection, and follows FOLIO checkstyle conventions.

**Stack:** Java 17, Maven, Spring Context, Lombok, Jackson, JUnit 5, Mockito, AssertJ

## Build & Test Commands

### Build
```bash
# Full build (includes clean, compile, test, checkstyle)
mvn clean install

# Build without running tests
mvn clean install -DskipTests

# Compile only
mvn compile
```

### Testing
```bash
# Run all tests
mvn test

# Run only unit tests (tagged with @UnitTest)
mvn test -Dgroups=unit

# Run a single test class
mvn test -Dtest=ApplicationContextBuilderTest

# Run a single test method
mvn test -Dtest=ApplicationContextBuilderTest#shouldBuildContext

# Run with coverage (JaCoCo)
mvn clean verify -Pcoverage
```

### Linting/Checkstyle
```bash
# Run checkstyle validation
mvn checkstyle:check

# Process classes (includes checkstyle during build)
mvn process-classes
```

### Plugin Goals
```bash
# Generate application descriptor from JSON template
mvn folio-application-generator:generateFromJson

# Generate from POM configuration
mvn folio-application-generator:generateFromConfiguration

# Update existing application descriptor
mvn folio-application-generator:updateFromJson

# Update from template
mvn folio-application-generator:updateFromTemplate
```

## Code Style Guidelines

### General Formatting
- **Indentation:** 2 spaces (NOT tabs)
- **Max line length:** 120 characters
- **Charset:** UTF-8
- **Line endings:** LF (Unix style)
- **Final newline:** Required
- **Trailing whitespace:** Remove all
- **Method length:** Max 80 lines (checkstyle enforced)

### Import Ordering
```java
// 1. Static imports
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// 2. Java standard library
import java.util.List;
import java.util.Objects;

// 3. Third-party libraries (alphabetically)
import org.apache.maven.plugin.logging.Log;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

// 4. Project packages
import org.folio.app.generator.model.registry.ModuleRegistries;
import org.folio.app.generator.utils.PluginConfig;
```

### Naming Conventions
- **Classes:** PascalCase (e.g., `ApplicationContextBuilder`)
- **Methods/Variables:** camelCase (e.g., `moduleRegistries`, `buildContext()`)
- **Constants:** UPPER_SNAKE_CASE (e.g., `DEFAULT_TIMEOUT`)
- **Packages:** lowercase (e.g., `org.folio.app.generator`)
- **Test methods:** descriptive names with underscores allowed (e.g., `shouldBuildContext_whenValidConfig`)

### Types and Annotations
- Use **Lombok** annotations for boilerplate reduction:
  - `@Data`, `@Builder`, `@Value` for data classes
  - `@Slf4j` for logging
  - `@SneakyThrows` sparingly for checked exceptions
  - Lombok config: `addLombokGeneratedAnnotation = true`
- Use **var** for local variable type inference when type is obvious
- Use **@Inject** for constructor injection in Mojos
- Use **@Parameter** for Maven plugin parameters

### Test Conventions
- All tests must extend/use appropriate test markers:
  - `@UnitTest` annotation for unit tests (tagged "unit")
  - `@IntegrationTest` annotation for integration tests
- Use **JUnit 5** (`@Test`, `@BeforeEach`, `@AfterEach`)
- Use **Mockito** for mocking (`@Mock`, `@InjectMocks`, `@ExtendWith(MockitoExtension.class)`)
- Use **AssertJ** for assertions (`assertThat()` style)
- Test class naming: `{ClassName}Test.java`
- Test method naming: `should{ExpectedBehavior}_when{Condition}` or descriptive names

### Error Handling
- Use **MojoExecutionException** for Maven plugin errors
- Use **IllegalArgumentException** for invalid parameters
- Use **IllegalStateException** for invalid state
- Log errors using Maven's `Log` interface: `log.error()`, `log.warn()`
- Don't swallow exceptions - log or rethrow appropriately
- Use `@SneakyThrows` only in test code or when checked exception handling adds no value

### Javadoc
- Required for public classes and public methods
- Use standard Javadoc format:
  ```java
  /**
   * Brief description of what the method does.
   *
   * @param paramName description of parameter
   * @return description of return value
   * @throws ExceptionType when this exception is thrown
   */
  ```
- Builder method pattern example:
  ```java
  /**
   * Sets log field and returns {@link ApplicationContextBuilder}.
   *
   * @return modified {@link ApplicationContextBuilder} value
   */
  public ApplicationContextBuilder withLog(Log log) {
    this.log = log;
    return this;
  }
  ```

### Spring & Dependency Injection
- Use **constructor injection** (preferred over field injection)
- Register beans programmatically in `ApplicationContextBuilder`
- Use `@Bean` methods in `SpringConfiguration` for complex bean creation
- Component classes should be services, not controllers or repositories

### Maven Plugin Development
- Extend `AbstractMojo` or project-specific abstract classes
- Use `@Parameter` annotation for configuration
- Required parameters: `required = true`
- Use `readonly = true` for injected Maven objects
- Use `defaultValue = "${propertyName}"` for command-line overrides

## File Organization

```
folio-application-generator/
├── src/
│   ├── main/java/org/folio/app/generator/
│   │   ├── configuration/          # Spring configuration
│   │   ├── model/                  # Data models, DTOs
│   │   ├── service/                # Business logic
│   │   ├── utils/                  # Utility classes
│   │   ├── validator/              # Validation logic
│   │   └── *Mojo.java              # Maven plugin goals
│   └── test/java/org/folio/app/generator/
│       ├── support/                # Test annotations (@UnitTest, @IntegrationTest)
│       └── *Test.java              # Test classes mirror main structure
├── checkstyle/                     # Checkstyle configuration
│   ├── checkstyle-checker.properties
│   └── checkstyle-suppressions.xml
├── pom.xml                         # Maven configuration
└── lombok.config                   # Lombok settings
```

## Common Patterns

### Builder Pattern for Context
```java
var context = new ApplicationContextBuilder()
  .withLog(log)
  .withPluginConfig(config)
  .withMavenProject(mavenProject)
  .withMavenSession(mavenSession)
  .withModuleRegistries(moduleRegistries)
  .build();
```

### Spring Bean Registration
```java
context.registerBean("beanName", BeanClass.class, () -> beanInstance);
context.registerBean(ConfigurationClass.class);
context.refresh();
```

## Important Notes

- This is a **Maven plugin project** - not a Spring Boot application
- Tests use **tags** for categorization (`@UnitTest` for "unit" group)
- Checkstyle is **strictly enforced** during build
- Code coverage target: **80%** instruction coverage
- AWS SDK integration for S3 registries (default region: us-east-1)
- Supports Okapi, S3, and simple HTTP module registries
