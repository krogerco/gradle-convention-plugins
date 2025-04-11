# Change Log

All notable changes to this project will be documented in this file.
`gradle-convention-plugins` adheres to [Semantic Versioning](https://semver.org/).

## 2.0.0
- [Feat]: allow jvmTarget, kotlin languageVersion, and kotlin apiVersion to be specified individually in version catalog.
- [Feat]: support Kotlin 2.x
- [Feat]: support new compose compiler plugin for Kotlin 2.x
- [Chore]: remove kgpAndroidxComposeCompiler
- [Chore]: update dokka to 2.0.0
- [Chore]: update Gradle to 8.13 with configuration cache enabled
- [Chore]: update AGP to 8.9.1 and make new minimum. Minimum supported Gradle version is now 8.11.1.
- [Chore]: update dagger to 2.56.2
- [Chore]: update android junit 5 plugin to 1.12.0.0. JUnit BOM to 5.12.0.
- [Chore]: update kotlinter to 5.0.1
- [Chore]: update kover to 0.8.3
- [Chore]: update ksp to 2.1.20-1.0.32
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
