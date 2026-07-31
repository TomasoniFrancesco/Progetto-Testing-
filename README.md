# SmartParking — Progetto TVSW

![CI](https://github.com/TomasoniFrancesco/Progetto-Testing-/actions/workflows/ci.yml/badge.svg)

Progetto per il corso di Testing e Verifica del Software:
sistema di gestione di un parcheggio intelligente (SmartParking), modellato in
Abstract State Machine (Asmeta), specificato con JML e implementato in Java con
interfaccia Vaadin.

**📄 La documentazione completa del progetto è il documento unico
[`docs/latex/main.pdf`](docs/latex/main.pdf) (33 pagine).**

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

