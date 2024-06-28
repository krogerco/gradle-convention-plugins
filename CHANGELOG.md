# Change Log

All notable changes to this project will be documented in this file.
`gradle-convention-plugins` adheres to [Semantic Versioning](https://semver.org/).

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
