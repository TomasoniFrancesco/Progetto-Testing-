# Documentazione LaTeX

Documento unico (in stile "relazione", come l'esempio del collega) che copre tutte
le sezioni del progetto: requisiti, Asmeta, JML/OpenJML, Java/UI, testing
(JUnit/JaCoCo/PIT/MCDC/combinatorial), analisi statica, CI.

## Come compilare

```
cd docs/latex
pdflatex main.tex
pdflatex main.tex   # una seconda volta, per indice e riferimenti incrociati
```

Il PDF risultante è `main.pdf`. Il documento **compila correttamente anche senza
nessuno screenshot**: dove manca un'immagine viene mostrato un riquadro
segnaposto rosso con il nome del file atteso e la descrizione di cosa serve
mostrare (vedi il comando `\screenshot{...}{...}{...}` in `main.tex`).

## Come aggiungere gli screenshot

1. Fare lo screenshot richiesto (vedi le didascalie/riquadri nel PDF o il riepilogo
   fornito in chat).
2. Salvarlo come **PNG** dentro `docs/latex/images/`, con **esattamente** il nome
   indicato nel riquadro rosso (es. `asmeta_simulator_init.png`).
3. Ricompilare (`pdflatex main.tex` x2): il riquadro rosso viene sostituito
   automaticamente dall'immagine.

## Struttura

```
docs/latex/
├── main.tex              # documento principale + comando \screenshot
├── sections/
│   ├── 01_requisiti.tex
│   ├── 02_asmeta.tex
│   ├── 03_jml.tex
│   ├── 04_java.tex
│   ├── 05_testing.tex
│   ├── 06_analisi_statica.tex
│   ├── 07_ci.tex
│   └── 08_conclusioni.tex
└── images/                # gli screenshot vanno qui (nomi esatti richiesti)
```

Nota: se il tuo TeX Live/MacTeX non ha il pacchetto lingua italiana per babel,
`main.tex` degrada automaticamente senza errori (usa la sillabazione di default).
Su MacTeX completo non è un problema: il pacchetto è incluso.
