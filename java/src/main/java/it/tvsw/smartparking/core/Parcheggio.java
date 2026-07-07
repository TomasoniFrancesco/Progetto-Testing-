package it.tvsw.smartparking.core;

/**
 * Nucleo puro del dominio SmartParking: gestisce il conteggio dei posti liberi
 * standard e disabili e la logica di assegnazione/rilascio, corrispondente alla
 * logica della regola {@code r_gestione_VER} del modello ASM (per l'assegnazione)
 * e delle regole {@code r_gestione_INGR} / {@code r_gestione_USC} (per il
 * decremento/incremento dei contatori).
 *
 * <p>Classe senza dipendenze esterne, con contratti JML pensati per essere
 * verificati staticamente (ESC) con OpenJML: vedi {@code docs/03_jml.md}.</p>
 *
 * <p>Semplificazione rispetto al modello ASM: qui l'assegnazione del posto
 * ({@link #assegnaPosto(TipoUtente)}) decide E decrementa il contatore in un
 * unico passo atomico, mentre nell'ASM la decisione avviene in VER e il
 * decremento solo al transito effettivo in INGR. La differenza non è
 * osservabile dagli scenari di test del progetto (nessun controllo intermedio
 * tra i due passi) ed è stata scelta per mantenere la classe più semplice.</p>
 */
public class Parcheggio {

    /** Capacita' massima dei posti standard (allineata all'init dell'ASM: posti_std = 1). */
    public static final int MAX_STD = 1;

    /** Capacita' massima dei posti disabili (allineata all'init dell'ASM: posti_dis = 1). */
    public static final int MAX_DIS = 1;

    //@ public invariant 0 <= postiStd && postiStd <= MAX_STD;
    private int postiStd;

    //@ public invariant 0 <= postiDis && postiDis <= MAX_DIS;
    private int postiDis;

    /**
     * Crea un parcheggio con tutti i posti liberi (stato iniziale del modello ASM).
     */
    //@ ensures postiStd == MAX_STD && postiDis == MAX_DIS;
    public Parcheggio() {
        this(MAX_STD, MAX_DIS);
    }

    /**
     * Crea un parcheggio con un numero di posti liberi arbitrario (usato nei test,
     * ad esempio per il testing combinatorio e MC/DC).
     *
     * @param postiStdIniziali posti standard liberi iniziali, 0..MAX_STD
     * @param postiDisIniziali posti disabili liberi iniziali, 0..MAX_DIS
     */
    //@ requires 0 <= postiStdIniziali && postiStdIniziali <= MAX_STD;
    //@ requires 0 <= postiDisIniziali && postiDisIniziali <= MAX_DIS;
    //@ ensures postiStd == postiStdIniziali && postiDis == postiDisIniziali;
    public Parcheggio(int postiStdIniziali, int postiDisIniziali) {
        if (postiStdIniziali < 0 || postiStdIniziali > MAX_STD) {
            throw new IllegalArgumentException("postiStdIniziali fuori range: " + postiStdIniziali);
        }
        if (postiDisIniziali < 0 || postiDisIniziali > MAX_DIS) {
            throw new IllegalArgumentException("postiDisIniziali fuori range: " + postiDisIniziali);
        }
        this.postiStd = postiStdIniziali;
        this.postiDis = postiDisIniziali;
    }

    /**
     * Assegna un posto in base al tipo di utente, replicando la logica di
     * {@code r_gestione_VER}: STD/ABBONATO ricevono un posto standard se
     * disponibile; DISABILE riceve un posto disabile se disponibile, altrimenti
     * un posto standard come fallback. Se nessun posto e' disponibile, restituisce
     * {@link TipoPosto#NESSUNO} e non modifica i contatori.
     *
     * @param utente il tipo di utente rilevato, non nullo
     * @return il tipo di posto assegnato, oppure NESSUNO se non c'e' posto
     */
    //@ requires utente != null;
    //@ ensures (utente == TipoUtente.STD || utente == TipoUtente.ABBONATO) ==>
    //@     ((\old(postiStd) > 0 && \result == TipoPosto.POSTO_STD && postiStd == \old(postiStd) - 1 && postiDis == \old(postiDis))
    //@      || (\old(postiStd) == 0 && \result == TipoPosto.NESSUNO && postiStd == \old(postiStd) && postiDis == \old(postiDis)));
    //@ ensures utente == TipoUtente.DISABILE ==>
    //@     ((\old(postiDis) > 0 && \result == TipoPosto.POSTO_DIS && postiDis == \old(postiDis) - 1 && postiStd == \old(postiStd))
    //@      || (\old(postiDis) == 0 && \old(postiStd) > 0 && \result == TipoPosto.POSTO_STD && postiStd == \old(postiStd) - 1 && postiDis == \old(postiDis))
    //@      || (\old(postiDis) == 0 && \old(postiStd) == 0 && \result == TipoPosto.NESSUNO && postiStd == \old(postiStd) && postiDis == \old(postiDis)));
    public TipoPosto assegnaPosto(TipoUtente utente) {
        if (utente == null) {
            throw new IllegalArgumentException("utente non puo' essere null");
        }
        if (utente == TipoUtente.STD || utente == TipoUtente.ABBONATO) {
            if (postiStd > 0) {
                postiStd = postiStd - 1;
                return TipoPosto.POSTO_STD;
            }
            return TipoPosto.NESSUNO;
        }
        // utente == DISABILE: priorita' al posto disabile, poi fallback su standard
        if (postiDis > 0) {
            postiDis = postiDis - 1;
            return TipoPosto.POSTO_DIS;
        }
        if (postiStd > 0) {
            postiStd = postiStd - 1;
            return TipoPosto.POSTO_STD;
        }
        return TipoPosto.NESSUNO;
    }

    /**
     * Libera un posto precedentemente assegnato, incrementando il contatore
     * corrispondente (regola {@code r_gestione_USC}). Se {@code posto} e'
     * {@link TipoPosto#NESSUNO} non fa nulla.
     *
     * @param posto il tipo di posto da liberare
     */
    //@ requires posto == TipoPosto.POSTO_STD ==> postiStd < MAX_STD;
    //@ requires posto == TipoPosto.POSTO_DIS ==> postiDis < MAX_DIS;
    //@ ensures posto == TipoPosto.POSTO_STD ==> postiStd == \old(postiStd) + 1 && postiDis == \old(postiDis);
    //@ ensures posto == TipoPosto.POSTO_DIS ==> postiDis == \old(postiDis) + 1 && postiStd == \old(postiStd);
    //@ ensures posto == TipoPosto.NESSUNO ==> postiStd == \old(postiStd) && postiDis == \old(postiDis);
    public void liberaPosto(TipoPosto posto) {
        if (posto == null) {
            throw new IllegalArgumentException("posto non puo' essere null");
        }
        if (posto == TipoPosto.POSTO_STD) {
            if (postiStd >= MAX_STD) {
                throw new IllegalStateException("posti_std e' gia' al massimo: impossibile liberare");
            }
            postiStd = postiStd + 1;
        } else if (posto == TipoPosto.POSTO_DIS) {
            if (postiDis >= MAX_DIS) {
                throw new IllegalStateException("posti_dis e' gia' al massimo: impossibile liberare");
            }
            postiDis = postiDis + 1;
        }
        // TipoPosto.NESSUNO -> nessuna operazione
    }

    //@ ensures \result == postiStd;
    public /*@ pure @*/ int getPostiStd() {
        return postiStd;
    }

    //@ ensures \result == postiDis;
    public /*@ pure @*/ int getPostiDis() {
        return postiDis;
    }
}
