package it.tvsw.smartparking.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Verifica, tramite un mock Mockito di {@link Display}, che la FSM notifichi
 * i messaggi corretti nei cambi di stato significativi.
 */
@ExtendWith(MockitoExtension.class)
class DisplayMockTest {

    @Mock
    private Display display;

    @Test
    void notificaAccessoNegatoQuandoParcheggioPieno() {
        SmartParkingFSM fsm = new SmartParkingFSM(new Parcheggio(0, 0), display);

        fsm.step(SensorInput.sensIn());
        fsm.step(SensorInput.utente(TipoUtente.STD));
        fsm.step(SensorInput.utente(TipoUtente.STD));

        verify(display).mostra("Veicolo rilevato in ingresso");
        verify(display).mostra("Verifica utente in corso");
        verify(display).mostra("Accesso negato");
        verifyNoMoreInteractions(display);
    }

    @Test
    void notificaSbarraApertaQuandoPostoDisponibile() {
        SmartParkingFSM fsm = new SmartParkingFSM(display);

        fsm.step(SensorInput.sensIn());
        fsm.step(SensorInput.utente(TipoUtente.STD));
        fsm.step(SensorInput.utente(TipoUtente.STD));

        verify(display).mostra("Sbarra aperta");
    }

    @Test
    void notificaInAttesaDiPagamentoPerUtenteStd() {
        SmartParkingFSM fsm = new SmartParkingFSM(display);
        fsm.step(SensorInput.sensIn());
        fsm.step(SensorInput.utente(TipoUtente.STD));
        fsm.step(SensorInput.utente(TipoUtente.STD));
        fsm.step(SensorInput.transito());

        fsm.step(SensorInput.sensOut());
        fsm.step(SensorInput.utente(TipoUtente.STD));

        verify(display).mostra("In attesa di pagamento");
    }

    @Test
    void nonNotificaNulaSeStepNonProvocaTransizione() {
        SmartParkingFSM fsm = new SmartParkingFSM(display);
        fsm.step(SensorInput.nessunEvento()); // in IDLE senza sensori attivi: nessuna transizione
        verifyNoMoreInteractions(display);
    }
}
