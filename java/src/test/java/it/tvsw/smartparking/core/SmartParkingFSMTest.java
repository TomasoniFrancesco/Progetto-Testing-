package it.tvsw.smartparking.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Copre tutti gli 8 stati e tutte le transizioni della FSM, incluso il
 * fallback disabile e il parcheggio pieno.
 */
class SmartParkingFSMTest {

    @Test
    void statoInizialeEIdleConPostiPieni() {
        SmartParkingFSM fsm = new SmartParkingFSM(msg -> { });
        assertEquals(StatoSistema.IDLE, fsm.getStato());
        assertEquals(1, fsm.getPostiStd());
        assertEquals(1, fsm.getPostiDis());
        assertEquals(TipoPosto.NESSUNO, fsm.getPostoAssegnato());
    }

    @Test
    void stepConInputNullLanciaEccezione() {
        SmartParkingFSM fsm = new SmartParkingFSM(msg -> { });
        assertThrows(IllegalArgumentException.class, () -> fsm.step(null));
    }

    @Test
    void costruttoreConParcheggioNullLanciaEccezione() {
        assertThrows(IllegalArgumentException.class, () -> new SmartParkingFSM(null, msg -> { }));
    }

    @Test
    void ingressoStdCompletoDecrementaPostiStd() {
        SmartParkingFSM fsm = new SmartParkingFSM(msg -> { });

        fsm.step(SensorInput.sensIn());
        assertEquals(StatoSistema.CHK_IN, fsm.getStato());

        fsm.step(SensorInput.utente(TipoUtente.STD));
        assertEquals(StatoSistema.VER, fsm.getStato());

        fsm.step(SensorInput.utente(TipoUtente.STD));
        assertEquals(StatoSistema.INGR, fsm.getStato());
        assertEquals(TipoPosto.POSTO_STD, fsm.getPostoAssegnato());

        fsm.step(SensorInput.transito());
        assertEquals(StatoSistema.IDLE, fsm.getStato());
        assertEquals(0, fsm.getPostiStd());
    }

    @Test
    void ingressoDisabileAssegnaPostoDisabile() {
        SmartParkingFSM fsm = new SmartParkingFSM(msg -> { });

        fsm.step(SensorInput.sensIn());
        fsm.step(SensorInput.utente(TipoUtente.DISABILE));
        fsm.step(SensorInput.utente(TipoUtente.DISABILE));

        assertEquals(StatoSistema.INGR, fsm.getStato());
        assertEquals(TipoPosto.POSTO_DIS, fsm.getPostoAssegnato());

        fsm.step(SensorInput.transito());
        assertEquals(StatoSistema.IDLE, fsm.getStato());
        assertEquals(0, fsm.getPostiDis());
        assertEquals(1, fsm.getPostiStd());
    }

    @Test
    void fallbackDisabileSuPostoStandardQuandoPostiDisEsauriti() {
        SmartParkingFSM fsm = new SmartParkingFSM(new Parcheggio(1, 0), msg -> { });

        fsm.step(SensorInput.sensIn());
        fsm.step(SensorInput.utente(TipoUtente.DISABILE));
        fsm.step(SensorInput.utente(TipoUtente.DISABILE));

        assertEquals(StatoSistema.INGR, fsm.getStato());
        assertEquals(TipoPosto.POSTO_STD, fsm.getPostoAssegnato()); // fallback

        fsm.step(SensorInput.transito());
        assertEquals(StatoSistema.IDLE, fsm.getStato());
        assertEquals(0, fsm.getPostiStd());
        assertEquals(0, fsm.getPostiDis());
    }

    @Test
    void accessoNegatoQuandoParcheggioPienoEAutoSiAllontana() {
        SmartParkingFSM fsm = new SmartParkingFSM(new Parcheggio(0, 0), msg -> { });

        fsm.step(SensorInput.sensIn());
        fsm.step(SensorInput.utente(TipoUtente.STD));
        fsm.step(SensorInput.utente(TipoUtente.STD));

        assertEquals(StatoSistema.NEG, fsm.getStato());
        assertEquals(TipoPosto.NESSUNO, fsm.getPostoAssegnato());

        fsm.step(SensorInput.autoVia());
        assertEquals(StatoSistema.IDLE, fsm.getStato());
        assertEquals(0, fsm.getPostiStd());
    }

    @Test
    void negRestaInNegSeAutoNonSiAllontanaAncora() {
        SmartParkingFSM fsm = new SmartParkingFSM(new Parcheggio(0, 0), msg -> { });
        fsm.step(SensorInput.sensIn());
        fsm.step(SensorInput.utente(TipoUtente.STD));
        fsm.step(SensorInput.utente(TipoUtente.STD));
        assertEquals(StatoSistema.NEG, fsm.getStato());

        fsm.step(SensorInput.nessunEvento());
        assertEquals(StatoSistema.NEG, fsm.getStato()); // resta in NEG
    }

    @Test
    void disabileRifiutatoQuandoParcheggioCompletamentePieno() {
        SmartParkingFSM fsm = new SmartParkingFSM(new Parcheggio(0, 0), msg -> { });
        fsm.step(SensorInput.sensIn());
        fsm.step(SensorInput.utente(TipoUtente.DISABILE));
        fsm.step(SensorInput.utente(TipoUtente.DISABILE));
        assertEquals(StatoSistema.NEG, fsm.getStato());
    }

    @Test
    void uscitaStdPassaPerTarifEAttendePagamento() {
        // per avere posto_assegnato = POSTO_STD facciamo prima entrare l'auto STD
        SmartParkingFSM fsm = new SmartParkingFSM(msg -> { });
        fsm.step(SensorInput.sensIn());
        fsm.step(SensorInput.utente(TipoUtente.STD));
        fsm.step(SensorInput.utente(TipoUtente.STD));
        fsm.step(SensorInput.transito());
        assertEquals(StatoSistema.IDLE, fsm.getStato());
        assertEquals(0, fsm.getPostiStd());

        fsm.step(SensorInput.sensOut());
        assertEquals(StatoSistema.CHK_OUT, fsm.getStato());

        fsm.step(SensorInput.utente(TipoUtente.STD));
        assertEquals(StatoSistema.TARIF, fsm.getStato());

        fsm.step(SensorInput.pagamento(false));
        assertEquals(StatoSistema.TARIF, fsm.getStato()); // resta in attesa

        fsm.step(SensorInput.pagamento(true));
        assertEquals(StatoSistema.USC, fsm.getStato());

        fsm.step(SensorInput.transito());
        assertEquals(StatoSistema.IDLE, fsm.getStato());
        assertEquals(1, fsm.getPostiStd());
        assertEquals(TipoPosto.NESSUNO, fsm.getPostoAssegnato());
    }

    @Test
    void uscitaAbbonatoSaltaTarif() {
        SmartParkingFSM fsm = new SmartParkingFSM(msg -> { });
        fsm.step(SensorInput.sensIn());
        fsm.step(SensorInput.utente(TipoUtente.ABBONATO));
        fsm.step(SensorInput.utente(TipoUtente.ABBONATO));
        fsm.step(SensorInput.transito());
        assertEquals(0, fsm.getPostiStd());

        fsm.step(SensorInput.sensOut());
        fsm.step(SensorInput.utente(TipoUtente.ABBONATO));
        assertEquals(StatoSistema.USC, fsm.getStato()); // salta TARIF

        fsm.step(SensorInput.transito());
        assertEquals(StatoSistema.IDLE, fsm.getStato());
        assertEquals(1, fsm.getPostiStd());
    }

    @Test
    void uscitaDisabileSaltaTarifERestituiscePostoDisabile() {
        SmartParkingFSM fsm = new SmartParkingFSM(msg -> { });
        fsm.step(SensorInput.sensIn());
        fsm.step(SensorInput.utente(TipoUtente.DISABILE));
        fsm.step(SensorInput.utente(TipoUtente.DISABILE));
        fsm.step(SensorInput.transito());
        assertEquals(0, fsm.getPostiDis());

        fsm.step(SensorInput.sensOut());
        fsm.step(SensorInput.utente(TipoUtente.DISABILE));
        assertEquals(StatoSistema.USC, fsm.getStato());

        fsm.step(SensorInput.transito());
        assertEquals(StatoSistema.IDLE, fsm.getStato());
        assertEquals(1, fsm.getPostiDis());
        assertEquals(TipoPosto.NESSUNO, fsm.getPostoAssegnato());
    }

    @Test
    void idleIgnoraStepSenzaSensoriAttivi() {
        SmartParkingFSM fsm = new SmartParkingFSM(msg -> { });
        fsm.step(SensorInput.nessunEvento());
        assertEquals(StatoSistema.IDLE, fsm.getStato());
    }

    @Test
    void ingrRestaInIngrFinoAlTransito() {
        SmartParkingFSM fsm = new SmartParkingFSM(msg -> { });
        fsm.step(SensorInput.sensIn());
        fsm.step(SensorInput.utente(TipoUtente.STD));
        fsm.step(SensorInput.utente(TipoUtente.STD));
        assertEquals(StatoSistema.INGR, fsm.getStato());

        fsm.step(SensorInput.nessunEvento());
        assertEquals(StatoSistema.INGR, fsm.getStato()); // resta finche' non transita
    }

    @Test
    void uscRestaInUscFinoAlTransito() {
        // Copre il self-loop di r_gestione_USC (transito_ok = false),
        // analogo a quelli gia' testati per NEG/INGR/TARIF.
        SmartParkingFSM fsm = new SmartParkingFSM(msg -> { });
        fsm.step(SensorInput.sensIn());
        fsm.step(SensorInput.utente(TipoUtente.ABBONATO));
        fsm.step(SensorInput.utente(TipoUtente.ABBONATO)); // VER -> INGR
        fsm.step(SensorInput.transito());                  // INGR -> IDLE
        fsm.step(SensorInput.sensOut());
        fsm.step(SensorInput.utente(TipoUtente.ABBONATO)); // CHK_OUT -> USC (no TARIF)
        assertEquals(StatoSistema.USC, fsm.getStato());

        fsm.step(SensorInput.nessunEvento());
        assertEquals(StatoSistema.USC, fsm.getStato()); // resta finche' non transita
        assertEquals(0, fsm.getPostiStd());             // posto non ancora rilasciato

        fsm.step(SensorInput.transito());
        assertEquals(StatoSistema.IDLE, fsm.getStato());
        assertEquals(1, fsm.getPostiStd());
    }

    @Test
    void fsmFunzionaSenzaDisplay() {
        // Copre il ramo display == null di cambiaStato: la FSM deve funzionare
        // anche senza un display collegato (display opzionale).
        SmartParkingFSM fsm = new SmartParkingFSM((Display) null);
        fsm.step(SensorInput.sensIn());
        assertEquals(StatoSistema.CHK_IN, fsm.getStato());
        fsm.step(SensorInput.utente(TipoUtente.STD));
        assertEquals(StatoSistema.VER, fsm.getStato());
    }
}
