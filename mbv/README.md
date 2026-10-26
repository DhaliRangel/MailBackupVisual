# Mail Backup Visual

Desktop email archive explorer with Apache Lucene-powered full-text search.

Esplore di archivi email desktop con ricerca full-text potenziata da Apache Lucene.

---

## Overview / Panoramica

**EN:** MBV is a JavaFX desktop application designed to import, index, and search through large email archives (MBOX, EML). Built with Apache Lucene for near-instantaneous full-text search even on archives with hundreds of thousands of emails.

**IT:** MBV è un'applicazione desktop JavaFX progettata per importare, indicizzare e cercare within grossi archivi email (MBOX, EML). Costruita con Apache Lucene per una ricerca full-text quasi istantanea anche su archivi con centinaia di migliaia di email.

---

## Features / Funzionalità

- **Multi-format import / Import multi-formato**: MBOX and EML support with automatic format detection
- **Full-text search / Ricerca full-text**: Google-like search syntax with operators (from:, subject:, after:, before:, body:, has:attachment)
- **Advanced filtering / Filtri avanzati**: Filter by sender, recipient, domain, date range, attachment type, and more
- **Thread reconstruction / Ricostruzione thread**: Automatic conversation threading via In-Reply-To/References headers
- **Multi-theme support / Multi-tema**: 6 themes (Blue, Green, Orange, Dark Blue, Dark Green, Dark Orange)
- **Export capabilities / Esportazione**: Export to CSV, EML, and PDF formats
- **Compressed archive support / Archivi compressi**: Direct import from ZIP and 7z files
- **Persistent indexing / Indicizzazione persistente**: Lucene index persists between sessions for instant search
- **Outlook-style UI / Interfaccia Outlook-style**: SplitPane layout with email list and viewer panels

---

## Tech Stack / Stack Tecnologico

| Component / Componente | Technology / Tecnologia |
|------------------------|-------------------------|
| Language / Linguaggio | Java 21 LTS |
| UI | JavaFX 21 |
| Framework | Spring Boot 3.2 |
| Search Engine / Motore di ricerca | Apache Lucene 9.10 |
| MIME Parsing | Jakarta Mail + Apache Mime4J |
| Database | SQLite (cache/config) |
| Compression / Compressione | Apache Commons Compress + SevenZipJBinding |
| PDF Export / Esportazione PDF | OpenPDF |
| Build Tool | Maven |

---

## Prerequisites / Requisiti

- **Java 21 JDK** (or later / o successivo) - with `jpackage` available (included in JDK 14+)
- **Maven 3.8+**
- **[WiX Toolset](https://wixtoolset.org/)** (optional, only for .msi/.exe installers on Windows)

### Install Java / Installa Java

**Windows:**
```powershell
# Download from https://adoptium.net/ or use:
winget install EclipseAdoptium.Temurin.21.JDK
```

**macOS:**
```bash
brew install openjdk@21
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt install openjdk-21-jdk
```

### Install Maven / Installa Maven

**Windows:**
```powershell
winget install Apache.Maven
```

**macOS:**
```bash
brew install maven
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt install maven
```

---

## Quick Start / Guida Rapida

### 1. Clone / Clona il progetto

```bash
git clone <repository-url>
cd mbv
```

### 2. Build / Compila

```bash
mvn clean package
```

### 3. Run / Esegui

**Option A - Using Maven (recommended / consigliato):**
```bash
mvn javafx:run
```

**Option B - Using JAR (after build / dopo la compilazione):**
```bash
# Windows
java -jar target\mail-backup-visualizer-1.0.0-SNAPSHOT.jar

# macOS / Linux
java -jar target/mail-backup-visualizer-1.0.0-SNAPSHOT.jar
```

**Option C - Windows desktop app (no CLI needed / senza riga di comando):**
```powershell
# Build app image with jpackage (from JDK 21)
cd mbv
mvn clean package -DskipTests
jpackage --type app-image --name "MBV" --app-version "1.0.0" --input target --main-jar mail-backup-visualizer-1.0.0-SNAPSHOT.jar --main-class org.springframework.boot.loader.launch.JarLauncher --dest dist --vendor "MBV" --description "Mail Backup Visualizer"
```
L'app image sarà in `dist\MBV\MBV.exe`. Puoi creare un collegamento sul desktop o aggiungerla al menu Start. Per creare un installer (.exe/.msi) su Windows serve [WiX Toolset](https://wixtoolset.org/).

### Alternative Run Methods / Metodi Alternativi di Esecuzione

**Using Maven Wrapper (if available / se disponibile):**
```bash
./mvnw javafx:run          # macOS / Linux
.\mvnw.cmd javafx:run      # Windows
```

**Direct Java execution:**
```bash
java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml -jar target/mail-backup-visualizer-1.0.0-SNAPSHOT.jar
```

---

## Project Structure / Struttura del Progetto

```
mbv/
├── pom.xml
├── LICENSE
├── README.md
├── src/main/java/com/mbv/
│   ├── MbvApplication.java          # Main entry point / Punto di ingresso principale
│   ├── config/
│   │   ├── AppConfig.java           # Spring configuration / Configurazione Spring
│   │   └── ThemeManager.java        # Theme management / Gestione temi
│   ├── model/
│   │   ├── MailMessage.java         # Email data model / Modello dati email
│   │   ├── MailAttachment.java      # Attachment model / Modello allegati
│   │   ├── MailThread.java          # Thread/conversation model / Modello thread
│   │   ├── MailFlags.java           # Email flags / Flag email
│   │   ├── Contact.java             # Contact / Contatto
│   │   ├── SearchQuery.java         # Search query / Query di ricerca
│   │   └── SearchResult.java        # Search result / Risultato ricerca
│   ├── parser/
│   │   ├── MailParser.java          # Parser interface / Interfaccia parser
│   │   ├── MboxParser.java          # MBOX format parser / Parser MBOX
│   │   ├── EmlParser.java           # EML format parser / Parser EML
│   │   └── ParserFactory.java       # Parser factory / Factory parser
│   ├── service/
│   │   ├── IndexingService.java     # Lucene indexing / Indicizzazione Lucene
│   │   ├── SearchService.java       # Search engine / Motore di ricerca
│   │   ├── CacheService.java        # SQLite cache / Cache SQLite
│   │   ├── ThreadBuilder.java       # Thread building / Costruzione thread
│   │   ├── ImportService.java       # Import service / Servizio importazione
│   │   └── ExportService.java       # Export service / Servizio esportazione
│   ├── ui/controller/
│   │   ├── MainController.java      # Main controller / Controller principale
│   │   ├── EmailListController.java # Email list / Lista email
│   │   ├── EmailViewerController.java # Email viewer / Visualizzatore email
│   │   ├── FilterBarController.java # Filters / Filtri
│   │   └── ThreadTimelineController.java # Thread timeline / Timeline thread
│   └── util/
│       ├── FileUtils.java           # File utilities / Utilità file
│       └── MimeUtils.java           # MIME utilities / Utilità MIME
├── src/main/resources/
│   ├── fxml/
│   │   ├── main.fxml               # Main layout / Layout principale
│   │   ├── email-list.fxml         # Email list / Lista email
│   │   ├── email-viewer.fxml       # Email viewer / Visualizzatore
│   │   ├── filter-bar.fxml         # Filters / Filtri
│   │   └── thread-timeline.fxml    # Thread timeline / Timeline thread
│   ├── css/
│   │   ├── theme-blue.css          # Blue theme / Tema blu
│   │   ├── theme-green.css         # Green theme / Tema verde
│   │   ├── theme-orange.css        # Orange theme / Tema arancione
│   │   ├── theme-dark-blue.css     # Dark blue / Blu notte
│   │   ├── theme-dark-green.css    # Dark green / Verde scuro
│   │   └── theme-dark-orange.css   # Dark orange / Arancione scuro
│   └── application.properties      # Config / Configurazione
└── src/test/java/com/mbv/         # Tests
```

---

## Search Syntax / Sintassi di Ricerca

MBV supports Google-like search syntax:

MBV supporta una sintassi di ricerca stile Google:

| Syntax | Description / Descrizione | Example / Esempio |
|--------|---------------------------|-------------------|
| `text` | Full-text search / Ricerca full-text | `spring boot mongodb` |
| `"phrase"` | Exact phrase / Frase esatta | `"NullPointerException"` |
| `from:` | Filter by sender / Filtra per mittente | `from:mario@gmail.com` |
| `to:` | Filter by recipient / Filtra per destinatario | `to:admin@company.it` |
| `subject:` | Search in subject / Cerca nell'oggetto | `subject:fattura` |
| `body:` | Search in body / Cerca nel corpo | `body:"spring security"` |
| `after:` | After date / Dopo la data | `after:2025-01-01` |
| `before:` | Before date / Prima della data | `before:2025-03-01` |
| `has:attachment` | Has attachments / Con allegati | `has:attachment` |
| `attachment:` | By attachment type / Per tipo allegato | `attachment:pdf` |

**Example / Esempio:**
```
from:mario@gmail.com subject:fattura after:2025-01-01 has:attachment
```

---

## Supported Formats / Formati Supportati

| Format | Description / Descrizione |
|--------|---------------------------|
| MBOX | Gmail Takeout, Thunderbird, Apple Mail |
| EML | Single email (RFC 822) / Singola email |

### Compressed Archives / Archivi Compressi

- ZIP
- 7z

---

## Troubleshooting / Risoluzione Problemi

### "Java not found" / "Java non trovato"

Ensure Java 21 is installed and in your PATH:
Assicurati che Java 21 sia installato e nel tuo PATH:

```bash
java -version
```

Should show: `openjdk version "21.x.x"`

### "Maven not found" / "Maven non trovato"

```bash
mvn -version
```

### Build fails / Compilazione fallisce

Clean and rebuild:
Pulisci e ricompila:

```bash
mvn clean install
```

### JavaFX module error / Errore modulo JavaFX

If you see module errors, ensure you're using Maven:
Se vedi errori di modulo, assicurati di usare Maven:

```bash
mvn javafx:run
```

---

## License / Licenza

MIT License - See [LICENSE](LICENSE) file for details.

Vedi il file [LICENSE](LICENSE) per i dettagli.
