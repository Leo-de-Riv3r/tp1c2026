package com.tacs.tp1c2026.mockFactories;

import java.util.Random;

import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.Usuario;

public class EntityFactory {

    public Usuario user() {
        Usuario usuario = new Usuario();
        usuario.setId(new Random().nextInt(1000));
        usuario.setNombre("Usuario" + usuario.getId());
        return usuario;
    }

    public FiguritaColeccion createMockFiguritaColeccion() {
        FiguritaColeccion figurita = new FiguritaColeccion();
        
        return figurita;
    }
    
}
