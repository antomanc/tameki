# Play Console checklist

Everything to fill in before the first upload, and where each answer comes from. Play Console
labels change from time to time; if a field is named differently, match it by meaning and check
the current form.

## Store listing

| Field | Value | Source |
| --- | --- | --- |
| App name (it-IT) | Tameki: spese e abbonamenti (27 characters) | `fastlane/metadata/android/it-IT/title.txt` |
| App name (en-US) | Tameki: Budget & Subscriptions (30 characters) | `fastlane/metadata/android/en-US/title.txt` |
| Short description | 73 characters in both languages | `short_description.txt` |
| Full description | under 4000 characters in both languages | `full_description.txt` |
| What's new (1) | release notes, under 500 characters | `changelogs/1.txt` |
| Category | Finance |
| Contact email | a real address, monitored | not stored in this repo |
| Privacy policy URL | public page generated from `docs/privacy-policy.md` | see below |
| Default language | Italian (it-IT), with en-US added |

The English title "Tameki: expenses & subscriptions" is 32 characters, over the 30-character
limit, so the store title is "Tameki: Budget & Subscriptions" (exactly 30): it keeps both search keywords.

The privacy policy must be reachable at a public HTTPS URL with no login. Publish
`docs/privacy-policy.md` (GitHub Pages or any static host) and paste that URL. The contact
address is already filled in.

## App content

### Data safety

Answers, with the reasoning, so the form can be filled consistently:

| Question | Answer |
| --- | --- |
| Does the app collect or share required user data types? | No |
| Data types collected | none |
| Data types shared | none |
| Is data encrypted in transit? | not applicable: no user data is transmitted |
| Can users request data deletion? | not applicable: no server-side data; uninstalling or clearing app data deletes everything |
| Is the app's data collection optional? | not applicable |
| Ads or analytics SDKs | none |

What actually crosses the network:

- `api.frankfurter.dev` receives a base currency code when exchange rates are refreshed, and the
  request exposes the usual connection metadata (IP address) to that server. No identifier,
  transaction or amount is sent.
- The optional Gemini Nano download is handled by Google Play services. Receipt text is processed
  on the device by ML Kit and is not uploaded.

If Google's third-party SDK guidance changes for ML Kit, re-check this section before submitting;
the app itself adds no collection of its own.

### Notification access declaration

Under "Sensitive permissions" / "Notification listener access", the justification text. English
first, then Italian.

English:

> Tameki is a local-first expense tracker. Notification access is the core way it captures card
> payments without bank aggregation or a server: the user enables it explicitly, picks the banking
> and wallet apps to read, and Tameki turns those payment notifications into expense drafts that
> the user confirms with one tap. Every notification is parsed on the device. Text from apps that
> are not on the user's allowlist is ignored immediately, nothing is logged, and no notification
> content is uploaded, shared or sold. Access is optional: the app is fully usable without it, and
> the user can revoke it at any time in Android settings.

Italian:

> Tameki è un'app per le spese che funziona interamente sul telefono. L'accesso alle notifiche è
> il modo principale con cui registra i pagamenti con carta senza aggregazione bancaria e senza un
> server: l'utente lo attiva esplicitamente, sceglie le app bancarie e i portafogli da leggere, e
> Tameki trasforma quelle notifiche di pagamento in bozze di spesa da confermare con un tocco.
> Ogni notifica viene analizzata sul dispositivo. Il testo delle app che non sono nell'elenco
> scelto dall'utente viene ignorato subito, nulla viene registrato nei log e nessun contenuto delle
> notifiche viene caricato, condiviso o venduto. L'accesso è facoltativo: l'app è utilizzabile
> anche senza, e l'utente può revocarlo in qualsiasi momento dalle impostazioni di Android.

The declaration asks for a video showing the feature in use. Record: enable capture in Settings,
grant notification access, receive a payment notification from an allowlisted app, open Tameki and
confirm the draft. Keep the video under the size limit shown in the console.

### Content rating

Run the IARC questionnaire and answer from the app's actual content:

- No violence, sexuality, nudity, profanity, drugs, gambling, horror or simulated gambling.
- No user-generated content, no chat, no social features, no user-to-user interaction.
- No advertising, no in-app purchases, no loot boxes.
- No web browsing and no links to external stores inside the app.
- The app shows money amounts and personal finance information; that is not one of the content
  categories the questionnaire rates.

Expected outcome: suitable for all ages (PEGI 3 / Everyone), with the finance category noted.

### Target audience and other declarations

- Target age group: 18 and over. The app is a personal finance tool, not designed for children,
  and it is not submitted to the Families programme.
- App access: no login, no account, no restricted areas. Answer "all functionality is available
  without special access", then note that notification capture and the biometric lock are optional
  features with their own Android consent prompts.
- Ads: no ads.
- News app: no.
- Government app: no.
- Financial features: the app does not offer loans, credit, insurance, payment accounts or
  cryptocurrency services, so the financial products declaration does not apply. It only records
  amounts the user types.
- Health: no health features.
- Data deletion: no account exists, so no deletion request flow is needed.

## Signing

Play App Signing is the default; keep the upload key safe and outside the repository.

Release builds read the upload key from the environment, as implemented in `app/build.gradle.kts`:

| Variable | Meaning |
| --- | --- |
| `TAMEKI_KEYSTORE_PATH` | absolute path to the upload keystore |
| `TAMEKI_KEYSTORE_PASSWORD` | keystore password |
| `TAMEKI_KEY_ALIAS` | key alias |
| `TAMEKI_KEY_PASSWORD` | key password |

When `TAMEKI_KEYSTORE_PATH` is not set, the release build is signed with the debug key. That build
is fine for local testing and must not be uploaded: always set the four variables for a bundle
destined for Play. `*.jks` and `*.keystore` are already ignored by git.

```bash
export TAMEKI_KEYSTORE_PATH="$HOME/.keys/tameki-upload.jks"
export TAMEKI_KEYSTORE_PASSWORD=...
export TAMEKI_KEY_ALIAS=tameki
export TAMEKI_KEY_PASSWORD=...
./gradlew :app:bundleRelease
```

## Graphics and screenshots

Upload through the console or drop the files into the fastlane folders named below.

| Asset | Requirement | fastlane path |
| --- | --- | --- |
| App icon | 512 x 512 px, 32-bit PNG | `fastlane/metadata/android/en-US/images/icon.png` |
| Feature graphic | 1024 x 500 px, PNG or JPEG, no transparency | `.../images/featureGraphic.png` |
| Phone screenshots | 2 to 8, PNG or JPEG, 16:9 or 9:16, 320 to 3840 px per side | `.../images/phoneScreenshots/1.png` ... |
| 7-inch tablet screenshots | optional but recommended | `.../images/sevenInchScreenshots/` |
| 10-inch tablet screenshots | optional but recommended | `.../images/tenInchScreenshots/` |
| Promo video | optional, YouTube URL | console only |

Take phone screenshots in both light and dark theme from the release build. Keep the status bar
clean and avoid showing real transaction data. The list to capture: Today, Activity, Recurring,
the review flow, Save, the quick-add sheet and Settings.

## Before the first release

1. Publish the privacy policy page and paste its URL in the Play Console.
2. Set the upload key variables and build an `.aab`, not the debug-signed APK.
3. Run `scripts/verify.sh` and fix anything it reports.
4. Check the version code in `app/build.gradle.kts`; it is `1` for the first upload.
5. Upload to internal testing first, walk through onboarding, notification capture and a backup
   restore on a clean install.
6. If the account is new to Play, check whether the current closed-testing requirement applies
   before booking a production release date.
