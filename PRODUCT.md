# Spare (working name)

Open source (GPL-3.0) Android app that tracks expenses and subscriptions and, above all, shows where you can save. Local-first: no account, no server, no API keys. Play Store release; Italian and English.

## Principles
- **Entering an expense takes under 3 seconds.** Numpad sheet, suggested category, widget, Quick Settings tile, share sheet, payment notifications.
- **Every screen answers "where is my money going and what can I cut?"** Savings are shown as a yearly amount in euros, not as abstract charts.
- **Never invent numbers.** An insight shows a saving only when the data supports it (e.g. "switch to annual" only when the user entered the annual price).
- **AI-friendly without keys.** The app speaks a documented JSON format; users bring their own chatbot (ChatGPT, Claude, Gemini) through copy-prompt / paste-result and the share sheet. On-device Gemini Nano improves receipt reading where available.
- **The user's data is theirs.** Encrypted backup, CSV import/export, nothing leaves the phone unless the user exports it.

## Features (v1)
1. **Quick add**: numpad bottom sheet, accounts, categories with subcategories, splits, notes, tags, multi-currency with a base currency.
2. **Receipts**: photo attached to a transaction; on-device reading (ML Kit OCR, then Gemini Nano where available, then a heuristic parser) fills amount, date, merchant and line items for review.
3. **Recurring payments**: subscriptions, bills and recurring income. Reminders before renewal and before a free trial ends, price history with price-increase alerts, normalized monthly/yearly cost, cancel link, "is it worth it?" rating, optional automatic logging of each charge.
4. **Budgets and goals**: per-category or total budgets (weekly/monthly) with pace projection, savings goals, no-spend days.
5. **Savings ("Risparmia") screen**: ranked opportunities with an estimated yearly saving: unused or low-value subscriptions, overlapping subscriptions, annual-plan savings, price increases, trials ending, category spikes vs your average, small frequent purchases projected over a year, budgets at risk.
6. **Payment notification capture** (opt-in): reads notifications from allowlisted banking/wallet apps on device, proposes transactions in an Inbox to confirm with one tap.
7. **AI bridge**: copy a ready prompt (receipt extraction, natural-language bulk entry, month analysis), paste the chatbot's JSON back, preview and import. Share an image or text to the app.
8. **Automation API**: `spare://add?...` deep link and an explicit intent action for Tasker, shortcuts and assistants. Opens a prefilled sheet unless the user allows silent saves.
9. **Widgets** (quick add, month summary, next renewals), Quick Settings tile, app shortcuts.
10. **Data**: encrypted backup/restore (manual and scheduled to a chosen folder), CSV import with column mapping (to migrate from other apps) and CSV/JSON export. Biometric app lock.

## Out of scope (v1)
Bank aggregation, cloud sync, multi-device, in-app LLM API keys, desktop MCP companion.
