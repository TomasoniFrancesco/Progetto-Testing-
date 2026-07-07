package it.tvsw.smartparking.core;

/**
 * Macchina a stati che replica la main rule {@code r_Main} del modello ASM
 * ({@code src/SmartParking.asm}): ad ogni chiamata di {@link #step(SensorInput)}
 * viene eseguita la regola di gestione corrispondente allo stato corrente,
 * esattamente come nel modello (un solo ramo dello switch e' attivo per step,
 * come nell'{@code if stato = X then ... endif} dentro il {@code par} dell'ASM).
 */
public class SmartParkingFSM {

    private StatoSistema stato;
    private TipoPosto postoAssegnato;
    private final Parcheggio parcheggio;
    private final Display display;

    public SmartParkingFSM(Display display) {
        this(new Parcheggio(), display);
    }

    public SmartParkingFSM(Parcheggio parcheggio, Display display) {
        if (parcheggio == null) {
            throw new IllegalArgumentException("parcheggio non puo' essere null");
        }
        this.parcheggio = parcheggio;
        this.display = display;
        this.stato = StatoSistema.IDLE;
        this.postoAssegnato = TipoPosto.NESSUNO;
    }

    public StatoSistema getStato() {
        return stato;
    }

    public TipoPosto getPostoAssegnato() {
        return postoAssegnato;
    }

    public int getPostiStd() {
        return parcheggio.getPostiStd();
    }

    public int getPostiDis() {
        return parcheggio.getPostiDis();
    }

    /**
     * Esegue un passo della macchina a stati con gli input monitorati correnti.
     *
     * @param input i 6 input monitorati per questo step
     */
    public void step(SensorInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input non puo' essere null");
        }
        switch (stato) {
            case IDLE:
                gestisciIdle(input);
                break;
            case CHK_IN:
                gestisciChkIn();
                break;
            case VER:
                gestisciVer(input);
                break;
            case NEG:
                gestisciNeg(input);
                break;
            case INGR:
                gestisciIngr(input);
                break;
            case CHK_OUT:
                gestisciChkOut(input);
                break;
            case TARIF:
                gestisciTarif(input);
                break;
            case USC:
                gestisciUsc(input);
                break;
            default:
                throw new IllegalStateException("Stato non gestito: " + stato);
        }
    }

    private void cambiaStato(StatoSistema nuovoStato, String messaggio) {
        this.stato = nuovoStato;
        if (display != null) {
            display.mostra(messaggio);
        }
    }

    // r_gestione_IDLE
    private void gestisciIdle(SensorInput input) {
        if (input.isSensIn()) {
            cambiaStato(StatoSistema.CHK_IN, "Veicolo rilevato in ingresso");
        } else if (input.isSensOut()) {
            cambiaStato(StatoSistema.CHK_OUT, "Veicolo rilevato in uscita");
        }
    }

    // r_gestione_CHK_IN
    private void gestisciChkIn() {
        cambiaStato(StatoSistema.VER, "Verifica utente in corso");
    }

    // r_gestione_VER
    private void gestisciVer(SensorInput input) {
        TipoPosto posto = parcheggio.assegnaPosto(input.getUtenteRilevato());
        if (posto != TipoPosto.NESSUNO) {
            postoAssegnato = posto;
            cambiaStato(StatoSistema.INGR, "Sbarra aperta");
        } else {
            cambiaStato(StatoSistema.NEG, "Accesso negato");
        }
    }

    // r_gestione_NEG
    private void gestisciNeg(SensorInput input) {
        if (input.isAutoVia()) {
            cambiaStato(StatoSistema.IDLE, "Sistema pronto");
        }
    }

    // r_gestione_INGR (il decremento e' gia' avvenuto in VER, vedi Parcheggio)
    private void gestisciIngr(SensorInput input) {
        if (input.isTransitoOk()) {
            cambiaStato(StatoSistema.IDLE, "Sistema pronto");
        }
    }

    // r_gestione_CHK_OUT
    private void gestisciChkOut(SensorInput input) {
        if (input.getUtenteRilevato() == TipoUtente.STD) {
            cambiaStato(StatoSistema.TARIF, "In attesa di pagamento");
        } else {
            cambiaStato(StatoSistema.USC, "Sbarra di uscita aperta");
        }
    }

    // r_gestione_TARIF
    private void gestisciTarif(SensorInput input) {
        if (input.isPagamentoOk()) {
            cambiaStato(StatoSistema.USC, "Sbarra di uscita aperta");
        }
    }

    // r_gestione_USC
    private void gestisciUsc(SensorInput input) {
        if (input.isTransitoOk()) {
            parcheggio.liberaPosto(postoAssegnato);
            postoAssegnato = TipoPosto.NESSUNO;
            cambiaStato(StatoSistema.IDLE, "Sistema pronto");
        }
    }
}
