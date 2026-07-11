# 3. JML — Contratti e verifica statica (ESC)

## 3.1 Dove si trovano i contratti

I contratti JML sono nei commenti `//@ ...` della classe
`java/src/main/java/it/tvsw/smartparking/core/Parcheggio.java`, l'unica classe del
progetto annotata con JML (come richiesto: il resto del sistema — FSM, UI — è Java
normale senza JML). La classe è stata tenuta volutamente piccola e senza dipendenze
esterne, con soli tipi primitivi (`int`) ed enum, nessuno stream/lambda, per essere
digeribile da OpenJML in modalità ESC (Extended Static Checking).

## 3.2 Spiegazione dei contratti

**Invarianti di classe:**

```
//@ public invariant 0 <= postiStd && postiStd <= MAX_STD;
//@ public invariant 0 <= postiDis && postiDis <= MAX_DIS;
```

I contatori dei posti liberi non devono mai uscire dall'intervallo `[0, MAX]`: è la
traduzione a livello Java della proprietà di safety `ag(posti_std >= 0)` /
`ag(posti_std <= 1)` già verificata a livello di modello ASM (vedi `docs/02_asmeta.md`).

**Costruttori:** garantiscono (`ensures`) che lo stato iniziale rispetti l'invariante;
il costruttore con parametri ha `requires` che vieta di costruire un parcheggio già
fuori dai limiti (violazione testata in `ParcheggioTest` con `assertThrows`).

**`assegnaPosto(TipoUtente utente)`:** precondizione `utente != null`. Le
postcondizioni (`ensures`) descrivono tutti e tre i possibili esiti — assegnazione
standard diretta, assegnazione disabile, fallback disabile→standard, o rifiuto — in
funzione del valore *precedente* dei contatori (`\old(...)`), analogamente
all'esempio fornito nella consegna:
`ensures \result == POSTO_STD ==> postiStd == \old(postiStd) - 1`.

**`liberaPosto(TipoPosto posto)`:** precondizione che il contatore corrispondente non
sia già al massimo (altrimenti si violerebbe l'invariante superiore); postcondizione
che il contatore corrispondente sia incrementato di 1 e l'altro resti invariato.

**Metodi `pure`:** i getter sono annotati `/*@ pure @*/` perché non hanno effetti
collaterali, condizione necessaria per poterli usare dentro le clausole JML stesse.

## 3.3 Verifica ESC con OpenJML

### Download

1. Scaricare da <https://github.com/OpenJML/OpenJML/releases> lo zip
   **`openjml-macos-arm64-<versione>.zip`** (dalla release 21.0.27 esiste la build
   nativa per Apple Silicon: niente Rosetta). Esempio:
   ```
   mkdir -p ~/tools && cd ~/tools
   curl -LO https://github.com/OpenJML/OpenJML/releases/download/21.0.27/openjml-macos-arm64-21.0.27.zip
   unzip openjml-macos-arm64-21.0.27.zip -d openjml
   ```
2. Rimuovere la quarantena macOS e verificare l'installazione:
   ```
   xattr -dr com.apple.quarantine ~/tools/openjml
   ~/tools/openjml/openjml --version
   ```

### Comando di verifica ESC

Dalla root del progetto (`$OJ` = cartella di installazione, es. `~/tools/openjml`):

```
$OJ/openjml --esc \
  -cp java/src/main/java \
  java/src/main/java/it/tvsw/smartparking/core/Parcheggio.java \
  java/src/main/java/it/tvsw/smartparking/core/TipoUtente.java \
  java/src/main/java/it/tvsw/smartparking/core/TipoPosto.java
```

Nota: i campi `postiStd`/`postiDis` sono dichiarati `private /*@ spec_public @*/`,
necessario perché invarianti e postcondizioni **public** possono riferire solo
elementi con visibilità di specifica public (senza `spec_public` OpenJML segnala
un errore di visibilità JML).

Esito atteso: OpenJML analizza ogni metodo di `Parcheggio` e, per ciascuna clausola
`requires`/`ensures`/`invariant`, tenta di dimostrare che il codice la rispetta in
ogni possibile esecuzione. Se tutte le prove hanno successo, il tool non riporta
errori (output senza "Warning" o "cannot be proved"); altrimenti indica la riga e la
clausola non dimostrabile, utile per capire se manca una precondizione o se il
codice ha un bug.

### Nota sull'esecuzione in questo ambiente

Non è stato possibile eseguire realmente OpenJML in questo sandbox: l'ambiente di
lavoro automatizzato non ha una JDK completa installata (solo una JRE 11, priva di
`javac`) né accesso di rete a Maven Central o ai file di release GitHub necessari
per scaricare OpenJML/JDK 17 (il proxy di rete blocca questi host). Il codice e i
contratti sono stati quindi scritti e rivisti manualmente con la massima cura, ma la
verifica ESC effettiva va eseguita dall'utente con i comandi sopra, sul proprio Mac
(cartella `eclipse_asmeta_smv_2024_06_macOs_aarch64` già presente nel percorso di
lavoro suggerisce un ambiente macOS aarch64 con i tool già pensati per quella
piattaforma).

> 📷 **Inserire screenshot** dell'output del comando `openjml --esc` (o `sh openjml
> --esc`) eseguito sulla classe `Parcheggio`, con l'esito (nessun errore, oppure gli
> eventuali warning e la correzione applicata di conseguenza).
