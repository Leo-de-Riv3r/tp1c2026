package com.tacs.tp1c2026.entities.user;

import org.bson.Document;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertCallback;
import org.springframework.stereotype.Component;

@Component
public class UserAfterConvertCallback implements AfterConvertCallback<User> {
    @Override
    public User onAfterConvert(User entity, Document document, String collection) {
        boolean profileIsStale = entity.getProfile().isEmpty()
            && (!entity.getCollection().isEmpty() || !entity.getMissingCards().isEmpty());
        if (profileIsStale) {
            entity.rebuildVectorProfile();
        }
        return entity;
    }
}
