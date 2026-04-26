package com.tacs.tp1c2026.entities.dto.user.output;

import com.tacs.tp1c2026.entities.user.User;

/**
 * Response DTO for user data. Excludes sensitive fields (passwordHash, collection, missingCards)
 * Field names match the frontend User interface
 */
public record UserDto(
        String id,
        String name,
        String email,
        Double rating,
        Integer exchangesAmount,
        String avatarId,
        String creationDate
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRating(),
                user.getExchangesCount(),
                user.getAvatarId(),
                user.getCreationDate().toString()
        );
    }
}
