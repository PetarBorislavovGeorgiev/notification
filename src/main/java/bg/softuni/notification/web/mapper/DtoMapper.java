package bg.softuni.notification.web.mapper;


import bg.softuni.notification.model.Notification;
import bg.softuni.notification.model.NotificationPreference;
import bg.softuni.notification.model.NotificationType;
import bg.softuni.notification.web.dto.NotificationPreferenceResponse;
import bg.softuni.notification.web.dto.NotificationResponse;
import bg.softuni.notification.web.dto.NotificationTypeRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DtoMapper {
    public static NotificationType fromNotificationTypeRequest(NotificationTypeRequest notificationTypeRequest) {

        return switch (notificationTypeRequest) {
            case EMAIL -> NotificationType.EMAIL;
        };
    }

    public static NotificationPreferenceResponse fromNotificationPreference(NotificationPreference entity) {

        return NotificationPreferenceResponse.builder()
                .id(entity.getId())
                .type(entity.getType())
                .contactInfo(entity.getContactInfo())
                .enabled(entity.isEnabled())
                .userId(entity.getUserId())
                .build();
    }

    public static NotificationResponse fromNotification(Notification entity) {

        return NotificationResponse.builder()
                .subject(entity.getSubject())
                .status(entity.getStatus())
                .createdOn(entity.getCreatedOn())
                .type(entity.getType())
                .build();
    }
}
