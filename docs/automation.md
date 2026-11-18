# Tameki automation

Local-only entry points for Tasker, assistants, shortcuts and ADB. No API keys.

## Deep links

### Add a transaction

```
tameki://add?amount=12.50&currency=EUR&kind=expense&merchant=Esselunga&category=Groceries&account=Card&date=2026-11-18&note=Latte&tags=food,shop&save=0
```

| Query | Meaning |
| --- | --- |
| `amount` | Decimal amount (locale-tolerant). |
| `currency` | ISO 4217. Default: base currency. |
| `kind` | `expense` \| `income` \| `transfer`. |
| `merchant` | Free text. |
| `category` | Category hint. |
| `account` | Account hint. |
| `date` | `YYYY-MM-DD`. |
| `note` | Free text. |
| `tags` | Comma-separated. |
| `save` | `0`/`false` opens the quick-add sheet (default). `1`/`true` silent-saves only if Settings → Automations → “Allow silent saves” is on. |

Silent saves show a notification: **Added by automation · Undo**.

### Paste AI import

```
tameki://import
```

Opens the AI paste-result screen.

## Explicit intent

Action: `dev.antomanc.tameki.action.ADD_TRANSACTION`

Extras match the deep-link query names (`amount`, `currency`, `kind`, `merchant`, `category`, `account`, `date`, `note`, `tags`, `save`).

## Tasker examples

1. **Open prefilled sheet**
   - Action → System → Send Intent
   - Action: `android.intent.action.VIEW`
   - Data: `tameki://add?amount=3.50&merchant=Coffee&save=0`
   - Target: Activity

2. **Silent save (requires setting)**
   - Data: `tameki://add?amount=3.50&merchant=Coffee&save=1`
   - Enable Settings → Automations → Allow silent saves

3. **Explicit action**
   - Action: `dev.antomanc.tameki.action.ADD_TRANSACTION`
   - Extra `amount`: `12.50`
   - Extra `merchant`: `Esselunga`
   - Package: `dev.antomanc.tameki` (debug: `dev.antomanc.tameki.debug`)

## ADB examples

```bash
# Prefill sheet
adb shell am start -a android.intent.action.VIEW \
  -d 'tameki://add?amount=12.50&merchant=Esselunga&save=0' \
  dev.antomanc.tameki/.MainActivity

# Explicit intent
adb shell am start -a dev.antomanc.tameki.action.ADD_TRANSACTION \
  --es amount 12.50 --es merchant Esselunga --es save 0 \
  dev.antomanc.tameki/.MainActivity

# Paste flow
adb shell am start -a android.intent.action.VIEW \
  -d 'tameki://import' \
  dev.antomanc.tameki/.MainActivity
```

Debug builds use application id `dev.antomanc.tameki.debug`.

## Shortcuts and tile

- App shortcuts: New expense, Scan receipt, Paste from AI
- Quick Settings tile: New expense → opens the quick-add sheet
- Home widgets: Quick add (1×1 / 2×1), Month summary (2×2 / 4×2)
