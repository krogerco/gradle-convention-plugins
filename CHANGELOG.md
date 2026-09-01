# Change Log

All notable changes to this project will be documented in this file.
`gradle-convention-plugins` adheres to [Semantic Versioning](https://semver.org/).

## 2.0.0
- [Breaking]: remove support for KAPT 
- [Feat]: build against AGP 9's new DSL (consuming projects can still use the old DSL)
- [Feat]: support AGP 9 with and without built in Kotlin enabled
- [Feat]: allow jvmTarget, kotlin languageVersion, and kotlin apiVersion to be specified individually in version catalog
- [Feat]: support Kotlin 2.x
- [Feat]: apply and configure Kotlin Binary Compatibility Validator for library projects
- [Feat]: support new compose compiler plugin for Kotlin 2.x
- [Feat]: auto-apply Dependency Guard plugin to Android and Kotlin library projects to track classpath dependencies
- [Feat]: org.junit.platform:junit-platform-launcher added as testRuntimeOnly dependency for junit5()
- [Feat]: kgpJunit5 version renamed to kgpJunitBom
- [Feat]: added kgp.repository.credentials.env.username and kgp.repository.credentials.env.password properties to allow setting repository credentials from custom environment variables
- [Chore]: remove kgpAndroidxComposeCompiler
- [Chore]: add a flag to TestProjectDsl to enable debugging Gradle builds in the tests
- [Chore]: update dokka to 2.0.0
- [Chore]: update android-junit5 to 2.0.1 (also it is now called android-junit-framework)
- [Chore]: update Gradle to 9.6.1
- [Chore]: update AGP to 8.10.0 and make new minimum 9.0.1. Minimum supported Gradle version is now 9.2.0
- [Chore]: update dagger to 2.59.2
- [Chore]: update android junit 5 plugin to 1.12.0.0. JUnit BOM to 5.12.0
- [Chore]: update kotlinter to 5.4.2
- [Chore]: update kover to 0.9.9
- [Chore]: update ksp to 2.3.6
- [Chore]: update maven publish plugin to 0.52.0
- [Chore]: update room to 2.7.0

## 1.1.0

### Added

- KSP support for dagger/hilt using `daggerKsp()` and `hiltKsp()`
- Default value of projectDir/schemas added for room `schemaDir`

### Updated

- Compiled AGP version 8.5.0
- Gradle 8.8

### Deprecated
- `hilt()`, `dagger()`, and hilt autoconfiguration have been deprecated due to KAPT usage. The new functions that use KSP should be used instead.

## 1.0.0

### Added

- Initial open source release.
