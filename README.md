# XDate 📅

A minimal Android app that shows today's date on your home screen widget — both English and Nepali (Bikram Sambat).

**Download:** https://osin1o1.github.io/XDate/ (latest APK: [XDate.apk](https://github.com/Osin1o1/XDate/releases/latest/download/XDate.apk))

## Features
- **Five widgets**: Classic 1×1, Tall 1×2, Wide 2×1, Strip 4×1, Month 4×2
- **Nepali, English or both** in every widget, chosen in the setup screen (long-press → Reconfigure on Android 12+)
- **Readable on any wallpaper**: AUTO picks white or black text from the wallpaper; WHITE/BLACK and a SOFT background are optional
- **Accurate dates**: BS calendar data for 2000–2100, matching the published Panchang up to 2084 BS (`NepaliDateConverter.kt`)
- **App** displays both English and Nepali full dates and follows system dark/light mode
- **Tiny size**: no AndroidX libraries, R8 minified, about 70 KB
- **Battery friendly**: redraws at midnight, with a 30-minute system refresh as backup

Back up `keystore.properties` and the keystore file it points to. Every future update must be signed with the same key, or it won't install over the old version.

## Publishing a release

GitHub Actions (`.github/workflows/release.yml`) builds, signs and publishes the APK when a version tag is pushed:

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`, then commit.
2. `git tag v1.2 && git push origin main v1.2` (the tag must match `versionName`).

The workflow needs these repository secrets: `KEYSTORE_BASE64` (`base64 -w0 your-key.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
The website lives in `docs/` and is served by GitHub Pages from the `main` branch.

## Release build

The project is wired for production signing through a `keystore.properties` file in the repo root.
Release builds will fail until that file is present and complete.

Create the file with these keys:

```properties
storeFile=/absolute/path/to/upload-key.jks
storePassword=yourStorePassword
keyAlias=yourKeyAlias
keyPassword=yourKeyPassword
```

Then build a release artifact:

```bash
./gradlew assembleRelease
./gradlew bundleRelease
```

Use debug builds for local testing:

```bash
./gradlew assembleDebug
```

## Build

```bash
# Set Android SDK path (if not already set)
export ANDROID_HOME=~/android-sdk

# Build debug APK for local testing
./gradlew assembleDebug

# APK output: app/build/outputs/apk/debug/app-debug.apk
```

## Shipping checklist

- Replace the fallback debug signing by adding your real upload keystore.
- Install the release APK on a device and confirm the launcher app and widget both open.
- Test on an Android 7.0+ phone, since `minSdk 24` is required.

## Tech Stack
- Kotlin
- Material Design 3
- AndroidX AppCompat
- Native AppWidgetProvider

## License
MIT
