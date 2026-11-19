# tameki

Android app, Kotlin + Jetpack Compose (Material 3), local-first. Product intent: `PRODUCT.md`. Domain contracts: `docs/spec/domain.md`. Design: `DESIGN.md`.

## Commands
- Everything (CI runs this): `scripts/verify.sh`
- Fast loop: `./gradlew :core:model:test`, `./gradlew :<module>:testDebugUnitTest`
- Machine has limited RAM: never run two Gradle builds at once in the same checkout; pass `--no-daemon` only if the daemon misbehaves.

## Stack
Kotlin 2.x, Compose BOM, Material 3, Navigation Compose (type-safe routes), Room (KSP), DataStore, WorkManager, Koin (see `docs/adr/0003`), kotlinx-serialization, kotlinx-datetime-free (use `java.time`), Glance widgets, CameraX, ML Kit text recognition + ML Kit GenAI Prompt API, BiometricPrompt. Tests: JUnit4, Truth, Turbine, coroutines-test, Robolectric, Roborazzi screenshots.

## Modules and allowed dependencies
| Module | Kind | May depend on |
| --- | --- | --- |
| `core:model` | pure JVM | nothing (stdlib, coroutines, serialization) |
| `core:data` | Android lib | core:model |
| `core:ui` | Android+Compose | core:model |
| `scan:api` | pure JVM | core:model |
| `scan:mlkit` | Android lib | scan:api, core:model |
| `capture` (notification listener) | Android lib | core:model, core:data |
| `feature:*` | Android+Compose | core:model, core:data, core:ui, scan:api |
| `widget` | Android+Glance | core:model, core:data, core:ui |
| `app` | application | everything; owns DI wiring, navigation, deep links, tile |

Features never depend on each other; cross-feature navigation goes through routes defined in `core:ui/navigation`.

## Rules
- **Money is `Money(minor: Long, currency: CurrencyCode)`.** Never `Double`/`Float` for amounts. Parsing and formatting live in core:model (`MoneyParser`) and core:ui (`MoneyFormatter`).
- **Decisions live in pure, tested code in core:model** (cost normalization, recurrence dates, insights, budget pace, notification parsing, AI JSON parsing, receipt text parsing). ViewModels map domain results to UI state; composables only render.
- **Time comes from an injected `java.time.Clock`.** No `LocalDate.now()`, `Instant.now()` or `System.currentTimeMillis()` outside it.
- **Catch with `runCatchingCancellable` from core:model, never plain `runCatching`.** A cancelled coroutine must stay cancelled.
- **Every external capability sits behind an interface** (`ReceiptReader`, `ExchangeRates`, `BackupCrypto`, `ReminderScheduler`...). Implementations are swapped in Koin modules only.
- ViewModels expose one `StateFlow<XxxUiState>` and take events as functions. No business logic in composables, no `!!`.
- All user-visible strings in `strings.xml` with `values/` (English) and `values-it/` (Italian). String names are prefixed by module (`subs_`, `home_`...).
- Every pure function in core:model has unit tests; every screen has at least one Roborazzi screenshot test in light and dark.
- A `@Suppress` or `@SuppressLint` carries its reason on the same line.
- If a test contradicts the task, stop and report the conflict. Never change an expected value, skip a test or regenerate a screenshot just to get a green build.
- Commit small, with clear messages. Never commit a red `scripts/verify.sh`.
- Package root: `dev.antomanc.tameki`. Application id `dev.antomanc.tameki`, debug suffix `.debug`.
