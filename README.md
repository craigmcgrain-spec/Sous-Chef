# Sous Chef

An Android recipe box for links shared from a browser. Recipes are extracted
on-device with Python (Chaquopy, recipe-scrapers, lxml) and stored locally.

## Build

The build needs JDK 17+ and an Android SDK with platform 36. Chaquopy compiles
Python bytecode on the build machine, so create the 3.13 interpreter first:

```bash
uv python install 3.13 && uv venv --python 3.13 .venv-build
./gradlew :app:assembleDebug
```

Set `CHAQUOPY_BUILD_PYTHON` to use a different build interpreter.

## Run

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.souschef.app/.MainActivity
```
