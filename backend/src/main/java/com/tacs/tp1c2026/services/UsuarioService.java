package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Publicacion;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.exception.UsuarioNotFoundException;
import com.tacs.tp1c2026.repository.UserRepository;

public class UsuarioService {

    private final UserRepository repository;

    public UsuarioService(UserRepository repository) {
        this.repository = repository;
    }

    public Usuario getById(Long id) throws UsuarioNotFoundException {
        return repository.findById(id).orElseThrow(() -> new UsuarioNotFoundException(id));
    }

    public void addFiguritaColeccionToUser(Usuario user, Publicacion figurita) {
        user.addFiguritaColeccion(figurita);
        repository.save(user);
    }


}
