asm SmartParking

import StandardLibrary.asm

signature:
	//  DOMINI 
	enum domain StatoSistema = {IDLE | CHK_IN | VER | NEG | INGR | CHK_OUT | TARIF | USC}
	enum domain TipoUtente = {STD | DISABILE | ABBONATO}
	enum domain TipoPosto = {POSTO_STD | POSTO_DIS | NESSUNO}

	//  FUNZIONI MONITORATE  
	monitored sens_in: Boolean
	monitored sens_out: Boolean
	monitored transito_ok: Boolean
	monitored auto_via: Boolean
	monitored utente_rilevato: TipoUtente
	monitored pagamento_ok: Boolean

	//  FUNZIONI CONTROLLATE (La memoria del sistema) 
	controlled stato: StatoSistema
 	controlled posti_std: Integer
	controlled posti_dis: Integer
	controlled posto_assegnato: TipoPosto // Memoria di quale posto è stato effettivamente preso

	//  FUNZIONI DERIVATE (calcolate, non memorizzate)
	// n-aria (binaria): dice se all'utente $u puo' essere assegnato il posto $p
	derived assegnabile: Prod(TipoUtente, TipoPosto) -> Boolean
	// vero quando NESSUN tipo di utente puo' piu' entrare (parcheggio saturo)
	derived parcheggioPieno: Boolean

definitions:
	//  DEFINIZIONE FUNZIONI DERIVATE 
	// POSTO_STD: assegnabile a chiunque se ci sono posti standard liberi.
	// POSTO_DIS: riservato ai DISABILI, se ci sono posti disabili liberi.
	function assegnabile($u in TipoUtente, $p in TipoPosto) =
		($p = POSTO_STD and posti_std > 0) or
		($p = POSTO_DIS and $u = DISABILE and posti_dis > 0)

	// parcheggioPieno: vero se, per OGNI tipo di utente, non e' assegnabile
	// ne' un posto standard ne' uno disabile. Usa il quantificatore forall.
	function parcheggioPieno =
		(forall $u in TipoUtente with
			(not assegnabile($u, POSTO_STD) and not assegnabile($u, POSTO_DIS)))

	//  REGOLE DI TRANSIZIONE (Gli step della Macchina a Stati) 
	
	rule r_gestione_IDLE =
		if sens_in = true then
			stato := CHK_IN
		else
			if sens_out = true then
				stato := CHK_OUT
			endif
		endif

	rule r_gestione_CHK_IN =
		// Simuliamo il tempo di lettura dell'utente e passiamo alla verifica
		stato := VER

	rule r_gestione_VER =
		//  LA LOGICA DEI POSTI, tramite la derivata n-aria "assegnabile" 
		// Priorita': prima POSTO_DIS (solo per DISABILE), poi POSTO_STD (fallback
		// del disabile e caso normale STD/ABBONATO), altrimenti NEG.
		let ($u = utente_rilevato) in
			if assegnabile($u, POSTO_DIS) then
				par
					stato := INGR
					posto_assegnato := POSTO_DIS
				endpar
			else
				if assegnabile($u, POSTO_STD) then
					par
						stato := INGR
						posto_assegnato := POSTO_STD
					endpar
				else
					stato := NEG
				endif
			endif
		endlet

	rule r_gestione_NEG =
		if auto_via = true then
			stato := IDLE
		endif

	rule r_gestione_INGR =
		if transito_ok = true then
			par
				stato := IDLE
				// Ora scaliamo il posto corretto in base a quello che ci siamo "ricordati"
				if posto_assegnato = POSTO_STD then
					posti_std := posti_std - 1
				else
					if posto_assegnato = POSTO_DIS then
						posti_dis := posti_dis - 1
					endif
				endif
			endpar
		endif


	rule r_gestione_CHK_OUT =
		if utente_rilevato = STD then
			stato := TARIF
		else
			stato := USC
		endif

	rule r_gestione_TARIF =
		if pagamento_ok = true then
			stato := USC
		endif

	rule r_gestione_USC =
		if transito_ok = true then
			par
				stato := IDLE
				// Rilasciamo il posto corretto e resettiamo la memoria
				if posto_assegnato = POSTO_STD then
					posti_std := posti_std + 1
				else
					if posto_assegnato = POSTO_DIS then
						posti_dis := posti_dis + 1
					endif
				endif
				posto_assegnato := NESSUNO
			endpar
		endif

	//  MAIN RULE 
	main rule r_Main =
		par
			if stato = IDLE then r_gestione_IDLE[] endif
			if stato = CHK_IN then r_gestione_CHK_IN[] endif
			if stato = VER then r_gestione_VER[] endif
			if stato = NEG then r_gestione_NEG[] endif
			if stato = INGR then r_gestione_INGR[] endif
			if stato = CHK_OUT then r_gestione_CHK_OUT[] endif
			if stato = TARIF then r_gestione_TARIF[] endif
			if stato = USC then r_gestione_USC[] endif
		endpar

	//  STATO INIZIALE 
	default init s0:
		function stato = IDLE
		// Partiamo con un parcheggio piccolo per non far esplodere il Model Checker dopo
		function posti_std = 1
		function posti_dis = 1
		function posto_assegnato = NESSUNO
		
		