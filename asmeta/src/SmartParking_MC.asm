asm SmartParking_MC
import StandardLibrary
import CTLLibrary

signature:
	enum domain StatoSistema = {IDLE | CHK_IN | VER | NEG | INGR | CHK_OUT | TARIF | USC}
	enum domain TipoUtente = {STD | DISABILE | ABBONATO}
	enum domain TipoPosto = {POSTO_STD | POSTO_DIS | NESSUNO}
	dynamic monitored sens_in: Boolean
	dynamic monitored sens_out: Boolean
	dynamic monitored transito_ok: Boolean
	dynamic monitored auto_via: Boolean
	dynamic monitored utente_rilevato: TipoUtente
	dynamic monitored pagamento_ok: Boolean
	dynamic controlled stato: StatoSistema
	dynamic controlled posti_std: Integer
	dynamic controlled posti_dis: Integer
	dynamic controlled posto_assegnato: TipoPosto

definitions:
	rule r_gestione_IDLE =
		if sens_in = true then stato := CHK_IN
		else if sens_out = true then stato := CHK_OUT endif endif

	rule r_gestione_CHK_IN = stato := VER

	rule r_gestione_VER =
		if utente_rilevato = STD or utente_rilevato = ABBONATO then
			if posti_std > 0 then
				par
					stato := INGR
					posto_assegnato := POSTO_STD
				endpar
			else stato := NEG endif
		else
			if posti_dis > 0 then
				par
					stato := INGR
					posto_assegnato := POSTO_DIS
				endpar
			else
				if posti_std > 0 then
					par
						stato := INGR
						posto_assegnato := POSTO_STD
					endpar
				else stato := NEG endif
			endif
		endif

	rule r_gestione_NEG =
		if auto_via = true then stato := IDLE endif

	rule r_gestione_INGR =
		if transito_ok = true then
			par
				stato := IDLE
				if posto_assegnato = POSTO_STD then posti_std := posti_std - 1
				else if posto_assegnato = POSTO_DIS then posti_dis := posti_dis - 1 endif endif
			endpar
		endif

	rule r_gestione_CHK_OUT =
		if utente_rilevato = STD then stato := TARIF else stato := USC endif

	rule r_gestione_TARIF =
		if pagamento_ok = true then stato := USC endif

	rule r_gestione_USC =
		if transito_ok = true then
			par
				stato := IDLE
				if posto_assegnato = POSTO_STD then posti_std := posti_std + 1
				else if posto_assegnato = POSTO_DIS then posti_dis := posti_dis + 1 endif endif
				posto_assegnato := NESSUNO
			endpar
		endif

	// Safety: il numero di posti standard liberi non e' mai negativo
	CTLSPEC ag(posti_std >= 0)
	// Safety: il numero di posti disabili liberi non e' mai negativo
	CTLSPEC ag(posti_dis >= 0)
	// Raggiungibilita': esiste un cammino che porta al rifiuto di accesso (NEG)
	CTLSPEC ef(stato = NEG)
	// Safety: se si sta per assegnare un posto standard, deve essercene almeno uno libero
	CTLSPEC ag((stato = INGR and posto_assegnato = POSTO_STD) implies posti_std > 0)

	// Safety: i posti standard liberi non superano mai la capacita' iniziale (posti_std <= 1):
	// il sistema non puo' "inventare" posti oltre a quelli di partenza
	CTLSPEC ag(posti_std <= 1)
	// Safety: analogo per i posti disabili (posti_dis <= 1)
	CTLSPEC ag(posti_dis <= 1)
	// Safety: se si sta per assegnare un posto disabile, deve essercene almeno uno libero
	// (garantisce che il ramo fallback non assegni mai POSTO_DIS quando posti_dis = 0)
	CTLSPEC ag((stato = INGR and posto_assegnato = POSTO_DIS) implies posti_dis > 0)
	// Raggiungibilita': e' possibile arrivare allo stato di pagamento tariffa (TARIF),
	// cioe' il ramo "utente STD in uscita" e' effettivamente percorribile
	CTLSPEC ef(stato = TARIF)
	// Raggiungibilita': e' possibile arrivare allo stato di uscita fisica (USC)
	CTLSPEC ef(stato = USC)
	// Liveness: da qualunque configurazione in cui il sistema e' in attesa di pagamento (TARIF)
	// esiste sempre un cammino che riporta il sistema in IDLE (nessun blocco permanente in cassa)
	CTLSPEC ag(stato = TARIF implies ef(stato = IDLE))

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

default init s0:
	function stato = IDLE
	function posti_std = 1
	function posti_dis = 1
	function posto_assegnato = NESSUNO