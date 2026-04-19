package com.tacs.tp1c2026.unit.mothers;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.Usuario;

public class EntityMother {

    public Usuario user() {
        return UsuarioMother.builder().build();
    }

    public UsuarioMother userBuilder() {
        return UsuarioMother.builder();
    }

    public Figurita figurita() {
        return FiguritaMother.builder().build();
    }

    public FiguritaMother figuritaBuilder() {
        return FiguritaMother.builder();
    }

    public FiguritaColeccion figuritaColeccion() {
        return FiguritaColeccionMother.builder().build();
    }

    public FiguritaColeccionMother figuritaColeccionBuilder() {
        return FiguritaColeccionMother.builder();
    }
}
