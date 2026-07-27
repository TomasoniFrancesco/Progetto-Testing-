# SmartParking — Progetto TVSW

![CI](https://github.com/TomasoniFrancesco/Progetto-Testing-/actions/workflows/ci.yml/badge.svg)

Progetto individuale per il corso di Testing e Verifica del Software (TVSW):
sistema di gestione di un parcheggio intelligente (SmartParking), modellato in
Abstract State Machine (Asmeta), specificato con JML e implementato in Java con
interfaccia Vaadin.

**📄 La documentazione completa del progetto è il documento unico
[`docs/latex/main.pdf`](docs/latex/main.pdf) (26 pagine).**

## Descrizione

SmartParking gestisce l'ingresso e l'uscita di veicoli da un parcheggio con posti
standard e posti disabili, riconoscendo tre tipologie di utente (STD, DISABILE,
ABBONATO) e applicando pagamento solo per gli utenti STD in uscita. Il
comportamento è specificato formalmente come macchina a stati ASM (8 stati),
validato con scenari Avalla e model checking CTL, e poi implementato in Java con
contratti JML sul nucleo di dominio.

## Struttura del repository

```
Progetto-Testing-/
├── asmeta/
│   └── src/                    # Modello Asmeta (.asm) e scenari (.avalla)
│       └── abstractests/       # Scenari generati automaticamente da ATGT
├── docs/
│   └── latex/                  # Documentazione unica (main.pdf + sorgenti .tex)
│       └── images/             # Screenshot da Eclipse-Asmeta / OpenJML / UI / CI
├── java/                       # Progetto Maven (core, FSM, UI Vaadin, test)
│   ├── pom.xml
│   └── src/main/java, src/test/java
├── .github/workflows/ci.yml    # Pipeline CI (build + test + JaCoCo)
└── README.md
```

## Mappa punti → dove trovarli

La trattazione completa di ogni punto è nel documento `docs/latex/main.pdf`; qui sotto
solo dove trovare i file sorgente corrispondenti.

| Sezione / punto | Peso | Dove |
|---|---|---|
| 1. Requisiti | 4 | `docs/latex/main.pdf` (Sez. 1) |
| 2.1 Correzione bug `test_abbonato.avalla` | incluso in 2 | `asmeta/src/test_abbonato.avalla` |
| 2.2 Verifica coerenza `test_disabile`/`test_pieno` | incluso in 2 | `asmeta/src/test_disabile.avalla`, `asmeta/src/test_pieno.avalla` |
| 2.3 Nuovi scenari Avalla | 3 | `asmeta/src/test_disabile_fallback.avalla`, `asmeta/src/test_pagamento_std.avalla`, `asmeta/src/test_pieno_totale.avalla` |
| 2.4 Model checking (CTLSPEC) | 4 | `asmeta/src/SmartParking_MC.asm` |
| 2.5 Model Advisor / ATGT | 1 + 1 | `docs/latex/main.pdf` (Sez. 2), `asmeta/src/abstractests/` |
| 3. JML | 4 | `java/src/main/java/it/tvsw/smartparking/core/Parcheggio.java`, `main.pdf` (Sez. 5) |
| 4.1 Progetto Maven | incluso in 12 | `java/pom.xml` |
| 4.2 Implementazione core/FSM | incluso in 12 | `java/src/main/java/it/tvsw/smartparking/core/*` |
| 4.3 Testing (JUnit, MCDC, coverage) | 5 | `java/src/test/java/it/tvsw/smartparking/core/*Test.java`, `main.pdf` (Sez. 4) |
| 4.4 Mockito | 1 | `java/src/test/java/it/tvsw/smartparking/core/DisplayMockTest.java` |
| 4.5 Model Based Testing | 1 | `java/src/test/java/it/tvsw/smartparking/core/ScenarioAvallaTest.java` |
| 4.6 Combinatorial testing | 2 | `java/src/test/java/it/tvsw/smartparking/core/CombinatorialTest.java` |
| 4.7 UI Vaadin + Selenium | 3 | `java/src/main/java/it/tvsw/smartparking/ui/MainView.java`, `.../ui/UISeleniumTest.java` |
| 4.8 CI | 1 | `.github/workflows/ci.yml` |
| 4.9 Analisi statica / ispezione | 1 | `docs/latex/main.pdf` (Sez. 6) |

## Come eseguire i tool Asmeta (in Eclipse)

Le istruzioni operative passo-passo (simulatore, animazione degli scenari `.avalla`,
AsmetaV con copertura, AsmetaSMV per il model checking, Model Advisor e ATGT) sono nella
Sezione 2 di `docs/latex/main.pdf`.

## Come buildare/testare il progetto Java

```bash
cd java
mvn verify
```

Report di copertura JaCoCo: `java/target/site/jacoco/index.html`
(59 test del core + 1 test Selenium; core al 98% instruction / 95%+ branch coverage,
`Parcheggio` 100%/100%).

### Mutation testing (PIT)

```bash
cd java
mvn org.pitest:pitest-maven:mutationCoverage
```

Report: `java/target/pit-reports/index.html`

### Analisi statica (SpotBugs)

```bash
cd java
mvn spotbugs:check
```

## Come lanciare la UI Vaadin

```bash
cd java
mvn spring-boot:run
```

Poi aprire <http://localhost:8080>.

## Verifica JML con OpenJML

Vedi le istruzioni dettagliate nella Sezione 5 di `docs/latex/main.pdf` (comando
`--esc` e note per macOS aarch64).

## Continuous Integration

Il workflow gira a ogni push e pull request: build Maven completa (esclusi i test
taggati `ui`, che richiedono Chrome) e upload del report JaCoCo come artifact.
Il branch [`ci_failure`](https://github.com/TomasoniFrancesco/Progetto-Testing-/tree/ci_failure)
è mantenuto apposta come dimostrazione di una pipeline che fallisce (Sezione 7 della
documentazione).
