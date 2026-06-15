package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando el request no trae credenciales de autenticación válidas.
 * Mapea a HTTP 401 Unauthorized.
 * Nota: en la mayoría de los casos Spring Security lo maneja antes de llegar al controller.
 */
public class UnauthorizedException extends CustomException {
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}