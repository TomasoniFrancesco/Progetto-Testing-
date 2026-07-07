package it.tvsw.smartparking.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import it.tvsw.smartparking.core.Display;
import it.tvsw.smartparking.core.SensorInput;
import it.tvsw.smartparking.core.SmartParkingFSM;
import it.tvsw.smartparking.core.TipoUtente;

/**
 * Vista Vaadin unica e minimale: mostra lo stato corrente della FSM e i posti
 * liberi, con bottoni che simulano i sensori del modello ASM. Nessun CSS
 * custom, nessun layout complesso.
 */
@Route("")
public class MainView extends VerticalLayout {

    private final SmartParkingFSM fsm;
    private TipoUtente ultimoUtente = TipoUtente.STD;

    private final Span statoLabel = new Span();
    private final Span postiStdLabel = new Span();
    private final Span postiDisLabel = new Span();
    private final Span messaggioLabel = new Span();

    public MainView() {
        Display display = messaggioLabel::setText;
        this.fsm = new SmartParkingFSM(display);

        add(new H1("SmartParking - Simulatore"));
        add(statoLabel, postiStdLabel, postiDisLabel, messaggioLabel);

        Button arrivaStd = new Button("Arriva auto STD", e -> simulaArrivo(TipoUtente.STD));
        Button arrivaDisabile = new Button("Arriva auto DISABILE", e -> simulaArrivo(TipoUtente.DISABILE));
        Button arrivaAbbonato = new Button("Arriva auto ABBONATO", e -> simulaArrivo(TipoUtente.ABBONATO));
        Button transito = new Button("Transito", e -> {
            fsm.step(SensorInput.transito());
            aggiornaEtichette();
        });
        Button pagamento = new Button("Pagamento", e -> {
            fsm.step(SensorInput.pagamento(true));
            aggiornaEtichette();
        });
        Button autoVia = new Button("Auto va via", e -> {
            fsm.step(SensorInput.autoVia());
            aggiornaEtichette();
        });
        Button autoInUscita = new Button("Auto in uscita", e -> simulaUscita());

        add(new HorizontalLayout(arrivaStd, arrivaDisabile, arrivaAbbonato));
        add(new HorizontalLayout(transito, pagamento, autoVia, autoInUscita));

        aggiornaEtichette();
    }

    /**
     * Simula l'arrivo di un'auto in ingresso: attiva il sensore di ingresso,
     * poi fornisce il tipo di utente rilevato (necessario per il passaggio
     * automatico CHK_IN -> VER -> INGR/NEG).
     */
    private void simulaArrivo(TipoUtente utente) {
        this.ultimoUtente = utente;
        fsm.step(SensorInput.sensIn());
        fsm.step(SensorInput.utente(utente));
        fsm.step(SensorInput.utente(utente));
        aggiornaEtichette();
    }

    /**
     * Simula la richiesta di uscita: attiva il sensore di uscita, poi rifornisce
     * l'ultimo tipo di utente rilevato in ingresso (necessario per la decisione
     * CHK_OUT -> TARIF/USC).
     */
    private void simulaUscita() {
        fsm.step(SensorInput.sensOut());
        fsm.step(SensorInput.utente(ultimoUtente));
        aggiornaEtichette();
    }

    private void aggiornaEtichette() {
        statoLabel.setText("Stato: " + fsm.getStato());
        postiStdLabel.setText("Posti standard liberi: " + fsm.getPostiStd());
        postiDisLabel.setText("Posti disabili liberi: " + fsm.getPostiDis());
    }
}
