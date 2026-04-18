package com.tacs.tp1c2026.unit.mothers;

import java.util.Random;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.Usuario;

public class EntityMother {

    public Usuario user() {
        Usuario usuario = new Usuario();
        usuario.initializeVectorProfile();
        usuario.setId(new Random().nextInt(1000));
        usuario.setNombre("Usuario" + usuario.getId());
        return usuario;
    }

    public FiguritaColeccion figuritaColeccion() {
        FiguritaColeccion figurita = new FiguritaColeccion();   
        return figurita;
    }

    public Figurita figurita() {
        Figurita figurita = new Figurita();
        return figurita;
    }   
    
}
