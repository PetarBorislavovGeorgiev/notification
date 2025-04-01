package bg.softuni.notification.service;


import bg.softuni.notification.model.Notification;
import bg.softuni.notification.model.NotificationPreference;
import bg.softuni.notification.model.NotificationStatus;
import bg.softuni.notification.model.NotificationType;
import bg.softuni.notification.repository.NotificationPreferenceRepository;
import bg.softuni.notification.repository.NotificationRepository;
import bg.softuni.notification.web.dto.NotificationRequest;
import bg.softuni.notification.web.dto.UpsertNotificationPreference;
import bg.softuni.notification.web.mapper.DtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final MailSender mailSender;
    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationService(NotificationPreferenceRepository notificationPreferenceRepository, MailSender mailSender, NotificationRepository notificationRepository) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.mailSender = mailSender;
        this.notificationRepository = notificationRepository;
    }

    public NotificationPreference upsertPreference(UpsertNotificationPreference upsertNotificationPreference) {


        Optional<NotificationPreference> userNotificationPreferenceOptional = notificationPreferenceRepository.findByUserId(upsertNotificationPreference.getUserId());

        if (userNotificationPreferenceOptional.isPresent()) {
            NotificationPreference preference = userNotificationPreferenceOptional.get();
            preference.setContactInfo(upsertNotificationPreference.getContactInfo());
            preference.setEnabled(upsertNotificationPreference.isNotificationEnabled());
            preference.setType(DtoMapper.fromNotificationTypeRequest(upsertNotificationPreference.getType()));
            preference.setUpdatedOn(LocalDateTime.now());
            return notificationPreferenceRepository.save(preference);
        }

        NotificationPreference notificationPreference = NotificationPreference.builder()
                .userId(upsertNotificationPreference.getUserId())
                .type(DtoMapper.fromNotificationTypeRequest(upsertNotificationPreference.getType()))
                .enabled(upsertNotificationPreference.isNotificationEnabled())
                .contactInfo(upsertNotificationPreference.getContactInfo())
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
        return notificationPreferenceRepository.save(notificationPreference);
    }

    public NotificationPreference getPreferenceByUserId(UUID userId) {

        return notificationPreferenceRepository.findByUserId(userId).orElseThrow(() -> new NullPointerException("Notification preference for user id %s was not found.".formatted(userId)));
    }

    public Notification sendNotification(NotificationRequest notificationRequest) {

        UUID userId = notificationRequest.getUserId();
        NotificationPreference userPreference = getPreferenceByUserId(userId);

        if (!userPreference.isEnabled()) {
            throw new IllegalArgumentException("User with id %s does not allow to receive notifications.".formatted(userId));
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userPreference.getContactInfo());
        message.setSubject(notificationRequest.getSubject());
        message.setText(notificationRequest.getBody());


        Notification notification = Notification.builder()
                .subject(notificationRequest.getSubject())
                .body(notificationRequest.getBody())
                .createdOn(LocalDateTime.now())
                .userId(userId)
                .deleted(false)
                .type(NotificationType.EMAIL)
                .build();

        try {
            mailSender.send(message);
            notification.setStatus(NotificationStatus.SUCCEEDED);
        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            log.warn("There was an issue sending an email to %s due to %s.".formatted(userPreference.getContactInfo(), e.getMessage()));
        }

        return notificationRepository.save(notification);

    }

    public List<Notification> getNotificationHistory(UUID userId) {

        return notificationRepository.findAllByUserIdAndDeletedIsFalse(userId);
    }
}
