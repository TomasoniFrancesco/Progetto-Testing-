# 2. Modello Asmeta — SmartParking

## 2.1 Il modello (`asmeta/src/SmartParking.asm`)

### Domini

| Dominio | Valori | Significato |
|---------|--------|-------------|
| `StatoSistema` | `IDLE, CHK_IN, VER, NEG, INGR, CHK_OUT, TARIF, USC` | I 8 stati operativi della FSM. |
| `TipoUtente` | `STD, DISABILE, ABBONATO` | Categoria dell'utente rilevata. |
| `TipoPosto` | `POSTO_STD, POSTO_DIS, NESSUNO` | Tipo di posto assegnato/occupato. |

### Funzioni monitorate (input dall'ambiente)

| Funzione | Tipo | Significato |
|----------|------|-------------|
| `sens_in` | Boolean | Veicolo rilevato alla sbarra di ingresso. |
| `sens_out` | Boolean | Veicolo rilevato alla sbarra di uscita. |
| `transito_ok` | Boolean | Il veicolo è transitato fisicamente sotto la sbarra aperta. |
| `auto_via` | Boolean | Il veicolo respinto si è allontanato. |
| `utente_rilevato` | TipoUtente | Tipo di utente identificato dal totem. |
| `pagamento_ok` | Boolean | Esito del pagamento della tariffa. |

### Funzioni controllate (stato/memoria del sistema)

| Funzione | Tipo | Significato |
|----------|------|-------------|
| `stato` | StatoSistema | Stato corrente della FSM. |
| `posti_std` | Integer | Posti standard liberi (init = 1). |
| `posti_dis` | Integer | Posti disabili liberi (init = 1). |
| `posto_assegnato` | TipoPosto | Memoria del posto occupato dal veicolo in transito (init = NESSUNO). |

### Funzioni derivate

| Funzione | Tipo | Significato |
|----------|------|-------------|
| `assegnabile` | Prod(TipoUtente, TipoPosto) → Boolean | Funzione derivata **n-aria (binaria)**: vero se all'utente `$u` può essere assegnato il posto `$p`. Incapsula la politica di assegnazione: `POSTO_STD` è assegnabile a chiunque se `posti_std > 0`; `POSTO_DIS` è riservato ai DISABILI se `posti_dis > 0`. Essendo derivata, è calcolata a ogni stato a partire dalle funzioni controllate, senza occupare memoria di stato. |

### Regole principali

- `r_gestione_IDLE`: da IDLE, smista verso CHK_IN (ingresso) o CHK_OUT (uscita) in base ai sensori.
- `r_gestione_CHK_IN`: transizione automatica verso VER (fase di verifica).
- `r_gestione_VER`: la regola più complessa. Usa una regola **`let`** (`let ($u = utente_rilevato) in ... endlet`) e la derivata `assegnabile`: prova prima `assegnabile($u, POSTO_DIS)` (vero solo per DISABILE con posti disabili liberi), poi `assegnabile($u, POSTO_STD)` (caso STD/ABBONATO e fallback del disabile), altrimenti NEG. Il comportamento è identico alla versione con if annidati, ma la politica di assegnazione è centralizzata nella derivata.
- `r_gestione_NEG`: resta in NEG finché `auto_via` non diventa true, poi torna in IDLE.
- `r_gestione_INGR`: al transito, decrementa il contatore corrispondente al `posto_assegnato` e torna in IDLE.
- `r_gestione_CHK_OUT`: STD va in TARIF, DISABILE/ABBONATO vanno direttamente in USC.
- `r_gestione_TARIF`: resta in TARIF finché `pagamento_ok` non è true, poi passa a USC.
- `r_gestione_USC`: al transito, incrementa il contatore corrispondente, azzera `posto_assegnato` e torna in IDLE.

## 2.2 Scenari Avalla (`asmeta/src/*.avalla`)

| Scenario | Obiettivo | Note |
|----------|-----------|------|
| `test_abbonato.avalla` | Ciclo completo ABBONATO: ingresso senza pagamento, uscita diretta (salta TARIF), verifica decremento/incremento `posti_std`. | **Corretto in questo progetto**: l'init originale assumeva `posti_std = 5` e uno stato inesistente `PARK`; ora coerente con `posti_std = 1` e stato `IDLE` dopo il transito. |
| `test_disabile.avalla` | Ciclo completo DISABILE: assegnazione POSTO_DIS, uscita diretta (salta TARIF), verifica che `posto_assegnato` resti `POSTO_DIS` durante la sosta e venga azzerato solo in uscita. | Logicamente già corretto; rinforzato con check espliciti su `posti_dis`. |
| `test_pieno.avalla` | Un utente STD occupa l'unico posto standard; un secondo utente STD viene respinto (NEG) perché `posti_std = 0`; verifica il ritorno in IDLE dopo `auto_via`. | Già corretto, nessuna modifica necessaria. |
| `test_disabile_fallback.avalla` **(nuovo)** | Copre il ramo di fallback: primo DISABILE occupa l'unico posto disabile; secondo DISABILE, con `posti_dis = 0`, riceve `POSTO_STD`. | Copre la diramazione altrimenti non testata dagli scenari preesistenti. |
| `test_pagamento_std.avalla` **(nuovo)** | Ciclo di uscita STD completo: CHK_OUT → TARIF, un tentativo di pagamento fallito (resta in TARIF), poi pagamento riuscito → USC → IDLE con posto restituito. | Copre il self-loop di `r_gestione_TARIF`. |
| `test_pieno_totale.avalla` **(nuovo)** | Saturazione totale del parcheggio (1 STD + 1 DISABILE occupano entrambi i posti), poi un terzo DISABILE viene respinto (NEG) perché sia `posti_dis` che `posti_std` sono a 0. | Copre il ramo NEG raggiunto dal percorso di fallback esaurito, distinto dal NEG "semplice" di `test_pieno`. |
| `test_random_30steps.avalla` **(generato)** | 30 step random eseguiti nel simulatore Asmeta ed esportati con "export to avalla": ingressi DISABILE e STD fino a saturazione, doppio rifiuto (NEG), uscita STD con pagamento (TARIF), rientro. | Scenario **generato automaticamente** (random), a supporto del punto ATGT/random generator. Validato con coverage: **rule coverage 100%** (tutte e 9 le regole coperte), ma branch coverage incompleta (USC 33%, TARIF 50%, IDLE 75%): ad es. il ramo "pagamento fallito" è coperto solo dallo scenario manuale `test_pagamento_std`. Un primo tentativo con 15 step copriva solo 6/9 regole (NEG, TARIF, USC scoperte): la copertura random cresce lentamente rispetto agli scenari mirati. |

## 2.3 Model checking (`asmeta/src/SmartParking_MC.asm`)

| # | CTLSPEC | Categoria | Esito atteso |
|---|---------|-----------|---------------|
| 1 | `ag(posti_std >= 0)` | Safety | TRUE |
| 2 | `ag(posti_dis >= 0)` | Safety | TRUE |
| 3 | `ef(stato = NEG)` | Raggiungibilità | TRUE |
| 4 | `ag((stato = INGR and posto_assegnato = POSTO_STD) implies posti_std > 0)` | Safety | TRUE |
| 5 | `ag(posti_std <= 1)` | Safety (limite superiore) | TRUE |
| 6 | `ag(posti_dis <= 1)` | Safety (limite superiore) | TRUE |
| 7 | `ag((stato = INGR and posto_assegnato = POSTO_DIS) implies posti_dis > 0)` | Safety | TRUE |
| 8 | `ef(stato = TARIF)` | Raggiungibilità | TRUE |
| 9 | `ef(stato = USC)` | Raggiungibilità | TRUE |
| 10 | `ag(stato = TARIF implies ef(stato = IDLE))` | Liveness | TRUE |

Il modello mantiene volutamente capacità ridotta (`posti_std = posti_dis = 1`) per limitare
lo spazio degli stati esplorato da NuSMV ed evitare l'esplosione di stati; nessun nuovo
dominio è stato introdotto rispetto al modello originale.

Nota: `SmartParking_MC.asm` mantiene volutamente la versione "appiattita" di
`r_gestione_VER` (if annidati, senza derivate né `let`), per massimizzare la
compatibilità con la traduzione AsmetaSMV → NuSMV. Il comportamento è identico a
quello del modello principale.

Inoltre, nel modello MC i contatori `posti_std`/`posti_dis` sono dichiarati su un
**dominio concreto finito** `Posti = {0..2}` invece che su `Integer`: AsmetaSMV
rifiuta i domini infiniti ("Error Domain Integer not supported"), perché NuSMV deve
enumerare lo spazio degli stati. La scelta di `{0..2}` (anziché `{0..1}`) rende le
proprietà `ag(posti_std <= 1)` / `ag(posti_dis <= 1)` verifiche autentiche: il tipo
ammetterebbe il valore 2, ed è il model checking a dimostrare che non è mai raggiunto.

## 2.4 Istruzioni operative (Eclipse - Asmeta)

> 📷 **Inserire screenshot** per ciascuno dei passaggi seguenti, salvandoli in `docs/screenshots/`.

### a) Simulazione con il simulatore Asmeta

1. Aprire il progetto in Eclipse con il plugin Asmeta installato.
2. Aprire `asmeta/src/SmartParking.asm`.
3. Click destro sul file → **Run As → ASM Simulation** (o dal menu Asmeta → Simulator).
4. Eseguire alcuni step manuali impostando i valori delle funzioni monitorate dalla vista del simulatore e osservare l'evoluzione di `stato`, `posti_std`, `posti_dis`.
5. 📷 Screenshot della vista simulatore con lo stato dopo un paio di step (es. dopo un ingresso STD).

### b) Animazione degli scenari Avalla

1. Click destro su uno dei file `.avalla` (es. `test_abbonato.avalla`) → **Run As → Avalla Animation** (o **ASM Scenario Execution**, a seconda della versione del plugin).
2. Verificare che tutti i `check` risultino verdi (PASSED).
3. Ripetere per tutti i 6 scenari: `test_abbonato`, `test_disabile`, `test_pieno`, `test_disabile_fallback`, `test_pagamento_std`, `test_pieno_totale`.
4. 📷 Screenshot dell'esito per almeno 2-3 scenari rappresentativi (uno corretto/uno nuovo).

### c) AsmetaV — esecuzione con copertura

1. Click destro sul progetto (o sui file `.avalla`) → **Run As → AsmetaV** (Coverage/Validation).
2. Selezionare come criterio di copertura la copertura delle regole/transizioni (Vc / Rule coverage) e includere tutti gli scenari `.avalla` presenti in `asmeta/src/`.
3. Osservare il report di copertura generato: percentuale di regole/transizioni coperte.
4. 📷 Screenshot del report di copertura AsmetaV.

### d) AsmetaSMV — model checking

1. Aprire `asmeta/src/SmartParking_MC.asm`.
2. Click destro → **Run As → AsmetaSMV** (o **Model Checking**).
3. Il tool traduce il modello ASM in NuSMV ed esegue le 10 CTLSPEC definite.
4. Verificare che tutte le proprietà risultino `TRUE` (nessun controesempio).
5. 📷 Screenshot dell'output di AsmetaSMV con l'esito delle CTLSPEC.

### e) Model Advisor (opzionale, +1 punto)

1. Click destro su `asmeta/src/SmartParking.asm` → **Run As → Model Advisor** (o dal menu Asmeta).
2. Lanciare l'analisi e osservare i suggerimenti/warning prodotti (es. su completezza delle regole, funzioni non usate, ecc.).
3. 📷 Screenshot dell'output del Model Advisor.
4. Commentare nel documento (a cura dell'utente) se i suggerimenti sono pertinenti o falsi positivi, motivando eventuali modifiche non apportate.

### f) ATGT (opzionale, +1 punto)

1. Click destro su `asmeta/src/SmartParking.asm` → **Run As → ATGT** (Asmeta Test Generation Tool).
2. Configurare un criterio di generazione random/strutturale e generare un set di scenari.
3. Ispezionare gli scenari `.avalla` generati automaticamente: confrontarli con quelli scritti a mano (coprono altri casi limite? sono ridondanti?).
4. 📷 Screenshot dell'esecuzione di ATGT e di uno scenario generato.
5. Commentare brevemente (a cura dell'utente) l'utilità dei casi generati rispetto a quelli manuali.
