package it.tvsw.smartparking.core;

/**
 * Raggruppa i 6 input monitorati dal modello ASM ({@code sens_in}, {@code sens_out},
 * {@code transito_ok}, {@code auto_via}, {@code utente_rilevato}, {@code pagamento_ok})
 * usati da {@link SmartParkingFSM#step(SensorInput)}. Immutabile.
 */
public final class SensorInput {

    private final boolean sensIn;
    private final boolean sensOut;
    private final boolean transitoOk;
    private final boolean autoVia;
    private final TipoUtente utenteRilevato;
    private final boolean pagamentoOk;

    public SensorInput(boolean sensIn, boolean sensOut, boolean transitoOk,
                        boolean autoVia, TipoUtente utenteRilevato, boolean pagamentoOk) {
        this.sensIn = sensIn;
        this.sensOut = sensOut;
        this.transitoOk = transitoOk;
        this.autoVia = autoVia;
        this.utenteRilevato = utenteRilevato;
        this.pagamentoOk = pagamentoOk;
    }

    /** Nessun sensore attivo (tutti false, utente nullo). */
    public static SensorInput nessunEvento() {
        return new SensorInput(false, false, false, false, null, false);
    }

    /** Auto rilevata in ingresso. */
    public static SensorInput sensIn() {
        return new SensorInput(true, false, false, false, null, false);
    }

    /** Auto rilevata in uscita. */
    public static SensorInput sensOut() {
        return new SensorInput(false, true, false, false, null, false);
    }

    /** Identificazione del tipo di utente (usato in VER e in CHK_OUT). */
    public static SensorInput utente(TipoUtente tipoUtente) {
        return new SensorInput(false, false, false, false, tipoUtente, false);
    }

    /** Il veicolo transita fisicamente sotto la sbarra aperta. */
    public static SensorInput transito() {
        return new SensorInput(false, false, true, false, null, false);
    }

    /** Il veicolo respinto si allontana dalla sbarra. */
    public static SensorInput autoVia() {
        return new SensorInput(false, false, false, true, null, false);
    }

    /** Esito del pagamento della tariffa. */
    public static SensorInput pagamento(boolean esito) {
        return new SensorInput(false, false, false, false, null, esito);
    }

    public boolean isSensIn() {
        return sensIn;
    }

    public boolean isSensOut() {
        return sensOut;
    }

    public boolean isTransitoOk() {
        return transitoOk;
    }

    public boolean isAutoVia() {
        return autoVia;
    }

    public TipoUtente getUtenteRilevato() {
        return utenteRilevato;
    }

    public boolean isPagamentoOk() {
        return pagamentoOk;
    }
}
