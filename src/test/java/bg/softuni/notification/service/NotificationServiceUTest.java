package bg.softuni.notification.service;

import bg.softuni.notification.model.Notification;
import bg.softuni.notification.model.NotificationPreference;
import bg.softuni.notification.model.NotificationStatus;
import bg.softuni.notification.model.NotificationType;
import bg.softuni.notification.repository.NotificationPreferenceRepository;
import bg.softuni.notification.repository.NotificationRepository;
import bg.softuni.notification.web.dto.NotificationRequest;
import bg.softuni.notification.web.dto.NotificationTypeRequest;
import bg.softuni.notification.web.dto.UpsertNotificationPreference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceUTest {

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;
    @Mock
    private MailSender mailSender;
    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;


    @Test
    void givenNewUser_whenUpsertPreference_thenCreatesNewPreference() {
        UUID userId = UUID.randomUUID();

        UpsertNotificationPreference dto = UpsertNotificationPreference.builder()
                .userId(userId)
                .notificationEnabled(true)
                .type(NotificationTypeRequest.EMAIL)
                .contactInfo("new@example.com")
                .build();

        when(notificationPreferenceRepository.save(any(NotificationPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreference saved = notificationService.upsertPreference(dto);

        assertThat(saved).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getType()).isEqualTo(NotificationType.EMAIL);
        assertThat(saved.getContactInfo()).isEqualTo("new@example.com");

        verify(notificationPreferenceRepository).save(any(NotificationPreference.class));
    }

    @Test
    void givenExistingUser_whenUpsertPreference_thenUpdatesExisting() {
        UUID userId = UUID.randomUUID();

        NotificationPreference existing = NotificationPreference.builder()
                .userId(userId)
                .contactInfo("old@example.com")
                .enabled(false)
                .type(NotificationType.EMAIL)
                .createdOn(LocalDateTime.now().minusDays(1))
                .updatedOn(LocalDateTime.now().minusDays(1))
                .build();

        UpsertNotificationPreference dto = UpsertNotificationPreference.builder()
                .userId(userId)
                .notificationEnabled(true)
                .type(NotificationTypeRequest.EMAIL)
                .contactInfo("new@example.com")
                .build();

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        when(notificationPreferenceRepository.save(any(NotificationPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreference updated = notificationService.upsertPreference(dto);

        assertThat(updated.getContactInfo()).isEqualTo("new@example.com");
        assertThat(updated.isEnabled()).isTrue();

        verify(notificationPreferenceRepository).save(existing);
    }


    @Test
    void givenExistingUserId_whenGetPreferenceByUserId_thenReturnsPreference() {
        UUID userId = UUID.randomUUID();

        NotificationPreference preference = NotificationPreference.builder()
                .userId(userId)
                .contactInfo("pesho@example.com")
                .enabled(true)
                .type(NotificationType.EMAIL)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));

        NotificationPreference result = notificationService.getPreferenceByUserId(userId);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getContactInfo()).isEqualTo("pesho@example.com");
        assertThat(result.isEnabled()).isTrue();
    }

    @Test
    void givenNonExistingUserId_whenGetPreferenceByUserId_thenThrowsException() {
        UUID userId = UUID.randomUUID();

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getPreferenceByUserId(userId))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Notification preference for user id");
    }

    @Test
    void givenEnabledPreference_whenSendNotification_thenSavesSucceededNotification() {
        UUID userId = UUID.randomUUID();

        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .subject("Test Subject")
                .body("Test Body")
                .build();

        NotificationPreference pref = NotificationPreference.builder()
                .userId(userId)
                .enabled(true)
                .contactInfo("test@example.com")
                .type(NotificationType.EMAIL)
                .build();

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.sendNotification(request);

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SUCCEEDED);
        assertThat(result.getSubject()).isEqualTo("Test Subject");
        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(notificationRepository).save(any());
    }

    @Test
    void givenDisabledPreference_whenSendNotification_thenThrowsException() {
        UUID userId = UUID.randomUUID();

        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .subject("Subject")
                .body("Body")
                .build();

        NotificationPreference pref = NotificationPreference.builder()
                .userId(userId)
                .enabled(false)
                .contactInfo("test@example.com")
                .type(NotificationType.EMAIL)
                .build();

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

        assertThatThrownBy(() -> notificationService.sendNotification(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not allow to receive notifications");
    }

    @Test
    void givenExceptionInMailSender_whenSendNotification_thenSetsStatusFailed() {
        UUID userId = UUID.randomUUID();

        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .subject("Subject")
                .body("Body")
                .build();

        NotificationPreference pref = NotificationPreference.builder()
                .userId(userId)
                .enabled(true)
                .contactInfo("test@example.com")
                .type(NotificationType.EMAIL)
                .build();

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));
        doThrow(new RuntimeException("Mail error")).when(mailSender).send(any(SimpleMailMessage.class));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.sendNotification(request);

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(notificationRepository).save(any());
    }


    @Test
    void givenUserId_whenGetNotificationHistory_thenReturnsCorrectList() {


        UUID userId = UUID.randomUUID();

        Notification notification1 = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .subject("Subject 1")
                .body("Body 1")
                .deleted(false)
                .build();

        Notification notification2 = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .subject("Subject 2")
                .body("Body 2")
                .deleted(false)
                .build();

        List<Notification> mockNotifications = List.of(notification1, notification2);

        when(notificationRepository.findAllByUserIdAndDeletedIsFalse(userId))
                .thenReturn(mockNotifications);

        List<Notification> result = notificationService.getNotificationHistory(userId);


        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(notification1, notification2);
        verify(notificationRepository).findAllByUserIdAndDeletedIsFalse(userId);
    }


}

