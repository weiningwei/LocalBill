# AGENTS.md

LocalBill — a local-only Android expense tracker (Kotlin, no backend). UI strings, comments, and commit messages are in Chinese.

## Build

- Windows / pwsh: `.\gradlew.bat assembleDebug`. Linux/macOS: `./gradlew`.
- Single module `:app` (namespace/applicationId `com.localbill`). No version catalog (`gradle/libs.versions.toml` doesn't exist) — declare deps directly in `app/build.gradle.kts`.
- AGP 9.3.1 + Gradle 9.5.0 wrapper + Java 17 (`compileOptions`), compileSdk/targetSdk 37, minSdk 26.
- AGP 9 has **built-in Kotlin support** — there is no `org.jetbrains.kotlin.android` plugin. Do not add one.
- `dependencies {}` in `app/build.gradle.kts` is currently empty; that's intentional.
- No tests, no CI, no lint config — `lint`/`test` tasks have no sources.

## Architecture

- **UI is 100% programmatic Android Views** (Activity + LinearLayout/TextView/ImageView built in Kotlin). There are no XML layouts and no Compose. New screens/extensions should use the `UiKit` helpers (`app/src/main/java/com/localbill/util/UiKit.kt`), not new layout XML.
- Colors are resolved through the `Theme` object (`Theme.mainText(ctx)`, `Theme.primary(ctx)`, …) backed by `R.attr` styles in `res/values/attrs.xml`/`themes.xml` (4 accent colors × light/dark). Never hardcode colors — add entries to `C.kt` and attrs/themes, or use existing `Theme.*` accessors.
- Insets: use `UiKit.fitSystemBars(view)` (or the pattern in `MainActivity.buildUi`) to handle edge-to-edge status/nav bars and the IME.
- Global state lives on singletons: `App.instance` / `App.db` (see `App.kt`) and `Prefs` (SharedPreferences). `Prefs` must be initialized before use — it already is, in `App.onCreate`.

## Data layer (`db/DB.kt`)

- Raw `SQLiteOpenHelper`. Schema and seed data live **only in `onCreate`**; `onUpgrade` is an intentional no-op — the project is in fast iteration and does NOT do compatibility or migrations. When the schema or seed data changes, bump the DB version and tell the user to clear app data / reinstall to get the latest.
- Domain encoding (critical, used everywhere):
  - Money is stored as **integer cents** (`Long`), not decimal — use `Money.parseToCents` / `Money.format`.
  - `day` is an `Int` `YYYYMMDD`; `monthKey` is an `Int` `YYYYMM`; `time` is seconds-since-midnight. Use `DateUtil` helpers.
  - `kind` is 1 = expense (`Kinds.EXPENSE`), 2 = income (`Kinds.INCOME`). Categories are two-level: top (parent=0) + sub.
- Account `balance` is reconciled manually in `applyBalance` when bills are added/updated/deleted/restored/hard-deleted. Never update a balance in SQL without preserving that invariant.

## Auto-import (accessibility)

- `accessibility/BillImportService.kt` watches WeChat/Alipay "payment success" screens, parses amount/merchant/time, and opens `RecordActivity` with the form prefilled (user confirms before saving). When touching it: preserve the debounce + dedupe signature logic (`Prefs.lastImportSignature`/`lastImportAtMillis`).
- `accessibility/MerchantMap.kt` maps merchant keyword → top-level expense category name; the list is order-sensitive and unmatched merchants fall back to「其他」.
