package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.exception.UsuarioNotFoundException;
import com.tacs.tp1c2026.repository.UserRepository;

public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public Usuario getById(Long id) throws UsuarioNotFoundException {
        return repository.findById(id).orElseThrow(() -> new UsuarioNotFoundException(id));
    }

}
