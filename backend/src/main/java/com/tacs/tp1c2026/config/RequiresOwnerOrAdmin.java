package com.tacs.tp1c2026.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint as restricted to the resource owner (identified by a {@code @PathVariable}
 * or a {@code @RequestParam}) or an {@code ADMIN} user.
 *
 * <p>The check is performed by {@link OwnerOrAdminInterceptor} comparing the JWT {@code userId}
 * (set by {@link JwtAuthenticationFilter}) against the value extracted from the path or query
 * (according to {@link #source()}) by the name in {@link #value()}.
 *
 * <p>Defaults to the path variable {@code id}. For query params use {@code source = Source.QUERY}.
 *
 * <p>Behavior with {@link Source#QUERY}: if the query param is <strong>absent</strong>, the check
 * bypasses (meaning "default to current user"). If it is <strong>present and does not match</strong>
 * the JWT user (and the caller is not ADMIN), the request is rejected with 403.
 *
 * <p>Bypass: if the JWT role is {@code ADMIN}, access is always allowed.
 *
 * <p>No match and not ADMIN → 403 Forbidden with body {@link com.tacs.tp1c2026.entities.dto.common.ApiError}.
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
