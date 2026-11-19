# 4. Explicit state machines for screen and flow state

Date: 2026-11-19

## Status

Accepted

## Context

Several flows encoded their state as loose flags: `visible` + `readingReceipt` + `canSave`
for the quick-add sheet, `loading` + `found` + `deleted` for the recurring detail, a
`step: Int` for onboarding, and `deleteGeneration` counters next to nullable snapshots for every
Undo snackbar. Each combination the flags allowed but the flow did not (saving twice, "not found"
flashing during a delete, an Undo replayed after a rotation) was a bug waiting to happen, and
several of them shipped.

## Decision

- A flow with more than two states gets an enum or sealed type naming every state, with its legal
  transitions written in the KDoc: `SheetPhase`, `QuickAddMode`, `DetailPhase`, `EditPhase`,
  `ReviewPhase`, `OnboardingStep`. Booleans the screen still reads (`loading`, `visible`,
  `canSave`) are computed from the state, never stored next to it.
- Decisions with domain meaning live in pure reducers in core:model with unit tests:
  `AppLock.reduce` for the app lock, `RecurringLifecycle` for pause/resume/cancel/reactivate and
  the reminders each status implies.
- One-shot events (a snackbar with Undo) carry a token. The view model keeps the Undo payload keyed
  by that token; the screen shows one snackbar per token through `OneShotSnackbar` and
  acknowledges it, so two events in a row are two snackbars and a stale acknowledgement never
  expires a newer Undo.
- Route arguments that are requests ("open the inbox", "filter by this category", "open the budget
  sheet") are consumed once, with the flag in the destination's saved state.

## Consequences

Impossible combinations no longer compile or cannot be reached from the public API, a double tap
is ignored by the state rather than by a debounce, and the lock and recurring rules are covered by
fast JVM tests. New flows follow the same shape.
