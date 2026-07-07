package it.tvsw.smartparking.core;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Combinatorial testing (pairwise) su {@link Parcheggio#assegnaPosto(TipoUtente)}.
 *
 * <p>Parametri: tipoUtente {STD, DISABILE, ABBONATO} (3 valori);
 * postiStd {0, &gt;0} rappresentato come {0, 1} (2 valori);
 * postiDis {0, &gt;0} rappresentato come {0, 1} (2 valori).
 *
 * <p>La tabella pairwise a 6 righe (che copre tutte le coppie di valori dei
 * 3 parametri) e' documentata in {@code docs/04_java_testing.md}.
 */
class CombinatorialTest {

    @ParameterizedTest(name = "[{index}] {0}, postiStd={1}, postiDis={2}")
    @CsvSource({
            // utente,    postiStd, postiDis, postoAtteso, postiStdAtteso, postiDisAtteso
            "STD,      0, 0, NESSUNO,   0, 0",
            "STD,      1, 1, POSTO_STD, 0, 1",
            "DISABILE, 0, 1, POSTO_DIS, 0, 0",
            "DISABILE, 1, 0, POSTO_STD, 0, 0",
            "ABBONATO, 0, 1, NESSUNO,   0, 1",
            "ABBONATO, 1, 0, POSTO_STD, 0, 0"
    })
    void tabellaPairwise(TipoUtente utente, int postiStdIniziali, int postiDisIniziali,
                          TipoPosto postoAtteso, int postiStdAttesi, int postiDisAttesi) {
        Parcheggio p = new Parcheggio(postiStdIniziali, postiDisIniziali);
        TipoPosto risultato = p.assegnaPosto(utente);
        assertEquals(postoAtteso, risultato);
        assertEquals(postiStdAttesi, p.getPostiStd());
        assertEquals(postiDisAttesi, p.getPostiDis());
    }
}
