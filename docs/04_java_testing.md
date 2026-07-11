# 4. Java — Testing

## 4.1 Struttura dei test

| Classe di test | Copre |
|---|---|
| `ParcheggioTest` | `Parcheggio`: costruttori, precondizioni violate (`assertThrows`), tutti i rami di `assegnaPosto`/`liberaPosto`, test parametrico CSV. |
| `SmartParkingFSMTest` | `SmartParkingFSM`: tutti gli 8 stati e le transizioni, fallback disabile, parcheggio pieno, self-loop NEG/INGR/TARIF. |
| `McdcVerificaTest` | Copertura MC/DC delle due decisioni composte di `assegnaPosto` (vedi §4.2). |
| `DisplayMockTest` | Notifiche a `Display` con Mockito (`verify`). |
| `ScenarioAvallaTest` | Traduzione fedele di `asmeta/src/test_pieno.avalla` in JUnit. |
| `CombinatorialTest` | Tabella pairwise a 6 righe su (tipoUtente, postiStd, postiDis) (vedi §4.3). |
| `UISeleniumTest` (tag `ui`) | Smoke test della UI Vaadin, escluso dalla build standard. |

## 4.2 MC/DC — tabella dei casi di test

**Decisione 1** (ramo STD/ABBONATO in `r_gestione_VER`):
`D1 = (utente == STD || utente == ABBONATO) && postiStd > 0`, condizioni atomiche
A = `utente==STD`, B = `utente==ABBONATO`, C = `postiStd>0`.

| Caso | A | B | C | D1 | Esito | Coppia indipendente |
|------|---|---|---|----|-------|----------------------|
| TC1 | T | F | T | T | POSTO_STD | base |
| TC2 | T | F | F | F | NESSUNO | isola **C** (vs TC1: A,B fissi, C cambia, esito cambia) |
| TC3 | F | F | T | F* | POSTO_DIS (altro ramo) | isola **A** (vs TC1: B,C fissi, A cambia, esito cambia) |
| TC4 | F | T | T | T | POSTO_STD | isola **B** (vs TC3: A,C fissi, B cambia, esito cambia) |

\* con A=F, B=F la decisione D1 non è nemmeno valutata nel codice (si entra nel ramo
disabile): l'osservabile "non è stato assegnato un posto standard via questo ramo" è
la prova che D1 è falsa.

**Decisione 2** (ramo DISABILE, fallback): condizioni atomiche C1 = `postiDis>0`,
C2 = `postiStd>0` (valutata solo se C1 falsa).

| Caso | C1 | C2 | Esito | Coppia indipendente |
|------|----|----|-------|----------------------|
| TC5 | T | — | POSTO_DIS | base |
| TC6 | F | T | POSTO_STD | isola **C1** (vs TC5: esito cambia) |
| TC7 | F | F | NESSUNO | isola **C2** (vs TC6: C1 fisso, C2 cambia, esito cambia) |

I 7 casi sono implementati in `McdcVerificaTest`.

## 4.3 Combinatorial testing — tabella pairwise

Parametri: `tipoUtente` ∈ {STD, DISABILE, ABBONATO}, `postiStd` ∈ {0, &gt;0}, `postiDis`
∈ {0, &gt;0} (rappresentati come 0/1 dato `MAX=1`). Tabella pairwise a 6 righe che
copre tutte le coppie di valori dei 3 parametri:

| # | tipoUtente | postiStd | postiDis | Esito atteso |
|---|-----------|----------|----------|---------------|
| 1 | STD | 0 | 0 | NESSUNO |
| 2 | STD | 1 | 1 | POSTO_STD |
| 3 | DISABILE | 0 | 1 | POSTO_DIS |
| 4 | DISABILE | 1 | 0 | POSTO_STD (fallback) |
| 5 | ABBONATO | 0 | 1 | NESSUNO |
| 6 | ABBONATO | 1 | 0 | POSTO_STD |

Implementata in `CombinatorialTest` con `@ParameterizedTest` + `@CsvSource`.

## 4.4 Esecuzione e copertura (risultati reali)

Build eseguita con `mvn verify`: **59 test, 0 falliti** (JUnit 5, incluse le classi
parametriche, MC/DC, Mockito, combinatorial e MBT).

Copertura JaCoCo sul package `core` (UI Vaadin e classe di avvio Spring Boot escluse
dal report, vedi commento nel `pom.xml`):

- **Statement coverage core: 98%** — `Parcheggio` **100%/100%**, enum e `SensorInput` 100%
- **Branch coverage core: 95%+** — `SmartParkingFSM` 96%/96%

Alla prima misurazione risultavano 3 branch scoperti nella FSM: il self-loop di
`gestisciUsc` (transito falso in USC), il ramo `display == null` di `cambiaStato` e
il `default` dello switch di `step()`. I primi due sono stati chiusi con due test
mirati (`uscRestaInUscFinoAlTransito`, `fsmFunzionaSenzaDisplay`); il terzo è
codice difensivo **strutturalmente irraggiungibile** (lo switch copre tutti e 8 i
valori dell'enum) e resta l'unico branch non coperto del package.

Il mutation testing (PIT, configurato nel `pom.xml`) non è stato incluso nel
perimetro del progetto (punto opzionale non svolto).

## 4.5 UI Vaadin

Per lanciare la UI:

```
cd java
mvn spring-boot:run
```

Poi aprire <http://localhost:8080> nel browser. La vista mostra lo stato corrente e
i posti liberi, con i bottoni "Arriva auto STD/DISABILE/ABBONATO", "Transito",
"Pagamento", "Auto va via", "Auto in uscita" per simulare i sensori.

> 📷 **Inserire screenshot** della UI in esecuzione nel browser, con alcuni stati
> diversi mostrati (es. dopo un ingresso STD e dopo un accesso negato).

Per verificare solo che il modulo UI compili (senza eseguire i test):

```
mvn -f java/pom.xml package -DskipTests
```

### Test Selenium (opzionale)

`UISeleniumTest` (tag `ui`) è escluso dalla build standard. Per eseguirlo: avviare
l'app con `mvn spring-boot:run`, avere Chrome/ChromeDriver disponibili, poi:

```
mvn -f java/pom.xml test -Dgroups=ui
```
