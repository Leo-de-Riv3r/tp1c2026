package com.tacs.tp1c2026.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un endpoint como restringido al dueño del recurso (identificado por un {@code @PathVariable}
 * o un {@code @RequestParam}) o a un user {@code ADMIN}.
 *
 * <p>La verificación la hace {@link OwnerOrAdminInterceptor} comparando el {@code userId} de la sesión
 * (seteado por {@link SessionAuthenticationFilter}) contra el valor extraído del path o query (según
 * {@link #source()}) por el nombre en {@link #value()}.
 *
 * <p>Por default usa el path variable {@code id}. Para query params: {@code source = Source.QUERY}.
 *
 * <p>Comportamiento con {@link Source#QUERY}: si el query param está <strong>ausente</strong>, la
 * verificación bypassa (significa "usar el user actual por default"). Si está <strong>presente y
 * no matchea</strong> el user de la sesión (y el caller no es ADMIN), el request se rechaza con 403.
 *
 * <p>Bypass: si el rol de la sesión es {@code ADMIN}, el acceso siempre se permite.
 *
 * <p>Si no matchea y no es ADMIN → 403 Forbidden con body {@link com.tacs.tp1c2026.entities.dto.common.ApiError}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresOwnerOrAdmin {
    /** Nombre del path variable o query param que identifica al user dueño del recurso. */
    String value() default "id";

    /** De dónde extraer el identificador: path variable (default) o query param. */
    Source source() default Source.PATH;

    enum Source { PATH, QUERY }
}
