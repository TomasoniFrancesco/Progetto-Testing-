package it.tvsw.smartparking.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Traduzione fedele, passo-passo, dello scenario Avalla {@code src/test_pieno.avalla}
 * in un test JUnit (Model Based Testing: lo scenario Avalla e' il "modello" da cui
 * deriva il test). Ogni blocco e' commentato con il riferimento alla riga dello
 * scenario originale.
 */
class ScenarioAvallaTest {

    @Test
    void scenarioTestPieno() {
        SmartParkingFSM fsm = new SmartParkingFSM(msg -> { });

        // riga 5-6: check stato = IDLE; check posti_std = 1;
        assertEquals(StatoSistema.IDLE, fsm.getStato());
        assertEquals(1, fsm.getPostiStd());

        // riga 9-11: set sens_in := true; step  -> check stato = CHK_IN;
        fsm.step(SensorInput.sensIn());
        assertEquals(StatoSistema.CHK_IN, fsm.getStato());

        // riga 13-16: set sens_in := false; set utente_rilevato := STD; step -> check stato = VER;
        fsm.step(SensorInput.utente(TipoUtente.STD));
        assertEquals(StatoSistema.VER, fsm.getStato());

        // riga 19-20: step (apre la sbarra) -> check stato = INGR;
        fsm.step(SensorInput.utente(TipoUtente.STD));
        assertEquals(StatoSistema.INGR, fsm.getStato());

        // riga 23-26: set transito_ok := true; step -> check stato = IDLE; check posti_std = 0;
        fsm.step(SensorInput.transito());
        assertEquals(StatoSistema.IDLE, fsm.getStato());
        assertEquals(0, fsm.getPostiStd());

        // riga 29-32: set transito_ok := false; set sens_in := true; step -> check stato = CHK_IN;
        fsm.step(SensorInput.sensIn());
        assertEquals(StatoSistema.CHK_IN, fsm.getStato());

        // riga 34-37: set sens_in := false; set utente_rilevato := STD; step -> check stato = VER;
        fsm.step(SensorInput.utente(TipoUtente.STD));
        assertEquals(StatoSistema.VER, fsm.getStato());

        // riga 40-41: step (posti_std = 0 -> nega accesso) -> check stato = NEG;
        fsm.step(SensorInput.utente(TipoUtente.STD));
        assertEquals(StatoSistema.NEG, fsm.getStato());

        // riga 44-47: set auto_via := true; step -> check stato = IDLE; check posti_std = 0;
        fsm.step(SensorInput.autoVia());
        assertEquals(StatoSistema.IDLE, fsm.getStato());
        assertEquals(0, fsm.getPostiStd());
    }
}
