# WatchApp — Wear OS Skeleton

Standalone Wear OS watch face + action panel + phone config companion.

## Modules

- `:shared` — models and message paths
- `:wear` — watch face, complications, `RefreshWorker`, action panel
- `:phone` — config UI and action echo service

## Reference images

Visual targets live in [`faces/`](faces/):

- `preview.jpg` — active watch face
- `preview_dim.jpg` — ambient face
- `Screenshot_20260411_091008_WatchMaker.jpg` — action panel layout

## Build

Open this folder in Android Studio Meerkat+ or run:

```bash
./gradlew :wear:assembleDebug :phone:assembleDebug
```

## Install (sideload)

```bash
adb -s <watch> install -r wear/build/outputs/apk/debug/wear-debug.apk
adb -s <phone> install -r phone/build/outputs/apk/debug/phone-debug.apk
```

1. On watch: pick **WatchApp Face** in watch face chooser.
2. Tap face center → action panel.
3. On phone: open **WatchApp Config**, save settings (pushes to watch via Data Layer).
