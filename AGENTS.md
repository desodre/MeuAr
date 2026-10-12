# Repository Guidelines

## Project Structure

This is a Kotlin Multiplatform app for Android and iOS. Shared Compose UI, station selection, and Haversine logic live in `shared/src/commonMain/kotlin/com/desodre/meuar`. Android HTTP, location, and map integration live in `shared/src/androidMain`; iOS equivalents live in `shared/src/iosMain`. `androidApp` and `iosApp` contain platform entry points and permission configuration. Keep platform APIs out of `commonMain` and use `expect`/`actual` only where needed.

## Build and Test

Run `./gradlew :androidApp:assembleDebug` for Android. Run `./gradlew :shared:testAndroidHostTest` for shared Android host tests. Build the iOS app from `iosApp` in Xcode on macOS; run `./gradlew :shared:iosSimulatorArm64Test` there for iOS tests. Gradle needs a writable user home and may need network access to resolve dependencies.

## Style and Data Handling

Use descriptive Kotlin names and keep pure calculations in `commonMain`. The MonitorAr `/todas` response is used for station discovery; `/por-ids?ids=<id>` is used for a selected station. Coordinates come from the station's `nuLatitude` and `nuLongitude`, not municipality coordinates. These are strings in the API, and invalid coordinates must be skipped. The IQAr value can be a number or an object with `parsedValue`; normalize both. Display measurement time and the stale-data flag without assuming a timezone. Do not represent a station measurement as the air quality at the user's exact address.

## Tests and Changes

Add focused tests for distance and station selection under `shared/src/commonTest`. Check Android compilation and tests after UI or platform changes, and check iOS compilation on macOS. Document any build limitation in the change report. Keep credentials and machine-specific configuration out of version control.
