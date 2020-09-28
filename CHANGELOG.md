# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Added
- English localization
- User can change app language to override device language
- Open source notices in app settings

### Changed
- Show specific error messages during onboarding if user is not the device owner
- Use soft hyphens instead of zero-width spaces in longer words
- Retry encrypted shared preferences operations in case of decryption failures 
- UI text updates

### Fixed
- Show an error message if device does not have a web browser app

## [1.0.2](https://github.com/THLfi/koronavilkku-android/compare/v1.0.1...v1.0.2) - 2020-09-28

### Added
- Request to disable battery optimization for the app

### Fixed
- Android 11: Do not check for device location setting when not required by exposure notifications

## [1.0.1] - 2020-09-14

### Fixed
- Localized texts
- Minor layout issue with diagnosis code error

### Changed
- Error handling for exposure notifications

## [1.0.0] - 2020-08-31

### Added
- Initial release of the application


[1.0.1]: https://github.com/THLfi/koronavilkku-android/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/THLfi/koronavilkku-android/releases/tag/v1.0.0
