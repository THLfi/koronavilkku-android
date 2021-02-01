# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

## [2.0.2](https://github.com/THLfi/koronavilkku-android/compare/v2.0.1...v2.0.2) - 2021-02-01

### Changed
- UI text and background functionality updates related to quarantine time changing from 10 to 14 days in national guidelines for COVID-19

## [2.0.1](https://github.com/THLfi/koronavilkku-android/compare/v2.0.0...v2.0.1) - 2021-01-13

### Changed
- Http client timeouts increased to allow for network congestion

## [2.0.0](https://github.com/THLfi/koronavilkku-android/compare/v1.2.1...v2.0.0) - 2021-01-07

### Added
- User consent and travel information choices required for EFGS interoperability
- Huawei only (unreleased): Contact Shield exposure notification system implementation

### Changed
- Backend API provides country codes and accepts EFGS share consent and travel selections
- UI changes to share diagnosis screen and navigation

### Fixed
- Exposure count in notification detail shows only high risk exposures

## [1.3.0](https://github.com/THLfi/koronavilkku-android/compare/v1.2.1...v1.3.0) - 2020-11-24

### Added
- Manual exposure check allowed when checks have been delayed 24h
- Display exposure notification count on home screen and notification list on exposure detail screen
- Municipality data that controls the availability of Omaolo contact request
- Potential exposure detection based on exposure attenuation durations

### Changed
- Home screen text and icon updates when exposure notifications are turned off
- App structure and build properties to support Huawei Contact Shield build flavor
- Dependency updates

## [1.2.1](https://github.com/THLfi/koronavilkku-android/compare/v1.2.0...v1.2.1) - 2020-10-29

### Added
- English support for municipality data

### Changed
- Attempt to improve background execution by rescheduling delayed workers on app startup

## [1.2.0](https://github.com/THLfi/koronavilkku-android/compare/v1.1.0...v1.2.0) - 2020-10-22

### Changed
- UI text and background functionality updates related to quarantine time changing from 14 to 10 days

## [1.1.0](https://github.com/THLfi/koronavilkku-android/compare/v1.0.2...v1.1.0) - 2020-10-06

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
