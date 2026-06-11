package com.tacs.tp1c2026.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un endpoint que recibe un userId por {@code @PathVariable} y necesita verificar
 * que el usuario existe en la base de datos antes de ejecutar el handler.
 * El check lo hace {@link UserValidationInterceptor}; si no existe, devuelve 404
 * con body {@link com.tacs.tp1c2026.entities.dto.common.ApiError}.
 * Por default toma el path variable {@code id}; si el endpoint usa otro nombre, pasarlo como argumento.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidatesPathUser {
    String value() default "id";
}
