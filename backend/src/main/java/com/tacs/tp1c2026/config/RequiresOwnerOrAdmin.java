package com.tacs.tp1c2026.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un endpoint como restringido al dueño del recurso (identificado por un {@code @PathVariable}) o a un user con role {@code ADMIN}.
 * El check lo hace {@link OwnerOrAdminInterceptor} comparando el {@code userId} del JWT (puesto por {@link JwtAuthenticationFilter}) contra el path variable nombrado por {@link #value()}.
 * Por default toma el path variable {@code id}; si el endpoint usa otro nombre, pasarlo como argumento.
 * Bypass: si el role del JWT es {@code ADMIN}, se permite acceder a recursos de otros users.
 * No matchea ni es ADMIN → 403 Forbidden con body {@link com.tacs.tp1c2026.entities.dto.common.ApiError}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresOwnerOrAdmin {
    /** Nombre del {@code @PathVariable} que identifica al user dueño del recurso. */
    String value() default "id";
}
