package it.tvsw.smartparking.jml;

import it.tvsw.smartparking.core.Parcheggio;
import it.tvsw.smartparking.core.TipoPosto;
import it.tvsw.smartparking.core.TipoUtente;

/**
 * Programma di prova per la verifica <em>dinamica</em> dei contratti JML
 * (Runtime Assertion Checking, RAC).
 *
 * <p>Mentre la modalita' {@code --esc} dimostra staticamente che i contratti
 * valgono per ogni esecuzione possibile, la modalita' {@code --rac} li fa
 * controllare mentre il programma gira: se una precondizione o una
 * postcondizione viene violata, l'esecuzione si ferma con un errore che dice
 * quale clausola e' saltata e a che riga.</p>
 *
 * <p>Questa classe serve solo a quello: viola i contratti di proposito, uno
 * alla volta, per far vedere che il controllo scatta davvero. Non fa parte del
 * sistema e non viene mai chiamata dalla FSM ne' dalla UI.</p>
 *
 * <p>Compilazione ed esecuzione (dalla root del progetto):</p>
 * <pre>
 * openjml --rac -d out \
 *     java/src/main/java/it/tvsw/smartparking/core/*.java \
 *     java/src/main/java/it/tvsw/smartparking/jml/DemoRac.java
 * java -cp out:$OPENJML_HOME/jmlruntime.jar it.tvsw.smartparking.jml.DemoRac
 * </pre>
 */
public final class DemoRac {

    private DemoRac() {
        // classe di sola dimostrazione, non istanziabile
    }

    public static void main(String[] args) {
        usoCorretto();
        violaPrecondizioneCostruttore();
        violaPrecondizioneLiberaPosto();
    }

    /** Sequenza legittima: nessun contratto viene violato, non stampa errori. */
    private static void usoCorretto() {
        System.out.println("--- 1. Uso corretto (nessuna violazione attesa) ---");
        Parcheggio p = new Parcheggio();
        TipoPosto assegnato = p.assegnaPosto(TipoUtente.DISABILE);
        System.out.println("Assegnato: " + assegnato + ", posti disabili: " + p.getPostiDis());
        p.liberaPosto(assegnato);
        System.out.println("Rilasciato, posti disabili: " + p.getPostiDis());
    }

    /**
     * Viola il {@code requires} del costruttore con parametri, che pretende
     * valori iniziali compresi tra 0 e la capacita' massima. Qui ne passo 5,
     * mentre MAX_STD vale 1.
     */
    private static void violaPrecondizioneCostruttore() {
        System.out.println("--- 2. Violazione del requires del costruttore ---");
        Parcheggio p = new Parcheggio(5, 0);
        System.out.println("Non dovremmo arrivare qui: " + p.getPostiStd());
    }

    /**
     * Viola il {@code requires} di {@code liberaPosto}, che pretende che il
     * contatore non sia gia' al massimo. Qui libero un posto standard su un
     * parcheggio in cui non e' entrato nessuno, quindi postiStd vale gia'
     * MAX_STD e l'incremento sfonderebbe l'invariante.
     */
    private static void violaPrecondizioneLiberaPosto() {
        System.out.println("--- 3. Violazione del requires di liberaPosto ---");
        Parcheggio p = new Parcheggio();
        p.liberaPosto(TipoPosto.POSTO_STD);
        System.out.println("Non dovremmo arrivare qui: " + p.getPostiStd());
    }
}
