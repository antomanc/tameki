# Tameki (貯め木)

Tameki in giapponese significa "albero dei risparmi": 貯め (tame), mettere da parte, e 木 (ki),
albero.

Tameki è un'app Android gratuita per spese e abbonamenti. Tiene in un posto solo i numeri che hai
già, mostra quanto puoi ancora spendere questo mese e indica gli abbonamenti e le abitudini da
tagliare. Funziona interamente sul telefono: nessun account, nessun server, nessuna chiave API.
Italiano e inglese, tema chiaro e scuro, GPL-3.0.

## Funzioni

### Spese

- Aggiunta rapida con numpad e categorie suggerite: importo, un tocco sulla categoria, salva.
- Conti, categorie, note e trasferimenti.
- Divisioni e tag vengono conservati e letti (il filtro dei Movimenti filtra per tag), ma la v1 non
  ha un editor: arrivano da import CSV, automazioni o backup. Le sottocategorie non sono ancora
  esposte nell’interfaccia.
- Importi in valuta estera con una valuta di base; i cambi di riferimento BCE vengono scaricati
  solo quando servono e un cambio inserito a mano ha sempre la precedenza.
- Elenco dei movimenti raggruppato per giorno con i totali, ricerca e filtri per periodo,
  categoria, conto, origine e tag.

### Abbonamenti

- Abbonamenti, bollette ed entrate ricorrenti in un unico elenco, ordinato per prossimo addebito,
  con una striscia dei rinnovi a 30 giorni.
- Totale mensile o annuale di tutto ciò che è attivo, normalizzato da qualsiasi ciclo di
  fatturazione.
- Promemoria prima del rinnovo e prima della fine di una prova gratuita.
- Storico dei prezzi, un avviso quando il prezzo sale e il prezzo del piano annuale quando lo hai
  inserito.
- Voto "vale la pena?" (mi piace, va bene, lo uso poco, non lo uso) e una revisione che richiede
  di nuovo il voto dopo 90 giorni, così un servizio che hai smesso di usare non resta in piedi da
  solo.
- Link per disdire per ogni abbonamento e registrazione automatica facoltativa di ogni addebito.

### Risparmia

- Occasioni ordinate per importo annuo stimato: abbonamenti non usati o usati poco, servizi che si
  sovrappongono, piani annuali, aumenti di prezzo, prove in scadenza, picchi di spesa in una
  categoria rispetto alla tua media, piccole spese frequenti proiettate sull'anno.
- Budget con barra di avanzamento che dice se sei avanti o indietro rispetto a oggi, e obiettivi di
  risparmio.
- Barre della spesa degli ultimi sei mesi e ripartizione per categoria. Le stime sono indicate come
  stime e un risparmio compare solo quando i dati lo sostengono.

### Scontrini

- Allega la foto di uno scontrino a una spesa e leggila sul dispositivo: prima il riconoscimento
  del testo di ML Kit, poi Gemini Nano dove il telefono lo supporta, infine un parser euristico.
- Importo, data, esercente e righe arrivano come bozza da correggere prima di salvare.

### AI senza chiavi

- L'app parla un formato JSON documentato. Copia un prompt pronto, incolla la risposta, guarda
  l'anteprima di ciò che verrà importato. Lo stesso vale condividendo testo o un'immagine con
  Tameki.
- L'app non invia nulla a un servizio AI. Il lavoro lo fa il tuo chatbot; Tameki interpreta il
  risultato in locale.
- Formato ed esempi: [docs/ai-format.md](docs/ai-format.md).

### Automazioni

- Deep link `tameki://add?...` e un'azione intent esplicita per Tasker, scorciatoie e assistenti.
  Per impostazione predefinita si apre la schermata già compilata; i salvataggi silenziosi restano
  spenti finché non li abiliti.
- Scorciatoie sulla home e un riquadro nelle Impostazioni rapide che aprono l'aggiunta rapida.
- Documentazione ed esempi: [docs/automation.md](docs/automation.md).

### Dati e privacy

- Nessun account, nessuna analitica, nessuna pubblicità. Il database resta nello spazio privato
  dell'app.
- Backup e ripristino cifrati con una password scelta da te, manuali o programmati su una cartella
  che scegli.
- Importazione CSV con mappatura delle colonne per arrivare da un'altra app, esportazione CSV e
  JSON.
- Blocco facoltativo con impronta digitale.

## Schermate

<p>
  <img src="fastlane/metadata/android/it-IT/images/phoneScreenshots/1_today.png" width="200" alt="">
  <img src="fastlane/metadata/android/it-IT/images/phoneScreenshots/3_recurring.png" width="200" alt="">
  <img src="fastlane/metadata/android/it-IT/images/phoneScreenshots/4_save.png" width="200" alt="">
  <img src="fastlane/metadata/android/it-IT/images/phoneScreenshots/5_quickadd.png" width="200" alt="">
</p>

Roborazzi registra ogni schermata in chiaro, scuro e al 200% a ogni esecuzione dei test, in
`<module>/build/outputs/roborazzi/`: i PNG servono per la revisione locale, non sono golden
committati. La stessa esecuzione lancia i test di semantica Compose per i componenti condivisi
(`core:ui`) e per la schermata Movimenti (`feature:activity`), quindi un badge o una chip che
perde la sua etichetta TalkBack fa fallire la build.

## Diventa tester

Tameki è in test chiuso su Google Play prima dell'uscita pubblica, e Google chiede almeno 12 tester
per 14 giorni. Se hai un telefono Android puoi dare una mano:

1. Entra nel [gruppo tameki-testers](https://groups.google.com/g/tameki-testers) con il tuo account Google.
2. Apri il [link di test su Play](https://play.google.com/apps/testing/dev.antomanc.tameki), accetta e installa.
3. Tienila installata per 14 giorni e usala un po'. Segnalazioni e idee nelle [Issues](https://github.com/antomanc/tameki/issues).

## Compilazione

Servono JDK 21 e un Android SDK con la piattaforma 37. L'app gira da Android 12 in su (minSdk 31).

```bash
export ANDROID_HOME="$HOME/Android/Sdk"   # oppure imposta sdk.dir in local.properties
scripts/verify.sh
```

`scripts/verify.sh` esegue i controlli di convenzione, i test unitari, lint e le APK debug e
release. Per un ciclo più veloce usa `./gradlew :core:model:test` o
`./gradlew :app:assembleDebug`. La firma della release legge dall'ambiente
`TAMEKI_KEYSTORE_PATH`, `TAMEKI_KEYSTORE_PASSWORD`, `TAMEKI_KEY_ALIAS` e
`TAMEKI_KEY_PASSWORD`; senza queste variabili usa la chiave di debug.

## Architettura

| Modulo | Tipo | Dipende da |
| --- | --- | --- |
| `core:model` | JVM puro | niente: entità, denaro, parser, insight |
| `core:data` | libreria Android | `core:model`: Room, backup, CSV, cambi, impostazioni |
| `core:ui` | Android, Compose | `core:model`: tema e componenti condivisi |
| `scan:api` | JVM puro | `core:model`: contratto del lettore scontrini |
| `scan:mlkit` | libreria Android | `scan:api`, `core:model`: OCR e Gemini Nano |
| `capture` | libreria Android | `core:model`, `core:data`: lettore delle notifiche di pagamento |
| `feature:*` | Android, Compose | `core:model`, `core:data`, `core:ui`, `scan:api` |
| `widget` | Android, Glance | `core:model`, `core:data`, `core:ui` |
| `app` | applicazione | tutto: DI, navigazione, deep link, riquadro |

Le decisioni che meritano un test vivono in `core:model` come funzioni pure; il tempo arriva da un
`java.time.Clock` iniettato, ogni capacità esterna sta dietro un'interfaccia e le feature non
dipendono mai l'una dall'altra.

## Documentazione

- [docs/ai-format.md](docs/ai-format.md): il contratto JSON per i chatbot esterni.
- [docs/automation.md](docs/automation.md): deep link, intent, esempi per Tasker e ADB.
- [PRODUCT.md](PRODUCT.md) e [DESIGN.md](DESIGN.md): perimetro del prodotto e regole di design.
- [AGENTS.md](AGENTS.md): confini dei moduli e convenzioni.

## Licenza

Tameki è software libero, distribuito con la GNU General Public License versione 3. Vedi
[LICENSE](LICENSE).

[LICENSE-EXCEPTION.md](LICENSE-EXCEPTION.md) aggiunge il permesso necessario per collegare Tameki a
Google Play services, Google ML Kit e alle librerie Android Jetpack, che da sole non sono
compatibili con la GPL.
