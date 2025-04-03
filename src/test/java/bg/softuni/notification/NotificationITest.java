package bg.softuni.notification;


import bg.softuni.notification.model.Notification;
import bg.softuni.notification.repository.NotificationRepository;
import bg.softuni.notification.web.dto.NotificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class NotificationITest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sendNotification_shouldPersistNotification() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/notifications/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
{
    "userId": "%s",
    "type": "EMAIL",
    "contactInfo": "test@example.com",
    "notificationEnabled": true
}
""".formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enabled").value(true));

        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .subject("Test Subject")
                .body("This is a test message")
                .build();

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject").value("Test Subject"));

        List<Notification> notifications = notificationRepository.findAll()
                .stream()
                .filter(n -> n.getUserId().equals(userId))
                .toList();

        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getSubject()).isEqualTo("Test Subject");
        assertThat(notifications.get(0).getBody()).isEqualTo("This is a test message");
    }

}

