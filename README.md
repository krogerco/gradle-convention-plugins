# Gradle Convention Plugins

A collection of convention plugins to consistently configure Android applications and libraries.
The plugins require Gradle 8.4+ and Android Gradle Plugin 8.3+ for Android projects.

- [Installation](#installation)
  - [Version Catalog Requirements](#version-catalog-requirements)
- [Plugins](#plugins)
  - [Gradle Properties](#gradle-properties)
  - [KGP Root Plugin](#kgp-root-plugin)
  - [Android Application](#android-application)
  - [Published Android Library](#published-android-library)
  - [Published Kotlin Library](#published-kotlin-library)
  - [Release Conventions](#release-conventions)
  - [Dependency Management](#dependency-management)
  - [Kotlinter](#kotlinter)
- [Configuration](#configuration)
  - [Dagger](#dagger)
  - [Deep Links](#deep-links)
  - [Hilt](#hilt)
  - [Java API Desugaring](#java-api-desugaring)
  - [Jetpack Compose](#jetpack-compose)
  - [JUnit](#junit)
  - [Kotlinx Serialization](#kotlinx-serialization)
  - [Moshi](#moshi)
  - [Publishing](#publishing)
  - [Room](#room)

# Installation

All plugins are published to [Maven Central](https://central.sonatype.com/). To use the plugins add the following repository to `settings.gradle.kts` under `pluginManagement`:

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
    }
}
```

## Version Catalog Requirements
When using the convention plugins certain versions are expected to be in the version catalog of the project. The version catalog named `libs` is used by default but the catalog can be changed by setting the `kgp.versioncatalog.name` property in the `gradle.properties` file of the root project.

The following versions are required in the version catalog:
- **kgpJdk**: The JDK version to use when setting `jvmToolchain`.

The following versions are required in the version catalog when using Android convention plugins:
- **kgpCompileSdk**: The SDK version the application compiles against.
- **kgpMinSdk**: The minimum API level required to run the application.
- **kgpTargetSdk**: The API level the application targets.
- **kgpDokka**: The version used for the Android Documentation Plugin dependency.

# Plugins

## Gradle Properties
Some convention plugins auto-apply other plugins to better support common use cases. However, it is possible the plugins are not wanted on certain projects. When present in the `gradle.properties` file, the properties below can be used to prevent plugins from being auto-applied on supported projects:

```
kgp.plugins.autoapply.dependencymanagement=false
kgp.plugins.autoapply.dokka=false
kgp.plugins.autoapply.kotlinter=false
kgp.plugins.autoapply.kover=false
```

## KGP Root Plugin
A plugin that only gets applied to the root project in a multi-project build. It applies common configuration to the root project and subprojects including:
- Creating a clean task to delete the build directory of the root project.
- Setting the following default values for subprojects when enabled with `kgp.autoconfigure.publishingproperties`:
    - **group:** `com.kroger.{rootProject.name}`
    - **version:** set to the value of the environment variable `BUILD_VERSION` if it exists, otherwise `0.0.1`
- Configures the [Kotlinter](#kotlinter) plugin.
- Configures the [Dependency Management](#dependency-management) plugin. 

### Usage
Add the following plugin to the root project `build.gradle.kts` file:

```kotlin
plugins {
    alias(libs.plugins.conventions.rootPlugin)
}
```

## Android Application
- Applies common configuration to Android applications such as `defaultConfig`, `compileOptions`, and `packaging`
- Also applies and configures the following plugins:
    - Android Application
    - Kotlin Android
    - [Kover](https://github.com/Kotlin/kotlinx-kover)
    - See [Compose Configuration](#jetpack-compose) for more information on how Jetpack Compose is configured

### Usage

Add the following plugin to the `build.gradle.kts` file of the Android application:

```kotlin
plugins {
    alias(libs.plugins.conventions.androidApplication)
}
```

## Published Android Library

- Applies common configuration to Android libraries such as `defaultConfig`, `compileOptions`, `packaging`, and creating a sources jar.
- Also applies and configures the following plugins:
    - Release Conventions
    - Android Library
    - Kotlin Android
    - [Kover](https://github.com/Kotlin/kotlinx-kover)
    - [Dokka](https://github.com/Kotlin/dokka)
    - [android-junit5](https://github.com/mannodermaus/android-junit5)
    - See [Compose Configuration](#jetpack-compose) for more information on how Jetpack Compose is configured

### Usage

Add the following plugin to the `build.gradle.kts` file of the Android library:

```kotlin
plugins {
    alias(libs.plugins.conventions.publishedAndroidLibrary)
}
```

Alternatively if the library does not need to be published the following plugin can be used instead:

```kotlin
plugins {
    alias(libs.plugins.conventions.androidLibrary)
}
```

## Published Kotlin Library
- Applies common configuration to Kotlin libraries such as creating a sources jar and configuring junit5 testing.
- Also applies and configures the following plugins:
    - Release Conventions
    - Kotlin JVM
    - Java Library
    - [Kover](https://github.com/Kotlin/kotlinx-kover)
    - [Dokka](https://github.com/Kotlin/dokka)

### Usage

Add the following plugin to the `build.gradle.kts` file of the Kotlin library:

```kotlin
plugins {
    alias(libs.plugins.conventions.publishedKotlinLibrary)
}
```

Alternatively if the library does not need to be published the following plugin can be used instead:

```kotlin
plugins {
    alias(libs.plugins.conventions.KotlinLibrary)
}
```

## Release Conventions
This plugin is automatically applied when using either the Published Android Library Plugin or the Published Kotlin Library plugin. It uses the [Gradle Maven Publish Plugin](https://github.com/vanniktech/gradle-maven-publish-plugin) to publish the aar artifact for Android libraries or jar artifact for Kotlin libraries, including the sources jar when available, to the repository specified by the `kgp.repository.url` gradle property. The following project properties are used when publishing:
- **group id:** `project.group`
- **artifact id:** `project.name`
- **version:** `project.version`

These default values can be changed by using further configuration in the `build.gradle.kts` file of the project or the `gradle.properties` file of the project as shown in the [documentation](https://vanniktech.github.io/gradle-maven-publish-plugin/other/#github-packages-example).

The following additional properties can be added to the `gradle.properties` file:
- **kgp.repository.name:** the name of the repository as it will appear in generated Gradle tasks. Default is `Artifactory`.
- **kgp.repository.url:** the `URL` of the repository to publish to. Default is null.


### Standalone Usage

```kotlin
plugins {
    alias(libs.plugins.conventions.release)
}
```

## Dependency Management

This applies the dependency analysis android gradle plugin and the gradle versions plugin.

### Usage

The plugin is applied by default to the root project when using the [KGP Root Plugin](#kgp-root-plugin).

Auto-applying the plugin can be disabled by setting the following property to false in the `gradle.properties` file of the root project:

```
kgp.plugins.autoapply.dependencymanagement=false
```

To manually apply the plugin add the following to the root project `build.gradle.kts` file:

```kotlin
plugins {
    alias(libs.plugins.conventions.dependencyMaintenance)
}
```

### [Dependency Analysis Android Gradle Plugin](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin)
- Reports on unused dependencies that should be removed
- Used transitive dependencies (which you may want to declare directly)
- Dependencies declared on the wrong configuration (api vs implementation vs compileOnly, etc.)

### Tasks

To run the dependency analysis plugin execute the following gradle task from the project root directory:

```
gradlew buildHealth
```

A report will be created in the `{projectRoot}/build/reports/dependency-analysis` directory named `build-health-report` in txt and json format by default.

### [Gradle Versions Plugin](https://github.com/ben-manes/gradle-versions-plugin)

- Reports on dependencies that have an update available
- Includes updates to Gradle
- Updates that are not considered stable (e.g., beta, snapshot) will not be included in the report by default

### Tasks

To run the gradle versions plugin and check for dependencies with an update execute the following gradle task from the project root directory:

```
gradlew dependencyUpdates
```

A report will be created in the `{projectRoot}/build/dependencyUpdates` directory named `report` in txt and json format by default. The files show what dependencies are up to date, which have newer versions available, and which dependency versions could not be checked.

## Kotlinter
The [Kotlinter](https://github.com/jeremymailen/kotlinter-gradle) plugin is enabled by default for all projects that apply an Android plugin (application or library) or a kotlin plugin.

Auto-applying the plugin can be disabled by setting the following property to false in the `gradle.properties` file of the project:

```
kgp.plugins.autoapply.kotlinter=false
```

A pre-commit hook to lint kotlin files can be created by running the `createLintKotlinPreCommitHook` task from the root project. By default the task will create the hook in the `.husky` directory if it exists, otherwise the hook will be created in the `.git/hooks` directory. The hook executes the `checkKotlinFiles` task which will run `ktlint` against staged files for all configured projects. If a `ktlint` violation exists the commit will be cancelled.

If you want `ktlint` to try to auto-format staged files as a part of the commit then run the `createFormatKotlinPreCommitHook` instead.

The `checkKotlinFiles` task generates a report at `build/lint-report.json` that shows any issues and the `formatKotlinFiles` task generates a report at `build/format-report.txt` that shows any formatting fixes made.
By default the `Kotlinter` plugin version specifies what version of `ktlint` is used. See the [Kotlinter docs](https://github.com/jeremymailen/kotlinter-gradle?tab=readme-ov-file#custom-ktlint-version) for details on how to override the `ktlint` version if needed.

# Configuration

## Dagger
The following utility functions exist to help configure `Dagger`:
- **`daggerKsp()`:** adds the `dagger`, `javax:inject`, and `dagger-compiler` dependencies. Optionally adds `dagger-android-processor`, `dagger-android`, and `dagger-android-support` depending on the parameters used. Uses KSP for annotation processing.
- **`dagger()`:** (Deprecated) same as `daggerKsp()` but uses KAPT for annotation processing and sets `KaptExtension.correctErrorTypes = true`.

### Version Catalog Requirements
The following versions are expected in the Version Catalog when using the `Dagger` utility function:
- **`kgpDagger`:** The version to use for dagger dependencies.

## Deep Links
The following utility functions exist to help configure `Deep Links`:
- **`deepLink()`:** adds the `deeplink` and KSP `deeplink-processor` dependencies. Defaults the deep link doc file to `{buildDir}/doc/deeplinks.json`.

### Version Catalog Requirements
The following versions are expected in the Version Catalog when using the `Deep Links` utility functions:
- **`kgpDeepLink`:** The version to use for deep link dependencies.

## Hilt
By default when the [Android Application](#android-application) plugin is applied hilt will be auto-configured on the project using `hiltKapt()`.

The following utility functions exist to help configure `Hilt` when auto-configuration is not in use:
- **`hiltKsp()`:**
  - Adds the Hilt Android Compiler dependency to the `ksp`, `kspTest`, and `kspAndroidTest` configurations.
  - Adds the Hilt Android Testing dependency to the `testImplementation` and `AndroidTestImplementation` configurations.
  - Adds the Androidx Hilt Compiler dependency to the `ksp` configuration when the `androidxHiltCompilter` parameter is true. Default is `false`.
- **`hilt()` (Deprecated):**
  - Same as `hiltKsp()` except KAPT is used for annotation processing and sets `KaptExtension.correctErrorTypes = true`.

### Version Catalog Requirements
The following versions are expected in the Version Catalog when using Hilt auto-configuration:
- **`kgpDagger`:** The version to use for Hilt dependencies.
- **`kgpAndroidxHiltCompiler`:** The version of the hilt compiler to use for compatible androidx library processing.

### Properties
The following properties are used in the `gradle.properties` file of projects to control how auto-configuration is applied.
These properties are deprecated and will be removed in a future release:
- **`kgp.android.autoconfigure.hilt.application` (Deprecated):** if true then Hilt will be auto-configured when the [Android Application](#android-application) plugin is applied. Default is true.
- **`kgp.android.autoconfigure.hilt.library` (Deprecated):** if true then Hilt will be auto-configured when the [Android Library](#published-android-library) plugin is applied. Default is false.

## Java API Desugaring
Enabling desugaring allows the use of newer Java APIs in older versions of Java. Desugaring can be auto-configured when the [Android Application](#android-application) plugin or the [Published Android Library](#published-android-library) plugin is applied.

Auto-configuration does the following:
- **`android.compileOptions.isCoreLibraryDesugaringEnabled`:** set to true
- Dependencies:
  - Adds the desugar jdk libs dependency to the `coreLibraryDesugaring` configuration.

### Version Catalog Requirements
The following versions are expected in the Version Catalog when using desugaring auto-configuration:
- **`kgpAndroidDesugarJdkLibs`:** The version to use for the `com.android.tools:desugar_jdk_libs` dependency.

### Properties
The following property is used in the `gradle.properties` file of projects to control how auto-configuration is applied:
- **`kgp.android.autoconfigure.corelibrarydesugaring`:** if true then desugaring will be configured on supported projects. Default is false.

## Jetpack Compose
By default when an Android plugin is applied ([Android Application](#android-application) or [Published Android Library](#published-android-library)) then Jetpack Compose will be auto-configured.

Auto-configuration does the following:
- **`android.buildFeatures.compose`:** set to true
- **`android.composeOptions.kotlinCompilerExtensionVersion`:** set to `androidxComposeCompiler` version from Version Catalog.
- Dependencies
  - Adds the Jetpack Compose bill of materials and the default dependencies needed for Android Studio preview support and writing UI tests.
  - Optionally adds further dependencies depending on the value of the `kgp.android.autoconfigure.compose.dependencies` property.

### Version Catalog Requirements
The following versions and bundle are expected in the Version Catalog when using Jetpack Compose auto-configuration:
- **`kgpAndroidxComposeBom`:** [Jetpack Compose Bill of Materials](https://developer.android.com/jetpack/compose/bom) version to use for Jetpack Compose dependencies.
- **`kgpAndroidxComposeCompiler`:** Version of the Jetpack Compose Compiler to use. The version specified should be compatible with the version of the Kotlin Compiler Plugin used according to the [Compatibility Map](https://developer.android.com/jetpack/androidx/releases/compose-kotlin).
- **`kgpCompose`:** This Bundle is only required in the Version Catalog if the `kgp.android.autoconfigure.compose.dependencies` property is set to `bundle`. If it is then the `compose` bundle is added to the dependencies of the project.

### Properties
The following properties are used in the `gradle.properties` file of projects to further control how auto-configuration is applied:
- **`kgp.android.autoconfigure.compose`:** if true then Jetpack Compose will be auto-configured on supported projects. Default is true.
- **`kgp.android.autoconfigure.compose.dependencies`:** controls what additional compose dependencies are added by default to projects based on the values below.
  - **`bundle`:** adds dependencies from the `compose` bundle in the Version Catalog.
  - **`material`:** adds dependencies needed to use Material UI.
  - **`material3`:** adds dependencies needed to use Material3 UI. Also adds the core Material Icons.
  - **`none`:** does not add any additional dependencies. This is the default value.

## JUnit
A couple utility functions exist to help configure `JUnit` dependencies:
- **`junit5()`:** adds the `junit-bom` BOM and the `junit-jupiter` dependencies to the `testImplementation` configuration.
- **`junitVintage()`:** in addition to calling `junit5()` this will also add `junit4` to the `testImplementation` configuration and the `junit-vintage-engine` to the `testRuntimeOnly` configuration.

### Version Catalog Requirements
The following versions are expected in the Version Catalog when using the `junit` utility functions:
- **`kgpJunit4`:** The version of `junit4`. This is only required if `junitVintage()` is used.
- **`kgpJunit5`:** The version of `junit5`.

## Kotlinx Serialization
The following utility function exists to help configure `Kotlinx Serialization`:
- **`kotlinxSerialization()`:** adds the `org.jetbrains.kotlin.plugin.serialization` plugin. If `json` is true adds the `kotlinx-serialization-json` dependency.

### Version Catalog Requirements
The following versions are expected in the Version Catalog when using the `kotlinxSerialization()` utility functions:
- **`kotlinxSerialization`:** The version to use for Kotlinx Serialization dependencies.

## Moshi
The following utility function exists to help configure `Moshi`:
- **`moshi()`:** adds the `moshi` dependency. If `codegen` is true `moshi-kotlin-codegen` is added to the `ksp` configuration. If `moshiAdapters` is true `moshi-adapters` is included. If `moshiKoltlinReflect` is true then `moshi-kotlin` is included.

### Version Catalog Requirements
The following versions are expected in the Version Catalog when using the `moshi` utility functions:
- **`kgpMoshi`:** The version to use for Moshi dependencies.

## Publishing
Setting the default group and version for all subprojects can be disabled.

### Properties
The following property is used in the root `gradle.properties` file to control how auto-configuration is applied:
- **`kgp.autoconfigure.publishingproperties`:** if true then group and version will be set for all subprojects by the [KGP Root Plugin](#kgp-root-plugin). Default is true.

## Room
The following utility functions exist to help configure `Room`:
- **`room()`:** adds the `room-runtime`, `room-ktx`, `room-compiler` dependencies. `room-testing` and the room plugin are also added/applied if a `schemaDirectoryPath` is specified. The default `schemaDirectoryPath` is `{projectDir}/schemas`.

### Version Catalog Requirements
The following versions are expected in the Version Catalog when using the `room` utility functions:
- **`kgpAndroidxRoom`:** The version to use for Room dependencies.
