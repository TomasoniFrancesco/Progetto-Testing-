package it.tvsw.smartparking.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test MC/DC (Modified Condition/Decision Coverage) sulla logica di
 * {@code r_gestione_VER}, implementata in {@link Parcheggio#assegnaPosto(TipoUtente)}.
 *
 * <p>Decisione 1 (ramo STD/ABBONATO): {@code D1 = (utente == STD || utente == ABBONATO) && postiStd > 0}
 * <br>Condizioni atomiche: A = (utente == STD), B = (utente == ABBONATO), C = (postiStd > 0)
 *
 * <p>Decisione 2 (ramo DISABILE, fallback): {@code D2 = postiDis > 0 ? POSTO_DIS : (postiStd > 0 ? POSTO_STD : NESSUNO)}
 * <br>Condizioni atomiche: C1 = (postiDis > 0), C2 = (postiStd > 0)
 *
 * <p>La tabella dei casi di test e delle coppie indipendenti e' documentata
 * in dettaglio in {@code docs/04_java_testing.md}.
 */
class McdcVerificaTest {

    // --- Decisione 1: (utente == STD || utente == ABBONATO) && postiStd > 0 ---

    @Test
    void tc1_std_conPostoLibero_assegnaStandard() {
        // A=T, B=F, C=T -> D1=T
        Parcheggio p = new Parcheggio(1, 1);
        assertEquals(TipoPosto.POSTO_STD, p.assegnaPosto(TipoUtente.STD));
        assertEquals(0, p.getPostiStd());
    }

    @Test
    void tc2_std_senzaPostoLibero_negato() {
        // A=T, B=F, C=F -> D1=F (isola C rispetto a tc1: stesso A,B, C cambia, esito cambia)
        Parcheggio p = new Parcheggio(0, 1);
        assertEquals(TipoPosto.NESSUNO, p.assegnaPosto(TipoUtente.STD));
    }

    @Test
    void tc3_disabile_conPostoDisLibero_nonPrendeStandard() {
        // A=F, B=F, C=T -> D1=F (isola A rispetto a tc1: A cambia T->F, B resta F, C resta T,
        // l'esito cambia da POSTO_STD a POSTO_DIS: il ramo STD/ABBONATO non e' stato preso)
        Parcheggio p = new Parcheggio(1, 1);
        assertEquals(TipoPosto.POSTO_DIS, p.assegnaPosto(TipoUtente.DISABILE));
        assertEquals(1, p.getPostiStd()); // il posto standard NON e' stato toccato
    }

    @Test
    void tc4_abbonato_conPostoLibero_assegnaStandard() {
        // A=F, B=T, C=T -> D1=T (isola B rispetto a tc3: B cambia F->T, A resta F, C resta T,
        // l'esito cambia da POSTO_DIS a POSTO_STD)
        Parcheggio p = new Parcheggio(1, 1);
        assertEquals(TipoPosto.POSTO_STD, p.assegnaPosto(TipoUtente.ABBONATO));
        assertEquals(0, p.getPostiStd());
    }

    // --- Decisione 2 (ramo disabile): C1 = postiDis > 0, C2 = postiStd > 0 ---

    @Test
    void tc5_disabile_postoDisLibero_prioritaSuPostoDis() {
        // C1=T -> D2 = POSTO_DIS (C2 irrilevante)
        Parcheggio p = new Parcheggio(1, 1);
        assertEquals(TipoPosto.POSTO_DIS, p.assegnaPosto(TipoUtente.DISABILE));
    }

    @Test
    void tc6_disabile_postoDisEsaurito_fallbackSuStandard() {
        // C1=F, C2=T -> D2 = POSTO_STD (isola C1 rispetto a tc5: C1 cambia T->F, esito cambia
        // da POSTO_DIS a POSTO_STD)
        Parcheggio p = new Parcheggio(1, 0);
        assertEquals(TipoPosto.POSTO_STD, p.assegnaPosto(TipoUtente.DISABILE));
    }

    @Test
    void tc7_disabile_entrambiEsauriti_negato() {
        // C1=F, C2=F -> D2 = NESSUNO (isola C2 rispetto a tc6: C2 cambia T->F, esito cambia
        // da POSTO_STD a NESSUNO)
        Parcheggio p = new Parcheggio(0, 0);
        assertEquals(TipoPosto.NESSUNO, p.assegnaPosto(TipoUtente.DISABILE));
    }
}
