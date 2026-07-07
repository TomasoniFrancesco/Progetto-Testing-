package it.tvsw.smartparking.core;

/**
 * Gli 8 stati operativi della macchina a stati, identici al dominio
 * {@code StatoSistema} definito in {@code src/SmartParking.asm}.
 */
public enum StatoSistema {
    IDLE,
    CHK_IN,
    VER,
    NEG,
    INGR,
    CHK_OUT,
    TARIF,
    USC
}
