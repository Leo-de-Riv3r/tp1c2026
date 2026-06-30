package com.tacs.tp1c2026.entities.dto.user.output;

import com.tacs.tp1c2026.entities.user.User;

/**
 * DTO de respuesta para los datos del usuario. Excluye campos sensibles (passwordHash, collection, missingCards).
 * Los nombres de los campos coinciden con la interfaz User del frontend.
 */
public record UserDto(
        String id,
        String name,
        String email,
        Double rating,
        Integer exchangesAmount,
        String avatarId,
        String creationDate,
        String role
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRating(),
                user.getExchangesAmount(),
                user.getAvatarId(),
                user.getCreationDate().toString(),
                user.getRole().name()
        );
    }
}
