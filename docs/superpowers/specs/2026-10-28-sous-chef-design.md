# Sous Chef — Design Spec

Date: 2026-10-28
Status: Approved design, pending spec review
Repo: https://github.com/craigmcgrain-spec/Sous-Chef (currently empty)

## 1. Summary and intent

Sous Chef turns a recipe web page into a clean, cookable card on an Android phone.

The user finds a recipe in their browser, shares the page to Sous Chef, and gets a
saved recipe with four tabs: Ingredients, Step-by-step, Times & Temperatures, and
Shopping. Ingredients and steps can be ticked off while cooking. Recipes can be
corrected after import, and ingredients can be sent to one shopping list shared
across recipes. Saved recipes open offline.

**Who it is for:** one home cook, on their own phone, standing in a kitchen.

**Success looks like:**
- Sharing a URL from Chrome saves the recipe and opens it, cold start to readable recipe under 10 seconds on the test emulator.
- Ticking steps or ingredients survives closing and reopening the app.
- All four tabs are usable one-handed, with wet or messy hands.
- An edited title or step persists, and temperatures refresh from the edited text.
- Ingredients added from a recipe appear once on the shared shopping list and can be ticked off while shopping.
- A page Sous Chef cannot parse fails with a plain-English reason and a retry, never a blank screen or a stack trace.
- A saved recipe renders fully with no network connection.

**Constraints from the brief:** Kotlin + Python, Android 13+ (minSdk 33), tabs as
listed, warm editorial visual direction, Material 3 foundations.

## 2. Scope

### In scope
- Share target for browser URLs (`ACTION_SEND`, `text/plain`).
- On-device HTML fetch and Python parsing (Chaquopy).
- Local recipe library, no account.
- Recipe screen with four tabs: Ingredients, Steps, Times, Shopping.
- Ingredient and step check-off, persisted.
- Times and oven temperatures extracted from the page.
- Recipe editing: title, servings, prep/cook/total, ingredients and steps, with
  add, remove and reorder. Temperatures are recomputed from the edited text.
- One shared shopping list, fed from recipe Shopping tabs and opened from the
  Library.
- Delete a recipe with confirmation.
- Paste-a-link fallback entry point.
- Light and dark themes, phone-first with a sensible tablet reflow.

### Out of scope
Timers, serving scaling, unit conversion, accounts or sync, search, photo/OCR
import, LLM fallback, iOS, CI pipelines, Play Store publishing, analytics, crash
reporting, localization beyond English, shopping-list sharing or export,
per-item quantity aggregation across recipes.

Saying no to these is deliberate. Each is a separate request.

## 3. Architecture

Single Gradle module `:app`. Kotlin + Jetpack Compose Material 3, AGP 9.4.1 with
built-in Kotlin (no `org.jetbrains.kotlin.android` plugin), Compose compiler
plugin, Chaquopy 17.2.0, Python 3.13. App id and namespace `com.souschef.app`,
app name "Sous Chef", versionName 0.1.0.

```
Browser share
   │  ACTION_SEND text/plain
   ▼
MainActivity ──► ShareViewModel
                    │ extract first http(s) URL
                    ▼
              RecipeFetcher (OkHttp)
                    │ HTML (≤8 MB, HTML only, 15 s)
                    ▼
              RecipeParser ── Chaquopy ──► souschef/extract.py
                    │                        recipe-scrapers 15.13
                    │                        → JSON-LD fallback
                    ▼
              ExtractedRecipe (kotlinx.serialization)
                    ▼
              RecipeRepository (DataStore: recipes + shopping JSON)
                    ▼
              Library · Recipe · Edit · Shopping screens
                    │
                    └─ Edit save ── Chaquopy ──► souschef/extract.py
                                          temperatures_from_texts()
```

**Why Kotlin fetches and Python only parses.** One boundary: HTML in, normalized
JSON out. OkHttp owns redirects, cookies and UA. Python needs no network stack,
and `extract.py` is testable with fixture HTML on a normal machine.

**Dependency injection:** none. A small `AppContainer` created in the
`Application` exposes the repository, fetcher and parser. Constructor injection
everywhere else.

**Threading:** fetch and parse on `Dispatchers.IO`. Chaquopy calls are serialized
through a `Mutex`; the Python interpreter is initialized lazily on the first
parse or edit, and the UI shows progress meanwhile.

**HTML fetch rules:** Chrome-mobile User-Agent, follow redirects, 15 s connect
and read timeouts, abort above 8 MB, accept only `text/html` and
`application/xhtml+xml`. No retries beyond the user-facing Retry button.

## 4. Python extraction contract

Module: `app/src/main/python/souschef/extract.py`

```python
def extract(html: str, url: str) -> str: ...
```

Returns a JSON string. Raises `ExtractionError` with a short machine code
(`NO_RECIPE`, `EMPTY_PAGE`, `PARSE_FAILED`) when nothing cookable is found.

```python
def temperatures_from_texts(items: list[str]) -> str: ...
```

Returns the temperature array for edited ingredients and steps, reusing the
same regex and normalization as `extract`. Editing never re-fetches the page and
never duplicates temperature logic in Kotlin.

```json
{
  "title": "Sunday Ragù",
  "author": null,
  "siteName": "BBC Good Food",
  "imageUrl": "https://…",
  "servings": "4",
  "prepMinutes": 15,
  "cookMinutes": 180,
  "totalMinutes": 195,
  "ingredients": ["2 tbsp olive oil", "1kg beef shin"],
  "steps": ["Heat the oven to 160°C.", "Brown the beef."],
  "temperatures": [
    {
      "valueC": 160,
      "valueF": 320,
      "raw": "160°C",
      "context": "Heat the oven to 160°C.",
      "stepIndex": 0
    }
  ],
  "missing": ["author", "imageUrl"]
}
```

**Parsing order**
1. `recipe_scrapers.scrape_html(html, url)` for the 500+ supported sites.
2. Own JSON-LD parser: stdlib `json` + BeautifulSoup, walking `@graph`, arrays,
   `HowToStep`, `HowToSection`, `recipeIngredient`, `cookTime`/`prepTime`/
   `totalTime` in ISO 8601.
3. If neither yields a title and at least one step or ingredient, raise
   `NO_RECIPE`.

**Normalization**
- Times become whole minutes; `totalMinutes` falls back to prep + cook when
  absent. Missing values are `null`, never zero.
- Steps are split on newlines when a site returns one blob; empty steps dropped;
  leading numbering (`1.`, `Step 2:`) stripped only when present on every step.
- Ingredients stay as raw display strings. Quantity parsing is out of scope.
- Temperatures: regex over steps and ingredients for `180°C`, `350 F`,
  `180 °C fan`, `gas mark 4`. Degree-less matches require a `C`/`F` token
  immediately adjacent, so `180g` is never a temperature. Fahrenheit-only values
  get a computed Celsius value; Celsius-only values get a computed Fahrenheit
  value; duplicates collapse by Celsius value, keeping the first context.
  Gas mark 4 maps to 180°C / 350°F.
- `missing` lists human-meaningful absent fields so the UI can explain, not guess.

**Privacy:** the URL and page content never leave the device. No analytics, no
telemetry, no third-party API.

## 5. Data and persistence

```kotlin
@Serializable
data class Recipe(
    val id: String,                 // UUID
    val url: String,
    val title: String,
    val siteName: String?,
    val author: String?,
    val imageUrl: String?,
    val servings: String?,
    val prepMinutes: Int?,
    val cookMinutes: Int?,
    val totalMinutes: Int?,
    val ingredients: List<Ingredient>,
    val steps: List<Step>,
    val temperatures: List<Temperature>,
    val savedAtEpochMs: Long,
)

@Serializable data class Ingredient(val id: String, val text: String, val checked: Boolean)
@Serializable data class Step(val id: String, val text: String, val checked: Boolean)
@Serializable data class Temperature(
    val valueC: Int?, val valueF: Int?, val raw: String,
    val context: String, val stepIndex: Int?,
)

@Serializable data class ShoppingItem(
    val id: String,                 // UUID
    val text: String,
    val checked: Boolean,
    val recipeId: String,
    val recipeTitle: String,        // snapshot, survives recipe deletion
    val addedAtEpochMs: Long,
)
```

**Store:** AndroidX DataStore Preferences with two keys, `recipes` and
`shopping`, each holding JSON via kotlinx.serialization. One repository class,
all writes serialized by a `Mutex`, exposing `Flow<List<Recipe>>` and
`Flow<List<ShoppingItem>>`.

Room is intentionally skipped: the data is small lists with no queries, joins
or migrations. Full-list rewrite on each check is a few kilobytes.
`shortcut: whole-list rewrite; move to Room if the library exceeds ~500 recipes
or needs search.`

**Dedup:** recipes by normalized URL (fragment removed, scheme/host lowercased).
Re-sharing a saved URL opens the saved copy without refetching. Shopping items
by `(recipeId, text)`, so tapping "Add all" twice never doubles a row.

**Deletion:** deleting a recipe removes it from the library, not from the
shopping list. Bought or planned groceries survive, grouped under the stored
recipe title.

**Editing:** ingredient and step ids are UUIDs assigned when the line is created,
so reordering or deleting neighbours never moves a tick to another line. Edited
lines keep their id and checked state; new lines start unchecked.

**Shopping is a snapshot:** items are copied when added. Later recipe edits do
not rewrite the shopping list; "Add all" adds only lines not already present for
that recipe.

## 6. Screens and flows

Routes: `library`, `recipe/{id}`, `recipe/{id}/edit`, `shopping`.

### Library
- Top bar: "Sous Chef" wordmark in Fraunces, 28sp; muted count line ("6 recipes").
  Trailing shopping-cart icon with an unchecked-count badge opens the shopping
  list. Overflow menu: "Paste a link".
- Rows 88dp tall: 64dp rounded thumbnail (Coil) or a paper-toned block with the
  title initial in serif; serif title 20sp; metadata "BBC Good Food · 3h";
  hairline divider.
- Grid instead of list at ≥600dp width: two columns, same row content.
- Empty state: serif "Nothing cooking yet", one sentence about sharing from the
  browser, "Paste a link" text button.
- Long-press does nothing. Deleting lives on the recipe screen.

### Recipe
- Back arrow and overflow (Edit recipe, Delete recipe → confirm dialog).
- 16:9 image when present, rounded bottom corners; title in 32sp Fraunces below
  the image on paper; metadata row "BBC Good Food · 3h · Serves 4", each part
  omitted when unknown.
- Sticky tab row: INGREDIENTS / STEPS / TIMES / SHOPPING, 11sp letter-spaced
  caps, terracotta underline for the selected tab, content crossfade + 8dp
  slide. Scrollable horizontally if the labels do not fit. Selected tab
  survives rotation and process death via `rememberSaveable`.
- **Ingredients tab:** servings line in small caps; rows 56dp minimum with a
  28dp checkbox; ticking strikes and dims the line.
- **Steps tab:** progress rule "3 of 8 done"; step cards with terracotta serif
  numerals (`01`); 17sp/1.55 text; whole card toggles; done cards dim and gain a
  filled check with an animated strike line.
- **Times & temperatures tab:** Prep / Cook / Total as large serif tabular
  numerals ("—" when unknown; section hidden if all unknown). Temperature cards
  show `180°C` with `/ 350°F` secondary, the source sentence beneath, and a
  "Step 4" chip that switches to Steps and scrolls to that step. Empty copy:
  "No oven temperatures found in this recipe."
- **Shopping tab:** "Add all ingredients" text button at the top, then the
  ingredient rows. Tapping a row adds it; rows already on the shared list dim
  and show a check instead. A bottom line reads "6 items on the shopping list"
  with a "View list" action.
- Recipe content max width 720dp, centered on wide screens.

### Edit recipe
- Reached from the recipe overflow. One scrollable form: title (required),
  servings, prep/cook/total minutes (numeric, optional), ingredients, steps.
- Each list has drag-handle reorder (plus TalkBack "Move up" / "Move down"
  actions), a trailing remove icon, and a quiet "+ Add ingredient" / "+ Add
  step" row. Empty lines are dropped on save.
- Top-bar Save; back or up with unsaved changes asks "Discard changes?".
- Title is required; a blank title blocks saving with "Give this recipe a title."
- Saving keeps ids and checked state for retained lines, assigns UUIDs to new
  ones, computes total from prep + cook when total is blank, then recomputes
  temperatures through `temperatures_from_texts`. No network is touched.
- Author, image, source URL and saved date are not edited.

### Shopping list
- Opened from the Library cart icon; accessible even with no recipes.
- Grouped by recipe title in small caps, rows 56dp with the same checkbox as
  ingredients; checked rows strike and dim. Library badge counts unchecked items.
- Overflow: "Clear checked" (immediate) and "Clear all" (confirm dialog).
- Empty state: serif "Nothing to buy", one sentence: "Add ingredients from a
  recipe's Shopping tab."

### Share flow
- `MainActivity` is exported with an `ACTION_SEND` / `text/plain` filter.
- First URL matching `https?://\S+` is used; trailing punctuation stripped.
- Share dialog: "Reading <domain>…" with a thin progress rule and a cancel
  action. Cancelling aborts the fetch and returns to the library.
- Success: save, dismiss, navigate to recipe.
- Already saved: open existing, no fetch.
- Failure: dialog "Couldn't read that recipe" + plain reason, Retry, and
  "Paste a different link".
- Launch with no share data: library.

### Paste a link
Dialog with a URL field. Same fetch/parse path, same error handling.

## 7. Visual system

**Mode:** Operate. Editorial character in type and color; layout optimized for
scanning while cooking.

**Palette**

| Token | Light | Dark |
|---|---|---|
| paper (background) | `#FAF5EB` | `#171310` |
| surface | `#FFFDF8` | `#201B17` |
| ink | `#211C18` | `#F2E9DC` |
| ink muted | `#6B5F55` | `#B3A697` |
| terracotta (primary) | `#B4442A` | `#E48263` |
| terracotta wash | `#F3DDD4` | `#3A241D` |
| rule | `#E4D9C8` | `#3A312A` |
| sage (done) | `#5A6650` | `#93A487` |
| error | `#A33A2B` | `#F08A76` |

Dynamic color is disabled on purpose. The cookbook palette is the brand.

**Type:** Fraunces variable (display, serif numerals) and Inter variable (body,
controls), both OFL and bundled in `res/font`. Scale: 32/28/20/17/15/13/11sp,
body line height 1.55. Numerals in times and step numbers use Fraunces with
tabular figures.

**Shape and depth:** 12dp cards, 20dp image corners, 1dp rules, no elevation or
shadows; a single `surface` tone separates cards from paper.

**Spacing:** 20dp page gutter; 4/8/12/16/24/32 scale.

**Motion:** 220ms standard easing. Tab crossfade with 8dp slide; check pops
1.0→1.15→1.0 with `HapticFeedbackType.ToggleOn` on API 34+, silent fallback
below. When the system animation scale is off, states switch instantly.

**Icons:** Material core icons only (Back, MoreVert, Delete, Link, Share, Check,
Refresh) to avoid the extended-icons size cost.

**Chrome:** edge-to-edge with paper behind the system bars and dark system-bar
icons on light paper (light icons in dark mode). Android 12+ splash uses the
paper color and the launcher icon. Adaptive launcher icon: terracotta
background, paper fork-and-spoon glyph, plus a monochrome layer for themed
icons. Vector only, no bitmaps.

## 8. Error handling and copy

| Situation | User sees |
|---|---|
| No URL in share text | "That share didn't include a link." + Paste a link |
| No connection | "No internet connection. Connect and try again." |
| Timeout / 5xx | "The site didn't respond. Try again in a moment." |
| 403 / 404 | "That page isn't available (error 403)." |
| Not HTML / over 8 MB | "That link isn't a recipe page." |
| `NO_RECIPE` | "No recipe found on that page." |
| Python `PARSE_FAILED` | "Something went wrong reading that page." |
| Duplicate | Opens the saved recipe silently |
| Temperature recompute fails on save | Recipe text still saves; temperatures clear and a snackbar says "Saved, but temperatures couldn't be refreshed." |

All failures offer Retry where a retry can help, keep the URL visible, and log
the technical detail to Logcat. No raw exceptions in UI. The previous screen and
saved library are never disturbed by a failed share.

## 9. Accessibility and performance

- Body text contrast ≥ 7:1 on paper; terracotta used for large text, icons and
  indicators, not small body copy.
- Every checkbox/toggle exposes role, state and `stateDescription` ("Done",
  "Not done") to TalkBack.
- 48dp minimum touch targets; step cards are full-width targets.
- Tabs are distinguishable by underline and weight, not color alone.
- Font scale to 200%: no fixed-height text containers, titles wrap, tabs scroll
  horizontally if needed.
- Images have `contentDescription` = recipe title; decorative rules are null.
- Lists use `LazyColumn`/`LazyVerticalGrid`; recipe body uses a single
  `LazyColumn` per tab.
- Python init happens off the main thread; the UI remains interactive while
  parsing.

## 10. Testing

**Python (pytest, local CPython 3.13):** fixtures for a recipe-scrapers site, an
unknown site with JSON-LD, a page with no recipe, `HowToSection` steps, and
temperature variants (`180°C`, `350 F`, `gas mark 4`, `180g` negative, `fan
160`). Assert the normalized JSON, not scraper internals.
`temperatures_from_texts` runs against the same variants, including reordered
and edited lines.

**Kotlin unit (JUnit):**
- Decode Python JSON fixtures into `ExtractedRecipe`; missing/extra fields.
- Share text → URL extraction, including trailing punctuation and multiple URLs.
- Duration formatting (`195` → "3h 15m", `45` → "45m").
- Temperature display pairing and fallback.
- Repository dedup by normalized URL, and shopping dedup by `(recipeId, text)`.
- Edit save: blank title rejected, empty lines dropped, total computed from
  prep + cook, reorder keeps ids and checked state.
- Shopping badge counts unchecked items; "Clear checked" removes only those.

**Instrumented (Compose UI test, emulator `Test_Android`):**
- Empty library state renders and "Paste a link" opens the dialog.
- A seeded recipe shows all four tabs and switches between them.
- Ticking a step then recreating the activity keeps the step checked.
- Adding an ingredient from the Shopping tab shows it on the shopping list, and
  adding again does not duplicate it.
- Editing the title updates the recipe and the library row.

**Manual gate before calling it done:**
- `adb shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT <url>` on the emulator with a real recipe URL.
- Screenshots of all four tabs in light and dark, plus the empty, edit, shopping and error states.
- Airplane mode after saving: recipe still opens with all tabs.

## 11. Build and tooling

- Gradle wrapper 9.8.2 (Gradle 9.x supports the installed JDK 25; AGP requires
  JDK 17+). Single `gradle/libs.versions.toml` version catalog.
- AGP 9.4.1, the newest release inside Chaquopy 17.2's supported range
  (7.3–9.4). Kotlin 2.3.21 declared explicitly for both KGP and the Compose
  compiler plugin, which is the documented way to raise the built-in Kotlin
  version. Do not apply `org.jetbrains.kotlin.android`.
- `buildFeatures { compose = true }`, Compose BOM 2026.06.01 (the newest BOM
  whose Compose release targets compileSdk 36 rather than 37).
- compileSdk 36, targetSdk 36, minSdk 33. Other AndroidX/Coil/OkHttp versions:
  latest stable compatible, pinned in the catalog.
- Chaquopy: `python { version = "3.13"; pip { install("recipe-scrapers==15.13.0") } }`.
  `buildPython` is a uv-installed CPython 3.13 (`uv python install 3.13`), path
  recorded in `gradle.properties` as `chaquopy.buildPython` and not committed.
- ABIs: `arm64-v8a` and `x86_64` only. No 32-bit.
- `lxml` (via `extruct`) must resolve to Chaquopy's Android wheel for cp313;
  otherwise the build fails loudly rather than silently shipping a parser that
  cannot start.
- R8/minify off for this milestone; no signing config, no release publishing.
- Git: `main` branch, remote `git@github.com:craigmcgrain-spec/Sous-Chef.git`.
  Commit the spec, then implementation commits as work lands. Push when the user
  asks.

**Skeleton gate (first implementation step):** an app module that builds, starts
to a paper-colored screen, and calls `souschef.extract.extract` from a button via
Chaquopy. Install on `Test_Android`. This proves AGP + Kotlin + Compose +
Chaquopy + Python 3.13 + `lxml` before any product UI exists.

## 12. Risks and fallbacks

1. **AGP/Chaquopy/Compose toolchain mismatch.** Highest risk; the skeleton gate
   surfaces it in the first hour. Fallback: drop to the oldest AGP in Chaquopy's
   supported range that Compose supports, and pin the matching Compose BOM.
2. **`lxml` Android wheel unavailable or broken at runtime.** Fallback: drop
   `recipe-scrapers`/`extruct` and ship JSON-LD-only extraction with BeautifulSoup
   + stdlib `json`. Coverage drops but the contract and UI are unchanged.
3. **Site blocks non-browser clients (403 / bot wall).** We do not bypass
   protection. The user sees the 403 copy and can paste a different link.
4. **Oversized or JS-rendered pages.** Cap at 8 MB; JSON-LD is still present in
   most server-rendered recipe blogs. No headless browser in v1.
5. **First-parse latency (Chaquopy + imports).** Progress dialog; no blocking
   main thread. If it exceeds ~4 s on the emulator, pre-warm the interpreter at
   app start on a background dispatcher.
6. **Temperature false positives.** Unit-adjacency rules and negative tests;
   context sentence shown so the cook can judge.
7. **APK size (~40–60 MB).** Accepted for on-device Python; 32-bit ABIs omitted.
8. **Data loss on corrupt store.** Repository treats unparseable JSON as an empty
   library and keeps the raw string under `recipes_backup` before overwriting.
9. **Shopping list drifts from edited recipes.** Items are snapshots by design;
   "Add all" re-adds only missing lines. Acceptable for a grocery list that must
   stay stable while you shop.

## 13. Decisions recorded

| Decision | Choice | Why |
|---|---|---|
| Python placement | On-device Chaquopy | Private, offline, no server cost |
| Persistence | Saved library | User asked for a cookbook |
| Unsupported sites | recipe-scrapers → own JSON-LD → error | High coverage, no network dependency |
| Storage | DataStore JSON list | No queries, no migrations, least code |
| Fetching | Kotlin/OkHttp | One clean boundary, testable parser |
| Design | Warm editorial cookbook | User chose it; readable with wet hands |
| Shopping list | Recipe Shopping tab feeds one shared list | User chose it; groceries span recipes |
| Recipe editing | Essential fields + reorder | Fixes extraction errors without becoming a recipe editor |
| Edited temperatures | Recomputed in Python from edited text | One regex implementation, no drift between import and edit |
| Dynamic color | Off | Preserves the cookbook palette |
| DI | Manual `AppContainer` | One-screen-scale app; Hilt is overhead |
| Icons | Core Material icons | Avoid extended-icons APK cost |
