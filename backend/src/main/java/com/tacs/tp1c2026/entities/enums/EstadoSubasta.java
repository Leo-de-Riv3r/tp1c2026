package com.tacs.tp1c2026.entities.enums;

// Estado de la publicacion de una subasta
// Activa -> la subasta se encuentra disponible
// vencida -> se agotó el tiempo y gana la mejor subasta (ver dsp en caso de ofertar las mismas cant de fguritas) o en caso de que n ofertar nadie no deberia hacer nada (se devuelven las figuritas)
//  cerrada -> el publicante cerró la subasta (la canceló)
// adjudicada -> el publicante dio como ganador a uno
public enum EstadoSubasta {
    ACTIVA, VENCIDA,CERRADA, ADJUDICADA
}
