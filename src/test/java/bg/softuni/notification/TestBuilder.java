package bg.softuni.notification;


import bg.softuni.notification.model.NotificationPreference;
import bg.softuni.notification.model.NotificationType;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class TestBuilder {
    public static NotificationPreference aRandomNotificationPreference() {

        return NotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .enabled(true)
                .contactInfo("text")
                .type(NotificationType.EMAIL)
                .updatedOn(LocalDateTime.now())
                .createdOn(LocalDateTime.now())
                .build();
    }
}
