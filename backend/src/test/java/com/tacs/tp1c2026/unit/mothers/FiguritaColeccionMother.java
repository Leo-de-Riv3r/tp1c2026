package com.tacs.tp1c2026.unit.mothers;

import java.util.Random;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.enums.TipoParticipacion;

public class FiguritaColeccionMother {

    private Integer cantidad;
    private TipoParticipacion tipoParticipacion;
    private Figurita figurita;

    private FiguritaColeccionMother() {
        this.cantidad = 1;
        this.tipoParticipacion = TipoParticipacion.INTERCAMBIO;
        this.figurita = FiguritaMother.builder().build();
    }

    public static FiguritaColeccionMother builder() {
        return new FiguritaColeccionMother();
    }

    public FiguritaColeccionMother withCantidad(Integer cantidad) {
        this.cantidad = cantidad;
        return this;
    }

    public FiguritaColeccionMother withTipoParticipacion(TipoParticipacion tipoParticipacion) {
        this.tipoParticipacion = tipoParticipacion;
        return this;
    }

    public FiguritaColeccionMother withFigurita(Figurita figurita) {
        this.figurita = figurita;
        return this;
    }

    public FiguritaColeccion build() {
        return new FiguritaColeccion(this.cantidad, this.tipoParticipacion, this.figurita);
    }
}
