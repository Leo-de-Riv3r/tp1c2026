package com.tacs.tp1c2026.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un endpoint como restringido a un role específico del JWT (ej: "ADMIN")
 * El check lo hace {@link RoleInterceptor} contra el request attribute {@code role} que setea {@link JwtAuthenticationFilter}. Si no matchea, devuelve 403
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {
  String value();
}
