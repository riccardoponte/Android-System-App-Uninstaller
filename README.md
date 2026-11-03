# App Uninstaller Pro — Disinstallatore App Utente e di Sistema per Android

Applicazione nativa per Android (**Kotlin + Jetpack Compose**) che permette di disinstallare qualsiasi applicazione dal proprio smartphone — comprese le **app di sistema e il bloatware preinstallato** — senza bisogno di permessi di Root e **senza dover installare applicazioni esterne** grazie al **Motore ADB Locale Integrato**.

---

## Funzionalità Principali

- **Motore ADB Interno Integrato (Zero App Esterne)**:
  - Include al suo interno un client ADB nativo (`libadb-android` + TLS 1.3 `libconscrypt` + `libspake2` + generatore chiavi X.509/RSA a 2048 bit) che comunica direttamente con il demone interno del telefono (`127.0.0.1`) per rimuovere e ripristinare anche le app di sistema in 1 tocco (`pm uninstall --user 0 <pkg>`).
  - Supporta l'accoppiamento rapido tramite **Notifica Interattiva** con il *Debug Wireless* di Android (inserendo `PORTA:CODICE` direttamente nella tendina notifiche senza uscire dalle Impostazioni).
  - Supporta inoltre automaticamente i permessi di **Root (`su`)** se presenti e la modalità standard di Android per le app utente.
- **Indicatore di Sicurezza Automatico**:
  - 🟢 **Utente / Bloatware Sicuro**: app installate dall'utente o app preinstallate non essenziali (social, promo, Bixby, AR Zone, Game Launcher, ecc.) che possono essere rimosse in totale sicurezza.
  - 🟡 **Sistema (Con cautela)**: app di sistema legate a funzioni specifiche del telefono.
  - 🔴 **Critica (Protetta)**: componenti vitali del sistema operativo (`SystemUI`, `Telefono`, `Impostazioni`, `PackageInstaller`, ecc.) protetti con un lucchetto contro la selezione multipla accidentale.
- **Selezione Multipla (Batch Uninstall)**:
  - Seleziona più app contemporaneamente (o usa il pulsante rapido *"Seleziona tutto il Bloatware sicuro"*), visualizza lo spazio in MB che verrà liberato e disinstallale tutte in blocco con un solo tocco.
- **Sezione "Ripristina" (Recupero App di Sistema in 1 Tocco)**:
  - Tutte le app di sistema rimosse o disattivate compaiono nella scheda **Ripristina** e possono essere reinstallate istantaneamente (`cmd package install-existing <pkg>`) senza bisogno di scaricarle da Internet.

---

## Installazione

### Metodo 1 — Direttamente su Smartphone
1. Scarica il file **`APK_Disinstallatore app di sistema.apk`** presente in questo repository.
2. Aprilo sul tuo smartphone Android e conferma l'installazione.

### Metodo 2 — Tramite ADB da PC
```powershell
adb install -r "APK_Disinstallatore app di sistema.apk"
adb tcpip 5555
```

---

## Come si usa

### Attivazione del Motore ADB Interno (una sola volta)
- **Se hai collegato il telefono al PC con `adb tcpip 5555`**:
  1. Apri **App Uninstaller Pro** e tocca **"Connetti"** in alto.
  2. Spunta *"Consenti sempre da questo computer"* nel popup di Android e premi **Consenti**.
- **Direttamente dal telefono senza PC (tramite Debug Wireless)**:
  1. In **App Uninstaller Pro**, tocca **"Configura"** in alto e premi **"Avvia Notifica + Apri Opzioni Sviluppatore"**.
  2. Nelle Opzioni Sviluppatore entra in **Debug Wireless** -> **Accoppia dispositivo con codice di accoppiamento**.
  3. Abbassa la tendina delle notifiche e scrivi `PORTA:CODICE` (es. `39123:654321`) direttamente nella notifica di **App Uninstaller Pro**.

### Rimozione e Ripristino App
1. Nella scheda **Installate**, filtra per *Tutte*, *Utente*, *Sistema* o *Bloatware Sicuro*.
2. Seleziona una o più app e premi **Disinstalla**.
3. Se in futuro vuoi recuperare un'app di sistema rimossa, apri la scheda **Ripristina** e tocca **Ripristina** accanto al suo nome.

---

## Stack Tecnologico

- **Linguaggio**: Kotlin 2.2 (JVM 17)
- **UI**: Jetpack Compose + Material 3
- **Motore ADB Integrato**: `libadb-android` + Conscrypt TLS 1.3 + SPAKE2 + generatore ASN.1 DER X.509 v3 / RSA 2048-bit
- **Persistenza**: Jetpack DataStore Preferences
