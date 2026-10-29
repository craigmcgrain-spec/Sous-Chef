# Sous Chef

An Android recipe box for links shared from a browser. Share a recipe page and
Sous Chef fetches it, extracts the recipe with Python on the device, saves it,
and opens a clean cookable card with four tabs: Ingredients, Steps, Times &
temperatures, and Shopping.

Ingredients and steps can be ticked off while cooking and the ticks survive a
restart. Recipes can be corrected after import (title, servings, times,
ingredients, steps, reorder, oven temperatures), filed into built-in or custom
categories from the recipe screen, and linked back to their source page. The
screen stays awake while a recipe is open, which can be turned off in the
recipe's overflow menu. Ingredients can be added to one shopping list shared
across recipes. Saved recipes open offline.

Requires Android 13 (API 33) or newer.

## Requirements

- JDK 17 or newer (the wrapper runs on Gradle 9.8.2)
- Android SDK with platform 36 and build tools
- [uv](https://docs.astral.sh/uv/)
- Python 3.13 for the build interpreter

## Setup

Chaquopy compiles the Python bytecode at build time, so it needs a CPython 3.13
on the build machine. Create it once:

```bash
uv python install 3.13
uv venv --python 3.13 .venv-build
uv pip install --python .venv-build/bin/python -r python/requirements-dev.txt
```

The Gradle build uses `.venv-build/bin/python` by default. Point it somewhere
else with the `CHAQUOPY_BUILD_PYTHON` environment variable:

```bash
CHAQUOPY_BUILD_PYTHON=/path/to/python3.13 ./gradlew :app:assembleDebug
```

`local.properties` must contain `sdk.dir=/path/to/Android/Sdk` (Android Studio
writes this for you).

## Build and install

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## Test

```bash
# Python extraction, against fixture HTML
PYTHONPATH=app/src/main/python .venv-build/bin/pytest python/tests -q

# Kotlin unit tests, Compose UI tests on a booted emulator, and the APK
ANDROID_HOME=$HOME/Android/Sdk ./gradlew \
  :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:assembleDebug
```

`connectedDebugAndroidTest` needs a running device or emulator. On a headless
machine:

```bash
~/Android/Sdk/emulator/emulator -avd Test_Android -gpu host -no-window -no-audio &
adb wait-for-device
```

## Share a link

In a browser, use Share and pick Sous Chef. From a terminal, pass the URL in a
`text/plain` send intent:

```bash
adb shell am start -a android.intent.action.SEND -t text/plain \
  -n com.souschef.app/.MainActivity \
  --es android.intent.extra.TEXT 'https://www.bbcgoodfood.com/recipes/classic-lasagne'
```

The component (`-n com.souschef.app/.MainActivity`) matters on emulators with
several apps registered for text shares; without it Android may show a chooser
or pick the wrong target.

With no share intent the app opens the library. "Paste a link" in the library
overflow does the same job without leaving the app.

## Privacy

Everything happens on the phone. The page is fetched directly from the site,
parsed on the device with Chaquopy, and stored locally in DataStore. There is no
account, no server, no analytics, no telemetry, no crash reporting, and no
third-party API. The only network requests are to the recipe page you shared and
the images it references; nothing is sent to us or to any third party.

Recipes are local-only: Android backup is disabled (`android:allowBackup="false"`),
so the library and shopping list are not copied to Google Drive or restored onto
another device.

## How it fits together

```
Share intent ─► MainActivity ─► ShareViewModel ─► RecipeFetcher (OkHttp)
                                                       │ HTML ≤ 8 MB, 15 s
                                                       ▼
                     RecipeParser ── Chaquopy ──► souschef/extract.py
                                                       │ recipe-scrapers,
                                                       │ then JSON-LD
                                                       ▼
                     RecipeStore (DataStore JSON) ─► Compose screens
```

- One Gradle module, `:app`. Kotlin + Jetpack Compose Material 3, with a warm
  paper-and-terracotta palette (no dynamic color).
- Kotlin owns the network fetch and navigation; Python owns extraction. The
  boundary is HTML in, normalized JSON out, so the parser is testable on a
  desktop with fixture pages.
- `AppContainer` in `SousChefApp` wires the fetcher, parser and store; there is
  no DI framework.
- Recipes and the shopping list are JSON blobs in AndroidX DataStore. Repeated
  shares of the same URL open the saved copy; shopping items dedupe by recipe
  and text.
- Edit saves recompute temperatures with the same Python regex used on import,
  so import and edit never drift.

## Known limits

- **Bot walls are not bypassed.** Sites that block non-browser or automated
  clients return their 403 and the app shows "That page isn't available (error
  403)." Try a different link.
- **`http://` pages are fetched unencrypted.** Cleartext traffic is enabled so a
  share of a plain-http recipe page still works. The app sends no credentials or
  cookies, but the page contents travel over the network without TLS; prefer
  `https://` links when the site offers one.
- **The shopping list is a snapshot.** Items are copied when added; later recipe
  edits do not rewrite them. "Add all" only adds lines that are not already
  there for that recipe.
- **APK size.** The debug APK is about 80 MB because it carries Python and both
  ABIs. The design budget of roughly 40–60 MB assumes a release build with
  per-ABI delivery, which this milestone does not produce.
- **Navigation is pinned to 2.9.8** because 2.10.x requires compileSdk 37 and
  this project builds against compileSdk 36.
- **Edit drag has no edge auto-scroll.** Reordering a long list works on screen;
  dragging past the visible area does not scroll it. Use the TalkBack "Move up"
  / "Move down" actions for long moves.
- No timers, serving scaling, unit conversion, search, sync or export. These are
  deliberate omissions.
