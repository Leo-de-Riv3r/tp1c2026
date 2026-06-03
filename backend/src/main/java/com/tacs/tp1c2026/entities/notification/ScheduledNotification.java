package com.tacs.tp1c2026.entities.notification;

import com.tacs.tp1c2026.entities.user.User;
import lombok.Builder;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Document("scheduled_notifications")
@Builder
public class ScheduledNotification {

    private final Notification.NotificationData notificationData;

    private final LocalDateTime scheduledTime;

    @DocumentReference
    private final List<User> users;

    public boolean isDue(LocalDateTime currentTime) {
        return scheduledTime.isBefore(currentTime);
    }

    public void sendNotification(){
        Notification notification = Notification.builder().data(notificationData).creationDate(LocalDateTime.now()).build();
        users.forEach(user -> user.receiveNotification(notification));
    }

    public boolean hasReferenceId(String refId) {
        return Objects.equals(this.notificationData.getReferenceId(), refId);
    }

    public void addUser(User user) {
        users.add(user);
    }

    public List<User> getUsers() {
        return users;
    }

}
