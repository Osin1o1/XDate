# XDate

Today's Nepali (Bikram Sambat) date on your Android home screen. Small, offline, no ads.

**[Website](https://osin1o1.github.io/XDate/)** · **[Download APK](https://github.com/Osin1o1/XDate/releases/latest/download/XDate.apk)** · Android 7.0+

## Features

- **Five widgets**: Classic 1×1, Tall 1×2, Wide 2×1, Strip 4×1 and Month 4×2
- **नेपाली, English or both**, chosen per widget (long-press → Reconfigure on Android 12+)
- **Readable on any wallpaper**: AUTO picks white or black text; a SOFT background is optional
- **Accurate**: BS calendar data for 2000–2100, checked against the official calendar up to 2084 BS
- **Tiny**: about 70 KB, no libraries, no internet permission
- **Always current**: redraws at midnight, with a 30-minute system refresh as backup

## Build

Needs JDK 17 and the Android SDK.

```bash
./gradlew testDebugUnitTest   # date conversion tests
./gradlew assembleDebug       # installs as "XDate Share", next to the release app
```

Release builds need a `keystore.properties` in the repo root (see `keystore.properties.example`):

```bash
./gradlew assembleRelease     # app/build/outputs/apk/release/app-release.apk
```


## Project layout

| Path | What |
|---|---|
| `app/.../NepaliDateConverter.kt` | AD → BS conversion and the month-length table |
| `app/.../WidgetRenderer.kt` | Draws every widget style |
| `app/.../DateWidgetProvider.kt` | Widget providers and the midnight refresh |
| `docs/` | Website, served by GitHub Pages |
| `scripts/gen-site-calendar.py` | Copies the calendar table into `docs/bs.js`; run it after changing the table |

## License

MIT
