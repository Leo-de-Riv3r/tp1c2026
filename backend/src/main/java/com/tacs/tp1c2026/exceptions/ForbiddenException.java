package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando un user autenticado intenta hacer una acción que no le está permitida.
 * Mapea a HTTP 403 Forbidden.
 * Ejemplo: un user intentando aceptar una propuesta que pertenece a otro user.
 */
public class ForbiddenException extends CustomException {
    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}