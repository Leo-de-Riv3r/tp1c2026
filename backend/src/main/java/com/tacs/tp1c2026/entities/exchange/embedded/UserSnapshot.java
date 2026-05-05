package com.tacs.tp1c2026.entities.exchange.embedded;

import com.tacs.tp1c2026.entities.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserSnapshot {

    private String userId;
    private String name;
    private String avatarId;

    public static UserSnapshot from(User user) {
        return new UserSnapshot(user.getId(), user.getName(), user.getAvatarId());
    }
}
