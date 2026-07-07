package it.tvsw.smartparking.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test unitari della classe {@link Parcheggio}: costruttori, assegnazione,
 * rilascio e violazione delle precondizioni (corrispondenti alle clausole
 * {@code requires} JML documentate in docs/03_jml.md).
 */
class ParcheggioTest {

    @Test
    void costruttoreDiDefaultInizializzaTuttiIPostiLiberi() {
        Parcheggio p = new Parcheggio();
        assertEquals(Parcheggio.MAX_STD, p.getPostiStd());
        assertEquals(Parcheggio.MAX_DIS, p.getPostiDis());
    }

    @Test
    void costruttoreConValoriIniziali() {
        Parcheggio p = new Parcheggio(0, 1);
        assertEquals(0, p.getPostiStd());
        assertEquals(1, p.getPostiDis());
    }

    @Test
    void costruttorePostiStdFuoriRangeLanciaEccezione() {
        assertThrows(IllegalArgumentException.class, () -> new Parcheggio(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Parcheggio(Parcheggio.MAX_STD + 1, 0));
    }

    @Test
    void costruttorePostiDisFuoriRangeLanciaEccezione() {
        assertThrows(IllegalArgumentException.class, () -> new Parcheggio(0, -1));
        assertThrows(IllegalArgumentException.class, () -> new Parcheggio(0, Parcheggio.MAX_DIS + 1));
    }

    @Test
    void assegnaPostoUtenteNullLanciaEccezione() {
        Parcheggio p = new Parcheggio();
        assertThrows(IllegalArgumentException.class, () -> p.assegnaPosto(null));
    }

    @Test
    void stdRicevePostoStandardSeDisponibile() {
        Parcheggio p = new Parcheggio(1, 1);
        TipoPosto risultato = p.assegnaPosto(TipoUtente.STD);
        assertEquals(TipoPosto.POSTO_STD, risultato);
        assertEquals(0, p.getPostiStd());
        assertEquals(1, p.getPostiDis());
    }

    @Test
    void stdVieneRifiutatoSePostiStdEsauriti() {
        Parcheggio p = new Parcheggio(0, 1);
        TipoPosto risultato = p.assegnaPosto(TipoUtente.STD);
        assertEquals(TipoPosto.NESSUNO, risultato);
        assertEquals(0, p.getPostiStd());
        assertEquals(1, p.getPostiDis());
    }

    @Test
    void abbonatoRicevePostoStandardSeDisponibile() {
        Parcheggio p = new Parcheggio(1, 0);
        TipoPosto risultato = p.assegnaPosto(TipoUtente.ABBONATO);
        assertEquals(TipoPosto.POSTO_STD, risultato);
        assertEquals(0, p.getPostiStd());
    }

    @Test
    void disabileRicevePostoDisabileSeDisponibile() {
        Parcheggio p = new Parcheggio(1, 1);
        TipoPosto risultato = p.assegnaPosto(TipoUtente.DISABILE);
        assertEquals(TipoPosto.POSTO_DIS, risultato);
        assertEquals(1, p.getPostiStd());
        assertEquals(0, p.getPostiDis());
    }

    @Test
    void disabileFallbackSuPostoStandardSePostiDisEsauriti() {
        Parcheggio p = new Parcheggio(1, 0);
        TipoPosto risultato = p.assegnaPosto(TipoUtente.DISABILE);
        assertEquals(TipoPosto.POSTO_STD, risultato);
        assertEquals(0, p.getPostiStd());
        assertEquals(0, p.getPostiDis());
    }

    @Test
    void disabileRifiutatoSeEntrambiIPostiEsauriti() {
        Parcheggio p = new Parcheggio(0, 0);
        TipoPosto risultato = p.assegnaPosto(TipoUtente.DISABILE);
        assertEquals(TipoPosto.NESSUNO, risultato);
        assertEquals(0, p.getPostiStd());
        assertEquals(0, p.getPostiDis());
    }

    @Test
    void liberaPostoStandardIncrementaContatore() {
        Parcheggio p = new Parcheggio(0, 1);
        p.liberaPosto(TipoPosto.POSTO_STD);
        assertEquals(1, p.getPostiStd());
    }

    @Test
    void liberaPostoDisabileIncrementaContatore() {
        Parcheggio p = new Parcheggio(1, 0);
        p.liberaPosto(TipoPosto.POSTO_DIS);
        assertEquals(1, p.getPostiDis());
    }

    @Test
    void liberaPostoNessunoNonModificaContatori() {
        Parcheggio p = new Parcheggio(0, 0);
        p.liberaPosto(TipoPosto.NESSUNO);
        assertEquals(0, p.getPostiStd());
        assertEquals(0, p.getPostiDis());
    }

    @Test
    void liberaPostoStandardGiaAlMassimoLanciaEccezione() {
        Parcheggio p = new Parcheggio(); // gia' al massimo
        assertThrows(IllegalStateException.class, () -> p.liberaPosto(TipoPosto.POSTO_STD));
    }

    @Test
    void liberaPostoDisabileGiaAlMassimoLanciaEccezione() {
        Parcheggio p = new Parcheggio(); // gia' al massimo
        assertThrows(IllegalStateException.class, () -> p.liberaPosto(TipoPosto.POSTO_DIS));
    }

    @Test
    void liberaPostoNullLanciaEccezione() {
        Parcheggio p = new Parcheggio(0, 0);
        assertThrows(IllegalArgumentException.class, () -> p.liberaPosto(null));
    }

    /**
     * Test parametrico su assegnaPosto: combinazioni (tipoUtente, postiStd, postiDis) -> esito atteso.
     * Colonne: utente, postiStd, postiDis, postoAtteso, postiStdAtteso, postiDisAtteso
     */
    @ParameterizedTest(name = "{0} con postiStd={1} postiDis={2} -> {3}")
    @CsvSource({
            "STD,      1, 1, POSTO_STD, 0, 1",
            "STD,      0, 1, NESSUNO,   0, 1",
            "ABBONATO, 1, 0, POSTO_STD, 0, 0",
            "ABBONATO, 0, 0, NESSUNO,   0, 0",
            "DISABILE, 1, 1, POSTO_DIS, 1, 0",
            "DISABILE, 1, 0, POSTO_STD, 0, 0",
            "DISABILE, 0, 0, NESSUNO,   0, 0",
            "DISABILE, 0, 1, POSTO_DIS, 0, 0"
    })
    void assegnaPostoParametrico(TipoUtente utente, int postiStdIniziali, int postiDisIniziali,
                                  TipoPosto postoAtteso, int postiStdAttesi, int postiDisAttesi) {
        Parcheggio p = new Parcheggio(postiStdIniziali, postiDisIniziali);
        TipoPosto risultato = p.assegnaPosto(utente);
        assertEquals(postoAtteso, risultato);
        assertEquals(postiStdAttesi, p.getPostiStd());
        assertEquals(postiDisAttesi, p.getPostiDis());
    }
}
