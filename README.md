# COVID-19 Exposure Notifications Android app for Finland

This is the COVID-19 exposure notifications Android app for the Finnish Institute for Health and Welfare (THL). It uses Exposure Notifications API which is a joint effort between Apple and Google to provide the core functionality for building iOS and Android apps to notify users of possible exposure to confirmed COVID-19 cases.

[https://www.google.com/covid19/exposurenotifications/](https://www.google.com/covid19/exposurenotifications/)

## Build and run
Clone the repository, and open in Android Studio 4.0 or later (or build directly with Gradle).

Choose a build variant:
* simGoogleDebug for a simulated exposure notification system which provides dummy responses and allows running the app in emulator
* liveGoogleDebug for Google Play Services exposure notification system on a device. Note that using the exposure notification system is restricted by app id.
* liveHuaweiDebug for Huawei Contact Shield or Google Play Services exposure notification system on a device. Note that using Contact Shield requires configuration files not included in this repository.

Configure the app for your local environment by creating a local.properties file in project root directory with entries that override defaults from gradle.properties. For example:

```
backendUrl=http://10.0.2.2:8080/
enableTestUI=true
```

## Google device requirements
* Android version 6 or above
* Google Play Services version 20.18.13 or above
* Device owner user profile

## Huawei device requirements
* EMUI 5.0 or later
* Huawei Mobile Services (HMS) Core 5.0.2.300 or later

## Backend
See [koronavilkku-backend](https://github.com/THLfi/koronavilkku-backend) for information on application backend.

## Contributing

We are grateful for all the people who have contributed so far. Due to tight schedule of Koronovilkku release we had no time to hone the open source contribution process to the very last detail. This has caused for some contributors to do work we cannot accept due to legal details or design choices that have been made during development. For this we are sorry.

**IMPORTANT** See further details from [CONTRIBUTING.md](CONTRIBUTING.md)
