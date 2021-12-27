[![Build](https://github.com/heutelbeck/sapl-policy-engine/actions/workflows/build.yml/badge.svg)](https://github.com/heutelbeck/sapl-policy-engine/actions/workflows/build.yml)

# The Streaming Attribute Policy Language (SAPL) and the Streaming Attribute Authorization Engine (SAAE)



## Maven Dependencies

### Add the Maven Central snapshot repository to your `pom.xml`, if you want to use SNAPSHOT versions of the engine:

```xml
  <repositories>
    <repository>
      <id>ossrh</id>
      <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
    </repository>
  </repositories>
```

### SAPL requires Java 11 or later

```xml
  <properties>
    <java.version>11</java.version>
    <maven.compiler.source>${java.version}</maven.compiler.source>
    <maven.compiler.target>${java.version}</maven.compiler.target>
  </properties>
```

### Add a SAPL dependency to your application. When using Maven you can add the following dependencies to your project's `pom.xml`:

```xml
  <dependency>
    <groupId>io.sapl</groupId>
    <artifactId>sapl-pdp-embedded</artifactId>
    <version>2.0.2-SNAPSHOT</version>
  </dependency>
```

### If you plan to use more SAPL dependencies, a useful bill of materials POM is offered, centralizing the dependency management for SAPL artifacts:

```xml
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>io.sapl</groupId>
        <artifactId>sapl-bom</artifactId>
        <version>2.0.2-SNAPSHOT</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
```
