# Tameki privacy policy

Last updated: 18 November 2026

## Italiano

### In breve

Tameki non ha account, non ha un server e non manda i tuoi dati da nessuna parte. Spese,
abbonamenti, scontrini e impostazioni restano nella memoria privata dell'app, sul telefono. Non
usiamo analitica, pubblicità o servizi di segnalazione errori.

### Chi tratta i dati

Lo sviluppatore di Tameki non riceve e non conserva dati personali: non esiste un backend a cui
l'app si colleghi. Il telefono su cui installi l'app resta l'unico posto in cui i dati esistono.

Contatto per domande sulla privacy: an.manc25@gmail.com.

### Cosa resta sul telefono

- Movimenti, conti, categorie, budget, obiettivi, abbonamenti e note.
- Scontrini: le foto che scegli e il testo letto dallo scontrino.
- Bozze in attesa di conferma (Inbox), con il testo della notifica da cui sono nate.
- Impostazioni, valuta di base, cambi salvati, cronologia dei backup.

Questi dati stanno nel database privato dell'app. Nessun'altra app può leggerli e non vengono
inviati a noi.

### Rete

L'app usa la rete in due casi soltanto.

1. **Cambi di riferimento BCE.** Quando converti un importo in una valuta diversa da quella di
   base, Tameki chiede i tassi giornalieri a `api.frankfurter.dev`, che pubblica i tassi di
   riferimento della Banca Centrale Europea. La richiesta contiene la valuta di base scelta; come
   ogni richiesta HTTP, il server vede l'indirizzo IP da cui arriva. I tassi ricevuti vengono
   salvati sul telefono e riusati.
2. **Modello Gemini Nano.** Sui dispositivi che lo supportano, la lettura degli scontrini può usare
   il modello Gemini Nano. Il download e l'esecuzione del modello sono gestiti dal sistema
   (Google Play services), secondo i termini di Google. Lo scontrino e il testo riconosciuto
   restano sul telefono.

Nient'altro esce dall'app senza un tuo gesto esplicito, come esportare un file o condividerlo.

### Accesso alle notifiche (facoltativo)

L'accesso alle notifiche è spento finché non lo attivi in Impostazioni, e Android richiede un
consenso separato di sistema. Quando è attivo:

- Tameki legge solo le notifiche delle app bancarie e dei portafogli che scegli nell'elenco. Le
  altre notifiche vengono ignorate.
- Il testo viene analizzato sul telefono per ricavare importo, esercente e data.
- Nel telefono restano la bozza di spesa e il testo della notifica da cui è nata, finché non
  confermi o scarti la bozza; a quel punto il testo della notifica viene cancellato.
- Nulla viene inviato a noi o a terzi. Puoi revocare l'accesso in qualsiasi momento dalle
  impostazioni di Android.

### Fotocamera e foto

La fotocamera e l'accesso alle foto servono solo per allegare uno scontrino a una spesa. Le
immagini vengono lette sul dispositivo e non vengono caricate su alcun server.

### Backup ed esportazioni

Il backup è cifrato con AES-256-GCM e una password scelta da te, derivata con PBKDF2-HMAC-SHA256.
Senza quella password il file non si apre e nessuno, nemmeno lo sviluppatore, può recuperarla. Il
file cifrato finisce dove decidi tu (una cartella, un servizio di sincronizzazione, un'altra app).
L'esportazione CSV e JSON produce file leggibili che escono dall'app solo quando li condividi.

### Funzioni AI

Il ponte AI di Tameki non chiama nessun servizio AI. L'app copia negli appunti il prompt che hai
scelto e legge il testo che incolli, oppure riceve un testo o un'immagine condivisa con l'app. Se
incolli la risposta di un chatbot, quella risposta è già passata dal servizio che hai usato, con le
sue regole sulla privacy. Tameki non manda nulla a ChatGPT, Claude, Gemini o altri.

### Permessi richiesti

- Notifiche: per i promemoria dei rinnovi e per l'avviso "Aggiunto dall'automazione · Annulla".
- Fotocamera: solo per fotografare uno scontrino.
- Accesso biometrico: per il blocco facoltativo dell'app.
- Internet: solo per i cambi BCE e per il download del modello gestito dal sistema.
- Accesso alle notifiche: facoltativo, come descritto sopra.

### Conservazione e cancellazione

I dati restano sul telefono finché tieni l'app. Puoi cancellare singoli movimenti, svuotare i dati
dalle impostazioni o disinstallare l'app: la disinstallazione rimuove il database. I backup che hai
esportato restano dove li hai messi; cancellali da lì.

### Minori

Tameki non è pensata per i bambini e non raccoglie dati di minori, perché non raccoglie dati.

### Modifiche

Se questa informativa cambia, cambia anche la data in cima. Le modifiche sostanziali verranno
indicate nelle note di rilascio.

## English

### In short

Tameki has no account, no server and no place to send your data. Expenses, subscriptions, receipts
and settings stay in the app's private storage, on the phone. We use no analytics, no ads and no
crash reporting.

### Who processes the data

The Tameki developer receives and stores no personal data: there is no backend for the app to talk
to. The phone where you install the app is the only place your data exists.

Privacy contact: an.manc25@gmail.com.

### What stays on the phone

- Transactions, accounts, categories, budgets, goals, subscriptions and notes.
- Receipts: the photos you choose and the text read from the receipt.
- Drafts waiting for confirmation (Inbox), with the notification text they came from.
- Settings, base currency, stored exchange rates, backup history.

This data lives in the app's private database. No other app can read it, and it is not sent to us.

### Network

The app uses the network in two cases only.

1. **ECB reference rates.** When you convert an amount into a currency other than your base one,
   Tameki asks `api.frankfurter.dev` for the daily rates, which republish the European Central Bank
   reference rates. The request carries the selected base currency; like every HTTP request, the
   server sees the IP address it comes from. The rates are stored on the phone and reused.
2. **Gemini Nano model.** On devices that support it, receipt reading can use Gemini Nano. The
   model download and execution are handled by the system (Google Play services) under Google's
   terms. The receipt and the recognised text stay on the phone.

Nothing else leaves the app without an explicit action from you, such as exporting or sharing a
file.

### Notification access (optional)

Notification access stays off until you enable it in Settings, and Android asks for its own
separate consent. While it is on:

- Tameki reads only notifications from the banking and wallet apps you select. Other notifications
  are ignored.
- The text is parsed on the phone to extract amount, merchant and date.
- The phone keeps the expense draft and the notification text it came from until you confirm or
  discard the draft; the notification text is then deleted.
- Nothing is sent to us or to third parties. You can revoke access at any time in Android
  settings.

### Camera and photos

The camera and photo access exist only to attach a receipt to an expense. Images are read on the
device and are not uploaded anywhere.

### Backups and exports

Backups are encrypted with AES-256-GCM and a password you choose, derived with
PBKDF2-HMAC-SHA256. Without that password the file does not open, and nobody, the developer
included, can recover it. The encrypted file goes wherever you decide (a folder, a sync service,
another app). CSV and JSON exports are readable files that leave the app only when you share them.

### AI features

The AI bridge in Tameki calls no AI service. The app copies the prompt you picked to the clipboard
and reads the text you paste, or receives text or an image shared with the app. If you paste a
chatbot's answer, that answer already passed through the service you used, under its privacy
terms. Tameki sends nothing to ChatGPT, Claude, Gemini or anyone else.

### Permissions

- Notifications: renewal reminders and the "Added by automation · Undo" notice.
- Camera: only to photograph a receipt.
- Biometric access: for the optional app lock.
- Internet: only for ECB rates and the system-managed model download.
- Notification access: optional, as described above.

### Retention and deletion

Data stays on the phone as long as you keep the app. You can delete single transactions, clear all
data from settings, or uninstall the app: uninstalling removes the database. Backups you exported
stay where you put them; delete them there.

### Children

Tameki is not aimed at children and collects no data about minors, because it collects no data.

### Changes

If this policy changes, the date at the top changes too. Material changes will be noted in the
release notes.
