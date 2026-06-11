package com.tacs.tp1c2026.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint as restricted to the resource owner (identified by a {@code @PathVariable}) or an {@code ADMIN} user.
 * The check is performed by {@link OwnerOrAdminInterceptor} comparing the JWT {@code userId} (set by {@link JwtAuthenticationFilter}) against the path variable named by {@link #value()}.
 * Defaults to the path variable {@code id}; if the endpoint uses a different name, pass it as an argument.
 * Bypass: if the JWT role is {@code ADMIN}, access to other users' resources is allowed.
 * No match and not ADMIN → 403 Forbidden with body {@link com.tacs.tp1c2026.entities.dto.common.ApiError}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresOwnerOrAdmin {
    /** Nombre del {@code @PathVariable} que identifica al user dueño del recurso. */
    String value() default "id";
}
