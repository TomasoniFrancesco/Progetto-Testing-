# Funzionamento del sistema Smart Parking

Il programma simula la gestione automatizzata degli accessi di un parcheggio intelligente. 

Nel sistema è definita una capienza massima fissa di posti auto, che sono suddivisi in due categorie: posti "standard" e posti "riservati" (per persone con disabilità). Il perimetro del parcheggio è controllato da due sbarre automatizzate, una posizionata all'ingresso e una all'uscita, dotate di sensori di rilevamento.

Gli automobilisti che interagiscono con il parcheggio possono appartenere a tre categorie:
* Utenti Standard: ritirano il ticket e pagano all'uscita.
* Utenti Abbonati: hanno un pass prepagato.
* Utenti Disabili: hanno diritto ad accedere ai posti riservati.

Ad ogni step della macchina, il sistema legge i sensori per verificare se un'auto si è presentata in ingresso o in uscita. Se rileva un veicolo in ingresso, la macchina valuta la tipologia dell'utente e decide di conseguenza se consentire l'accesso aprendo la sbarra.

Le casistiche di accesso sono le seguenti:
* Se l'utente è **Standard** o **Abbonato**, il sistema controlla i posti standard. Se ce n'è almeno uno libero, la sbarra di ingresso si apre, il veicolo entra e il numero di posti standard disponibili diminuisce di uno. Se i posti standard sono esauriti, l'accesso viene negato (anche se ci sono posti riservati liberi).
* Se l'utente è **Disabile**, il sistema controlla unicamente i posti riservati. Se c'è disponibilità, la sbarra si apre e i posti riservati diminuiscono di uno. 

Quando un utente decide di uscire, il sistema esegue un controllo alla sbarra di uscita. Agli utenti Standard viene calcolata una tariffa proporzionale al tempo di permanenza nel parcheggio. Gli Abbonati e i Disabili, invece, possono procedere direttamente. Una volta che l'uscita è autorizzata, la sbarra si apre, l'auto esce e il conteggio dei posti disponibili (standard o riservati, a seconda di chi è uscito) viene incrementato.

Il sistema è progettato in modo da garantire sempre due regole fondamentali di sicurezza:
1. Il numero totale di auto presenti non può mai superare la capienza massima stabilita.
2. I contatori dei posti occupati e disponibili non possono mai assumere valori negativi.

### Riassunto delle regole di accesso (Tabella esplicativa)

| Tipologia Utente | Condizione per l'apertura della sbarra | Posto scalato |
| :--- | :--- | :--- |
| Standard | Posti Standard disponibili > 0 | Standard |
| Abbonato | Posti Standard disponibili > 0 | Standard |
| Disabile | Posti Riservati disponibili > 0 | Riservato |

### Diagramma degli stati della Sbarra

Di seguito è illustrato il funzionamento meccanico delle sbarre di accesso:

---
config:
  layout: elk
---
flowchart LR
    IDLE(("IDLE")) -- "Sens_In=1 / -" --> CHK_IN(("CHECK\nENTRY"))
    IDLE -- "Sens_Out=1 / -" --> CHK_OUT(("CHECK\nEXIT"))
    CHK_IN -- "Ticket_o_Pass / -" --> VER(("VERIFICA\nPOSTI"))
    VER -- "Posti=0 / MostraMsg" --> NEG(("ACCESSO\nNEGATO"))
    VER -- Posti>0 / ApriSbarra --> INGR(("SBARRA\nINGRESSO"))
    NEG -- "AutoVia=1 / ChiudiSbarra" --> IDLE
    INGR -- "TransitoOk=1 / DecrPosti" --> PARK(("CAR\nPARKED"))
    PARK -- "Sens_Out=1 / -" --> CHK_OUT
    PARK -- TimeTick / AggiornaTempo --> PARK
    CHK_OUT -- "Utente=Std / -" --> TARIF(("CALCOLO\nTARIFFA"))
    CHK_OUT -- "Utente=Dis_Abb / ApriSbarra" --> USC(("SBARRA\nUSCITA"))
    TARIF -- "Pagamento=OK / ApriSbarra" --> USC
    TARIF -- "Pagamento=KO / -" --> TARIF
    USC -- "TransitoOk=1 / IncrPosti" --> IDLE

* **Stato 0 (CHIUSA):** La sbarra è abbassata.
* **Stato 1 (IN_APERTURA):** Il sistema ha autorizzato l'ingresso/uscita, la sbarra si sta alzando.
* **Stato 2 (APERTA):** La sbarra è completamente alzata, il veicolo sta transitando.
* **Stato 3 (IN_CHIUSURA):** Il veicolo è passato, la sbarra si riabbassa per tornare allo Stato 0.