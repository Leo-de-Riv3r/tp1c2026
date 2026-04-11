package com.tacs.tp1c2026.exception;

public class UsuarioNotFoundException extends RuntimeException {

    public UsuarioNotFoundException(Long id) {
        super("No se ha encontrado el usuario con id " + id);
    }

}
