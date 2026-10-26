# Guida Utente - Mail Backup Visual

## 1. Cos'è MBV
MBV è un visualizzatore desktop per archivi email (formati MBOX ed EML) con indicizzazione full-text basata su Apache Lucene. Permette di importare grandi archivi, cercare istantaneamente, ricostruire conversazioni e filtrare i messaggi.

## 2. Requisiti
- **Per la versione pronta (raccomandata):** Windows 10/11 - Nessun Java richiesto (runtime incluso)
- **Per eseguire da sorgente:** Java 21 JDK + Maven 3.8+

## 3. Installazione / Avvio

### Opzione A - App pronta (senza installazione Java)
1. Scarica `MBV-Windows.zip` dalla sezione [Releases](https://github.com/DhaliRangel/MailBackupVisual/releases) di GitHub
2. Estrai tutto il contenuto in una cartella (es. `C:\Programmi\MBV\` o `Desktop\MBV\`)
3. Avvia `MBV.exe` nella cartella estratta
4. L'app si avvia direttamente - nessuna finestra di terminale necessaria

### Opzione B - Da sorgente
```bash
cd mbv
mvn clean package -DskipTests
mvn javafx:run
```
Oppure eseguire il JAR:
```bash
java -jar target\mail-backup-visualizer-1.0.0-SNAPSHOT.jar
```

### Opzione C - Generare app image personalizzata
Con JDK 21 e `jpackage` (vedi `mbv/README.md` sezione "Windows desktop app").

## 4. Primo utilizzo

All'avvio MBV mostra l'interfaccia in stile Outlook:
- **Pannello sinistro/superiore:** lista email
- **Pannello destro/inferiore:** visualizzazione messaggio (testo/HTML)
- **Barra filtri/ricerca:** in alto
- **Timeline thread:** per visualizzare conversazioni

I file di lavoro vengono creati nella cartella da cui avvii l'app:
- `mbv-index/` - Indice Lucene (ricerca veloce)
- `mbv-cache.db` - Cache SQLite

## 5. Importare archivi email

1. Avvia MBV
2. Usa la funzione di import (dal menu/toolbar dell'app) per selezionare:
   - File `.mbox` (Gmail Takeout, Thunderbird, Apple Mail)
   - File `.eml` singoli o multipli
   - Archivi compressi `.zip` o `.7z` contenenti mbox/eml
3. L'indicizzazione può richiedere tempo per archivi molto grandi (decine/centinaia di migliaia di messaggi) - l'operazione viene eseguita in background
4. Al termine, i messaggi sono disponibili nella lista e ricercabili

**Consigli:**
- Per archivi molto grandi, assicurati di avere spazio libero sufficiente (indice può occupare 10-30% della dimensione originale)
- Mantieni i file originali - MBV importa/copia per indicizzazione, non modifica gli archivi sorgente

## 6. Ricerca e filtri

### Ricerca full-text
Digita nella barra di ricerca. Supporta sintassi tipo Google:

| Sintassi | Descrizione | Esempio |
|----------|-------------|---------|
| `testo` | Ricerca in tutti i campi | `fattura 2025` |
| `"frase esatta"` | Cerca frase esatta | `"ordine confermato"` |
| `from:mittente` | Filtra per mittente | `from:nome@azienda.it` |
| `to:destinatario` | Filtra per destinatario | `to:cliente@esempio.com` |
| `subject:oggetto` | Cerca nell'oggetto | `subject:preventivo` |
| `body:corpo` | Cerca nel corpo messaggio | `body:pagamento` |
| `after:YYYY-MM-DD` | Messaggi dopo data | `after:2025-01-01` |
| `before:YYYY-MM-DD` | Messaggi prima data | `before:2025-12-31` |
| `has:attachment` | Solo con allegati | `has:attachment` |
| `attachment:tipo` | Per estensione/tipo allegato | `attachment:pdf` |

Esempio combinato:
```
from:mario.rossi@azienda.it subject:fattura after:2025-06-01 has:attachment
```

### Filtri aggiuntivi
Usa la barra filtri per affinare per mittente, dominio, tipo allegato, intervallo date.

## 7. Visualizzazione conversazioni (Thread)
MBV ricostruisce automaticamente i thread usando header `In-Reply-To` e `References`. Seleziona un messaggio per vedere la cronologia completa della conversazione nella timeline dei thread.

## 8. Esportazione
Dall'interfaccia è possibile esportare messaggi/selezioni in:
- **CSV** - Per analisi/tabulazione
- **EML** - Singolo messaggio in formato RFC822
- **PDF** - Per archiviazione/lettura

## 9. Temi
Cambia tema dalle impostazioni/app per scegliere tra 6 varianti (chiari e scuri): Blue, Green, Orange, Dark Blue, Dark Green, Dark Orange.

## 10. Consigli e risoluzione problemi

### Prestazioni
- Per archivi enormi, la prima indicizzazione è la più lenta ma successiva è istantanea
- Chiudi altre applicazioni pesanti durante l'import iniziale
- L'indice cresce con l'archivio - assicurati spazio su disco adeguato

### Problemi comuni

**L'app non si avvia (versione pronta)**
- Verifica di aver estratto TUTTO il contenuto dello zip (mantenendo struttura cartelle)
- Avviala dalla cartella estratta, non da dentro un file zip aperto
- Se Windows mostra avviso SmartScreen: "Ulteriori informazioni" → "Esegui comunque" (normale per app non firmate digitalmente)

**Ricerca non trova messaggi appena importati**
- Attendi il completamento dell'indicizzazione (potrebbe essere ancora in corso)
- Verifica che l'import sia terminato correttamente

**Errore permessi su cartella**
- Avvia da una cartella scrivibile (es. Desktop, Documenti). Evita cartelle protette come `C:\Program Files\` se non hai permessi adeguati

**Dati persi?**
- `mbv-index/` e `mbv-cache.db` sono nella cartella di avvio. Se li sposti/elimini, l'indice va ricostruito re-importando o perdendo l'indicizzazione. Fai backup se necessario.

## 11. Struttura cartelle consigliata per distribuzione
```
MBV/
├── MBV.exe
├── app/        (jar e configurazioni)
└── runtime/    (JRE bundlato)
```

Mantenere sempre questa struttura intatta.

## 12. Supporto e info
- **Repository:** [https://github.com/DhaliRangel/MailBackupVisual](https://github.com/DhaliRangel/MailBackupVisual)
- **Licenza:** MIT - Copyright (c) 2026 Dhali Rangel
- **Documentazione tecnica:** Vedi `mbv/README.md`
