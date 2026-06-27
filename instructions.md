# 📱 Progetto Android: App di Digital Wellbeing (Anti-Distrazione)

## 🎯 1. Scopo del Progetto
L'obiettivo è sviluppare un'applicazione Android nativa (in Kotlin) per il Digital Wellbeing, progettata per limitare l'utilizzo di app "dannose" o distraenti (es. social network). Il progetto è destinato a un esame universitario, quindi la pulizia del codice, l'architettura (MVVM raccomandata) e l'utilizzo corretto delle API di sistema sono prioritari.

## 🛠 2. Stack Tecnologico & Architettura
* **Linguaggio:** Kotlin
* **UI:** Jetpack Compose.
* **Architettura:** MVVM (Model-View-ViewModel)
* **Persistenza:** * `Room` per salvare lo storico degli utilizzi, log dei blocchi e dati di gamification.
    * `DataStore` per le preferenze utente (es. tema chiaro/scuro, token di auth).
* **Mappe/Geolocalizzazione:** Google Maps SDK o OpenStreetMap (OSM) per il Geofencing.
* **Fotocamera:** CameraX per la scansione dei QR code.
* **Notifiche:** Notifiche locali e in-app.

### ⚠️ Permessi Core Necessari:
Questo progetto richiede permessi avanzati che devono essere gestiti con cura nelle prime fasi:
1. `PACKAGE_USAGE_STATS`: Per leggere le app installate e misurare il tempo di utilizzo.
2. `SYSTEM_ALERT_WINDOW`: Per disegnare la schermata di blocco ("overlay") sopra le app distruttive.
3. `ACCESS_FINE_LOCATION` & `ACCESS_BACKGROUND_LOCATION`: Per il trigger del Geofencing.
4. `CAMERA`: Per la scansione del QR code di sblocco.

---

## 📱 3. Progettazione delle Schermate (UI/UX)

1. **Auth Screen:** Login/Registrazione con Email e Password, e opzione Provider esterno (es. Google SignIn).
2. **Dashboard (Home):**
    * Grafici/Data visualization del tempo di utilizzo giornaliero.
    * Sezione Gamification: Punteggio attuale, badge sbloccati, e classifiche.
3. **App List Screen:** * `RecyclerView` (o `LazyColumn`) con la lista delle app installate nel dispositivo.
    * Barra di ricerca e filtri (es. "Più usate", "Bloccate").
4. **App Detail Screen:** * Statistiche specifiche per la singola app.
    * Impostazione delle regole di blocco (limite di tempo giornaliero, blocco basato su posizione GPS).
5. **Map/Geofence Screen:** Mappa interattiva dove l'utente può posizionare un marker e definire il raggio della "Zona di blocco" (es. Università, Ufficio).
6. **Block Overlay Screen:** La schermata punitiva che appare quando si apre un'app bloccata. Presenta le opzioni di sblocco:
    * Inquadra QR Code.
    * (Bonus) Usa sblocco biometrico.
7. **Profile & Settings Screen:** * Gestione account, toggle tema chiaro/scuro/automatico.
    * **Intent Esterni (Massimizzazione Punteggio Esame):** Integrare un tasto "Condividi i miei risultati" che lancia un Intent verso WhatsApp/Telegram, oppure un tasto "Aggiungi periodo di concentrazione al Calendario" che apre l'app Calendario di Android passando orario di inizio e fine.

---

## 🪜 4. Piano e Ordine Implementativo (Da seguire step-by-step)

**Fase 1: Setup Progetto, Architettura Base e Permessi Speciali**
* Inizializza il progetto e imposta il navigation graph (Navigation Component).
* Implementa il meccanismo per richiedere all'utente di concedere `PACKAGE_USAGE_STATS` (portandolo alle impostazioni di sistema) e `SYSTEM_ALERT_WINDOW`.
* **Obiettivo:** Riuscire a stampare a log la lista delle app installate e il loro tempo di utilizzo.

**Fase 2: Persistenza Dati (Room & DataStore)**
* Crea l'Entity Room per le "Regole di Blocco" (App Package Name, Time Limit, Location ID).
* Crea l'Entity Room per lo storico gamification (Punti, Data).
* Imposta DataStore per salvare l'impostazione del Tema (Chiaro/Scuro).

**Fase 3: Sviluppo UI Core e Autenticazione**
* Implementa Login/Registrazione.
* Implementa la Dashboard, la lista delle App (`App List Screen`) e il dettaglio singolo (`App Detail Screen`).
* Implementa la logica di ricerca/filtro sulla lista app.

**Fase 4: Core Logic del Blocco (Il cuore dell'app)**
* Implementa un Service/Worker in background che monitora periodicamente l'app in foreground.
* Se l'app è in foreground, ha superato il limite e corrisponde a una regola Room, lancia il `Block Overlay Screen` usando l'API WindowManager.

**Fase 5: Sblocco tramite Fotocamera e Biometria**
* Nella schermata di blocco, integra `CameraX` per la lettura del QR Code.
* Aggiungi il fallback con le API `BiometricPrompt`.

**Fase 6: Geofencing e Mappe**
* Integra SDK Mappe.
* Permetti all'utente di selezionare una zona sulla mappa.
* Aggancia la posizione ai Worker: se l'utente è nell'area GPS definita, attiva il blocco di specifiche app.

**Fase 7: Gamification, Grafici, Notifiche e Intent Esterni**
* Implementa le Notifiche (avviso 5 minuti prima del blocco).
* Aggiungi i punti gamification (es. +10 se il timer non viene mai esaurito a fine giornata).
* **Sviluppo Intent Esterno:** Implementa i bottoni per la condivisione dei risultati (Intent verso WhatsApp/Telegram) o per aggiungere il periodo di concentrazione (Intent verso Calendario) come definito nella UI.
