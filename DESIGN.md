---
name: Tameki
description: A calm money notebook that shows what you can still spend and where you can save.
colors:
  surface-light: "#F2F0EA"   # warm paper
  surface-dark: "#141513"
  ink-light: "#1C1D1A"
  ink-dark: "#ECEAE3"
  leaf-light: "#2E7D4F"      # saved / kept / under budget
  leaf-dark: "#6FCF97"
  amber-light: "#B7791F"     # at risk, trial ending, price up
  amber-dark: "#F2B544"
  over-light: "#C2412D"      # over budget only
  over-dark: "#FF7A66"
typography:
  display: { fontFamily: Hanken Grotesk, fontWeight: 300, features: tnum }
  body:    { fontFamily: Hanken Grotesk, fontWeight: 400 }
---

# Tameki design

Tameki (貯め木, "tree of savings") is the sibling of Ameki: same typeface, same warm paper neutrals, same quiet instrument-like clarity. The goal is the best-feeling app in its niche: fewer screens, fewer taps, numbers you can read in one glance, and every screen pointing at a concrete saving.

## Experience principles
1. **One number first.** Each screen has a single hero figure: Home = *available to spend this month* (income/budget − spent − upcoming bills before month end); Recurring = *monthly cost* (tap toggles yearly); Save = *possible yearly saving*. Everything else supports it.
2. **Three seconds to log.** The FAB opens the quick-add sheet with the numpad already active. Amount, then one tap on a predicted category chip, then save. Date, account, note, photo are optional chips, never required fields.
3. **Never shame, never nag.** Spending is ink, not red. Red appears only when a budget is actually exceeded. Insights are phrased as opportunities with a yearly amount ("Save ~€84/year"), each with one clear action and a dismiss/snooze.
4. **No dead ends.** Every empty state explains what goes there and offers the action that fills it. Deletes are immediate with Undo in a snackbar, never a confirmation dialog (except restoring a backup or erasing all data).
5. **Honest numbers.** Estimates are labelled as estimates. Missing data stays missing, never zero. Foreign-currency amounts show the original and the converted value.
6. **Calm motion.** Material 3 expressive springs and amounts that roll when they change. All motion respects the system "remove animations" setting. The FAB opens the quick-add sheet without a container transform: that transition is not shipped yet.

## Restraint (the "designer, not AI" rules)
AI-generated UI explains too much and offers too much. Every screen, sheet and dialog is checked against these rules before it is accepted:
1. **The title is the explanation.** No paragraph restating what the screen or sheet does. If a sentence is needed, it is one short line of supporting fact (e.g. "Renews every month · next on 1 Dec"), placed after the actions, centred in sheets, `bodySmall`, muted.
2. **One primary action, at most one secondary.** Anything else (create another, advanced options, "manage elsewhere" hints) goes behind an overflow menu or does not exist. No footnotes pointing to other places.
3. **Labels are short verb + object** ("Save", "Copy prompt", "Cancel subscription"), not phrases ("Send Invite Link Now"). No icon inside a button unless the label alone is ambiguous.
4. **State never replaces a label.** A button keeps its label; confirmation is a snackbar, a haptic, or a brief check that fades back. Never a button that reads "Copied" / "Saved".
5. **Sheets hug their content.** Bottom sheets size to content (no empty lower half); a sheet taller than half the screen is a screen.
6. **Facts, not instructions.** "Expires in 7 days", not "It expires next week, after which you will need to…". Relative dates and numbers over sentences.
7. **Cut, then cut again.** Before accepting a screen, remove every element whose absence nobody would notice. Count the words: if the copy can lose a third without losing meaning, it must.

## Navigation
Bottom navigation with four destinations and a FAB:
- **Oggi / Today**: hero available-to-spend with a slim pace bar (spent vs. where you should be today), upcoming 7 days of renewals and bills, the top savings insight as one card, Inbox badge when notification/AI items wait for confirmation, recent transactions.
- **Movimenti / Activity**: transactions grouped by day with day totals in sticky headers; search field with its own clear, period chips (Any / This month / Last 30 days / This year) that store the preset rather than its dates, so a chip still means the current month after midnight, a filter sheet for category, account and every transaction source (imports included), a count badge on the filter icon that counts the sheet's facets (the period has its own chips), a removable chip per active sheet facet, and one Clear filters action at a time: the sheet and the header share it, and when the no-match empty state is on screen it carries the only copy — clearing never erases the search text and never moves the period. An empty journal offers Add movement; a failed read says so and offers Retry instead of pretending the list is empty. Inbox (pending captured items) as a segmented section at the top when non-empty.
- **Ricorrenti / Recurring**: monthly/yearly total, a 30-day calendar strip with dots on renewal days, list sorted by next date. Each row: brand monogram/logo, name, amount, relative date ("in 3 days"), worth chip. A "Review subscriptions" flow presents one subscription at a time: Keep / Rarely use / Cancel (opens cancel link and marks cancelled).
- **Risparmia / Save**: total possible yearly saving, ranked insight cards, budgets with pace bars, goals, then trends (6-month spending bars, category breakdown as horizontal bars, never pie/donut).
- Settings live behind the top-app-bar icon on Today. FAB long-press: Scan receipt / Paste from AI / New recurring.

## Quick-add sheet
Large amount display (display type, tabular figures) at top, kind toggle (Expense / Income / Transfer) as a segmented button, horizontally scrolling category chips ordered by prediction (merchant history, time of day, frequency), then chips for date ("Today"), account, note, photo/receipt. Custom numpad with decimal key matching the locale and a large Save button. Haptic tick on save, sheet closes, snackbar "Saved · Undo". Editing an existing transaction uses the same sheet.

## Colors
Material 3 roles carry surfaces, text and controls; dynamic color (wallpaper) is on by default for surfaces and accents, with the warm-paper static palette as fallback and as a setting. Data colors are fixed and keep their meaning across themes: **leaf** = saved/kept/on track, **amber** = attention (at risk, trial ending, price increase), **over** = over budget only. Error red is reserved for an actual over-budget amount, never for badges, counts or filters: the filter count badge uses the primary accent, not error red. Category colors are a fixed, tested set of 16 hues tuned for both themes (≥3:1 against surface). Exact values live in `core/ui/.../theme/Color.kt`.

## Typography
Hanken Grotesk, bundled. All amounts use tabular figures; the currency symbol is smaller and lighter than the number; minor units are slightly dimmed in hero figures (€1.284<span>,50</span>). Text uses sp and must stay readable at 200% font scale: rows wrap or stack before truncating an amount. Amounts are never truncated or ellipsized.

## Layout, shape, depth
16 dp gutters, 8 dp grid, cards with 20 dp radius, chips and buttons from the M3 shape scale (no second radius system). Sections are separated by spacing and tonal surfaces; no decorative shadows. Compact and expanded widths currently share the same single column; list-detail panes for tablets and foldables are planned, not shipped yet. Respect system bars, cutouts and IME insets. Touch targets ≥ 48 dp.

## Components (core:ui)
`AmountText` (formatting, tabular, dimmed minor units, roll animation, semantics speaking the full amount), `PaceBar`, `CategoryChip`/`CategoryIcon`, `BrandMark` (logo or coloured monogram), `InsightCard`, `TransactionRow`, `RecurringRow`, `DayHeader`, `EmptyState`, `Numpad`, `SegmentedKind`, `MoneyField`, `SectionHeader`. Screens compose these; they do not restyle them.

## Charts
Follow the `dataviz` approach: bars over pies, direct labels over legends, one highlight color per chart, axis in locale currency, text equivalent for TalkBack. Drawn with Compose Canvas in core:ui, no chart library.

## Accessibility & quality bar
TalkBack labels speak amounts with currency and sign; every icon-only control has a label; contrast AA; 48 dp targets; 200% font scale; dark theme; right-to-left safe. Every screen has Roborazzi screenshots (light, dark, 200% font) and is checked on the emulator before it is called done. Use the `impeccable` skill (`.agents/skills/impeccable`) to critique any new screen against generic "AI slop" UI: no gradients for decoration, no emoji as icons, no card-in-card, no centred everything, no filler copy.
