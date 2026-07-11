# 5. Analisi statica e ispezione critica (LLM)

La consegna indica esplicitamente l'ispezione "con chatgpt o LLM in generale" come
via privilegiata per questo punto: l'analisi è stata quindi svolta come **code
review critica condotta da un LLM** (Claude), che ha letto riga per riga il codice
del progetto. Il plugin SpotBugs resta configurato nel `pom.xml` come strumento
complementare facoltativo, ma non fa parte del perimetro documentato.

## 5.2 Ispezione critica del codice core (analisi LLM)

Di seguito un'ispezione critica del package `it.tvsw.smartparking.core`, eseguita
leggendo riga per riga `Parcheggio.java` e `SmartParkingFSM.java` con l'obiettivo di
individuare naming poco chiaro, casi limite non gestiti, possibili NPE e altre
osservazioni tipiche di una code review.

### Osservazione 1 — Validazione mancante di `utenteRilevato == null` in `CHK_OUT`

In `SmartParkingFSM.gestisciChkOut`, il confronto `input.getUtenteRilevato() ==
TipoUtente.STD` restituisce silenziosamente `false` se `utenteRilevato` è `null`
(cioè se chi pilota la FSM dimentica di impostare il tipo utente in uscita), portando
il sistema nello stato USC come se l'utente non fosse STD, invece di segnalare un
errore di configurazione.

**Come l'ho valutata**: ho scelto di lasciare il comportamento invariato perché
riflette fedelmente l'ASM (che non modella esplicitamente l'assenza di dato — un
`utente_rilevato` è sempre uno dei tre valori del dominio) e perché aggiungere un
controllo esplicito complicherebbe la classe senza un requisito che lo richieda.
Il rischio è mitigato dal fatto che tutti i punti di chiamata del progetto (FSM
test, UI, scenario Avalla) impostano sempre esplicitamente l'utente prima di uno
step in CHK_OUT/VER. Documentato qui per trasparenza.

### Osservazione 2 — `TipoPosto.NESSUNO` con doppio significato

Il valore `NESSUNO` è usato sia come "nessun posto assegnato/disponibile" (esito di
`assegnaPosto`) sia come valore neutro per `liberaPosto` ("non liberare nulla"). Pur
essendo coerente con l'ASM originale (stesso enum `TipoPosto` con lo stesso valore
`NESSUNO` usato nei due contesti), il doppio uso potrebbe generare ambiguità per chi
legge il codice per la prima volta.

**Come l'ho risolta**: ho aggiunto Javadoc esplicito su `liberaPosto` che chiarisce
che `NESSUNO` è un no-op, invece di introdurre un tipo/enum separato che avrebbe
appesantito inutilmente una classe che deve restare semplice.

### Osservazione 3 — Possibile eccezione non gestita in `liberaPosto`

`liberaPosto(POSTO_STD)` lancia `IllegalStateException` se `postiStd` è già al
massimo. Se la FSM avesse un bug e chiamasse `liberaPosto` con un `postoAssegnato`
non coerente con lo stato reale dei contatori (es. per un errore di sincronizzazione
introdotto in futuro), l'applicazione andrebbe in crash invece di degradare in modo
più controllato.

**Come l'ho valutata**: si tratta di una scelta di design deliberata ("fail fast" su
violazione di invariante): è preferibile un crash immediato e diagnosticabile a un
contatore silenziosamente inconsistente (`posti_std > MAX_STD`), che violerebbe la
proprietà di safety RNF1. I test (`ParcheggioTest.liberaPostoStandardGiaAlMassimoLanciaEccezione`)
verificano esplicitamente questo comportamento.

### Osservazione 4 — Scostamento temporale rispetto al modello ASM nel decremento dei contatori

Come documentato nel Javadoc di `Parcheggio`, l'implementazione Java decrementa il
contatore dei posti nello stesso passo in cui decide l'assegnazione (`assegnaPosto`,
chiamato da `gestisciVer`), mentre l'ASM lo fa un passo dopo, al transito effettivo
(`r_gestione_INGR`). Chi confronta una traccia di esecuzione Java con una traccia
Avalla passo-passo potrebbe notare che `posti_std`/`posti_dis` cambiano "un passo
prima" nella versione Java durante la permanenza nello stato INGR (prima del
transito).

**Come l'ho risolta**: la differenza non è osservabile in nessuno degli scenari di
test del progetto (nessun controllo intermedio tra l'ingresso in INGR e il
transito), quindi ho preferito la versione più semplice (un solo metodo che decide
e decrementa) piuttosto che introdurre un meccanismo di "prenotazione" a due fasi,
in linea con l'indicazione di mantenere il codice semplice. La scelta è documentata
esplicitamente nel Javadoc della classe per evitare fraintendimenti futuri.

### Osservazione 5 — Assenza di thread-safety

Né `Parcheggio` né `SmartParkingFSM` sono thread-safe (nessuna sincronizzazione sui
campi mutabili). Per un singolo varco fisico pilotato da un'unica FSM sequenziale
(come nel modello ASM, che è intrinsecamente single-threaded) questo non è un
problema; lo diventerebbe se in futuro si volesse gestire più varchi concorrenti
condividendo la stessa istanza di `Parcheggio`.

**Come l'ho valutata**: non ho aggiunto sincronizzazione perché fuori scopo per
questo progetto (un solo ingresso/uscita, come da requisiti) e perché
appesantirebbe il codice senza un caso d'uso reale. Annotato qui come limite noto
per una eventuale estensione futura.

### Osservazione 6 — Memoria a slot singolo per `posto_assegnato` (difetto presente ANCHE nel modello ASM)

È l'osservazione più interessante emersa dall'ispezione, perché riguarda sia
l'implementazione sia il modello. Sia la FSM Java (`postoAssegnato`) sia l'ASM
(`posto_assegnato`) memorizzano **un solo** posto assegnato. Ma il parcheggio può
contenere **due veicoli contemporaneamente** (1 posto standard + 1 disabile):
se entra un utente STD (slot = `POSTO_STD`) e poi entra un DISABILE (slot
sovrascritto a `POSTO_DIS`), quando il *primo* veicolo esce il sistema rilascia il
posto sbagliato (`POSTO_DIS` invece di `POSTO_STD`), corrompendo i contatori.

**Come l'ho valutata**: il difetto è *ereditato fedelmente dal modello ASM*, che ha
la stessa variabile singola — quindi l'implementazione è corretta rispetto alla
specifica; è la specifica a modellare implicitamente "un solo veicolo in transito
per volta tra ingresso e uscita" senza tracciare l'associazione veicolo→posto.
Nessuno scenario Avalla né test JUnit del progetto viola questa assunzione (gli
scenari con due veicoli dentro li fanno uscire in ordine LIFO o non fanno uscire
il primo). Correggerlo richiederebbe una mappa veicolo→posto in entrambi i
livelli (modello e Java): documentato come **limite noto della specifica**, non
dell'implementazione. È un buon esempio di difetto che il testing di conformità
non può trovare (il codice è conforme al modello) e che emerge solo da
un'ispezione critica.

### Osservazione 7 — Bottoni UI sempre attivi, mitigati dai self-loop della FSM

In `MainView` i bottoni non vengono mai disabilitati in base allo stato: l'utente
può cliccare "Pagamento" in IDLE o "Transito" in TARIF. Gli step "spuri" che ne
derivano risultano però innocui: i self-loop della FSM (gli `if` senza `else`
segnalati dal Model Advisor come MP2, qui deliberati) li assorbono senza cambiare
stato — l'osservazione conferma a posteriori la bontà di quella scelta difensiva.
Inoltre `simulaArrivo` esegue tre step in sequenza assumendo di partire da IDLE:
se il sistema è in NEG/TARIF gli step extra sono anch'essi assorbiti.

**Come l'ho valutata**: per un prototipo dimostrativo la semplicità è preferibile
alla gestione fine dell'abilitazione dei bottoni; il comportamento resta corretto
grazie alla robustezza della FSM. In un'applicazione reale andrebbe aggiunta la
disabilitazione contestuale dei comandi (facile: `setEnabled` in
`aggiornaEtichette` in base a `fsm.getStato()`).
