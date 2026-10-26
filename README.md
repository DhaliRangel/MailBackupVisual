# Mail Backup Visual

Desktop email archive explorer with Apache Lucene-powered full-text search.

Esploratore desktop di archivi email con ricerca full-text potenziata da Apache Lucene.

## Come eseguire / Pacchettizzazione Windows

- **Sviluppo**: `mvn javafx:run` (dalla cartella `mbv/`)
- **JAR**: `mvn clean package -DskipTests && java -jar mbv\target\mail-backup-visualizer-1.0.0-SNAPSHOT.jar`
- **App Windows (senza CLI)**: con JDK 21 e `jpackage` generare `mbv\dist\MBV\MBV.exe`. Può essere avviato direttamente, aggiunto al desktop/Start Menu. Per installer `.msi/.exe` serve [WiX Toolset](https://wixtoolset.org/).

## Struttura progetto
Il codice sorgente è in `mbv/` (vedi `mbv/README.md` per dettagli completi).

## Note sui dati
L'app salva indice Lucene in `mbv-index/` e cache SQLite in `mbv-cache.db` nella directory di lavoro corrente. Per una distribuzione standalone è consigliabile mantenere questi percorsi relativi o configurabili.

## Licenza
MIT License - Copyright (c) 2026 Dhali Rangel. Vedi [LICENSE](LICENSE).
