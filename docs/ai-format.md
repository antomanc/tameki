# Tameki AI exchange format (v1)

Contract between the app and any external chatbot, agent or automation.
The app never calls an LLM API; the user copies a prompt, pastes JSON back (or opens a `tameki://add` link).

Machine-readable schema: [`ai-format.schema.json`](ai-format.schema.json).

## Envelope

```json
{
  "tameki": 1,
  "transactions": [ /* optional */ ],
  "subscriptions": [ /* optional */ ]
}
```

- `tameki` (required): format version. Only `1` is accepted.
- Unknown fields are ignored.
- The parser is lenient: it strips Markdown code fences and surrounding prose, finds the first JSON object, accepts amounts as numbers or strings (including comma decimals), defaults missing `currency` to the user's base currency, and missing `date` to today. It never throws; problems are returned per path.

## Transaction object

| Field | Type | Notes |
| --- | --- | --- |
| `date` | string | ISO-8601 date `YYYY-MM-DD`. Default: today. |
| `time` | string | `HH:MM` or `HH:MM:SS`. Default: noon. |
| `kind` | string | `expense` (default), `income`, `transfer`. |
| `amount` | string or number | Always positive. `"12.50"`, `12.50`, `"12,50"` all work. |
| `currency` | string | ISO 4217. Default: base currency. |
| `merchant` | string | Optional. |
| `category` | string | Hint matched to a local category name/synonym. |
| `account` | string | Hint matched to an account name. |
| `note` | string | Optional. |
| `tags` | string[] | Optional. |
| `items` | array | Line items `{ "name", "amount", "quantity"? }`. |

## Subscription object

| Field | Type | Notes |
| --- | --- | --- |
| `name` | string | Required. |
| `amount` | string or number | Positive period price. |
| `currency` | string | ISO 4217. Default: base. |
| `cycle` | string | `"1 month"`, `"3 month"`, `"1 year"`, `"2 week"`, … |
| `next_date` | string \| null | Next charge date. |
| `trial_ends_on` | string \| null | Trial end date. |
| `category` | string | Hint. |
| `cancel_url` | string \| null | Optional. |

## Example (canonical)

This example is asserted by unit tests — keep it in sync with the schema.

```json
{
  "tameki": 1,
  "transactions": [
    {
      "date": "2026-11-18",
      "time": "13:05",
      "kind": "expense",
      "amount": "12.50",
      "currency": "EUR",
      "merchant": "Esselunga",
      "category": "Groceries",
      "account": "Card",
      "note": "",
      "tags": [],
      "items": [
        { "name": "Latte", "amount": "1.29", "quantity": 1 }
      ]
    }
  ],
  "subscriptions": [
    {
      "name": "Netflix",
      "amount": "13.99",
      "currency": "EUR",
      "cycle": "1 month",
      "next_date": "2026-12-01",
      "trial_ends_on": null,
      "category": "Entertainment",
      "cancel_url": null
    }
  ]
}
```

## Deep link: `tameki://add`

Automation entry for Tasker, shortcuts and assistants. Opens a prefilled add sheet unless the user enabled silent saves and `save=1`.

```
tameki://add?amount=12.50&currency=EUR&kind=expense&merchant=Esselunga&category=Groceries&account=Card&date=2026-11-18&note=Latte&tags=food,shop&save=0
```

| Query | Meaning |
| --- | --- |
| `amount` | Decimal amount (locale-tolerant). |
| `currency` | ISO 4217. Default: base. |
| `kind` | `expense` \| `income` \| `transfer`. |
| `merchant` | Free text. |
| `category` | Category hint. |
| `account` | Account hint. |
| `date` | `YYYY-MM-DD`. |
| `note` | Free text. |
| `tags` | Comma-separated. |
| `save` | `0`/`false` = open sheet (default); `1`/`true` = silent save if allowed. |

Malformed URIs are rejected with a reason string; the app must not invent amounts.
