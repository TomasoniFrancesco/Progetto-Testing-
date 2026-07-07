package it.tvsw.smartparking.core;

/**
 * Interfaccia di notifica verso l'esterno (UI, log, ecc.). La FSM la invoca
 * ad ogni cambio di stato significativo. Permette di testare le notifiche
 * con Mockito senza dipendere da Vaadin.
 */
public interface Display {

    /**
     * Mostra un messaggio informativo relativo a un cambio di stato del sistema.
     *
     * @param messaggio testo da mostrare all'utente/operatore
     */
    void mostra(String messaggio);
}
