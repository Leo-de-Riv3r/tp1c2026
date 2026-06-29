package com.tacs.tp1c2026.entities.enums;

/**
 * Rol de un {@link com.tacs.tp1c2026.entities.user.User}. Se guarda como string en Mongo y se propaga en la sesión (atributo {@code role} del request) para que el FE decida qué UI mostrar y para que {@link com.tacs.tp1c2026.config.RequiresRole} pueda restringir endpoints
 */
public enum UserRole {
    USER, ADMIN
}
