package com.example.demo.service;

import com.trustify.trustify.dto.Res.NotificationResponse;
import com.trustify.trustify.entity.Notification;
import com.trustify.trustify.entity.User;
import com.trustify.trustify.entity.UserBusiness;
import com.trustify.trustify.enums.NotificationType;
import com.trustify.trustify.repository.NotificatonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificatonRepository notificationRepository;

    /**
     * Gửi notification cho User và lưu vào DB
     * FE subscribe: /user/queue/notifications
     */
    @Transactional
    public void sendToUser(User user, String message, NotificationType type) {
        // Lưu vào DB
        Notification notification = new Notification();
        notification.setMessage(message);
        notification.setType(type);
        notification.setUser(user);
        notificationRepository.save(notification);

        // Gửi qua WebSocket
        NotificationResponse response = buildResponse(notification);
        messagingTemplate.convertAndSendToUser(
                user.getName(),
                "/queue/notifications",
                response
        );
    }

    @Transactional
    public void sendToAdmin(String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setMessage(message);
        notification.setType(type);
        notificationRepository.save(notification);

        NotificationResponse response = buildResponse(notification);
        messagingTemplate.convertAndSendToUser(
                "admin",
                "/queue/notifications",
                response
        );
    }

    /**
     * Gửi notification cho UserBusiness và lưu vào DB
     */
    @Transactional
    public void sendToUserBusiness(UserBusiness userBusiness, String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setMessage(message);
        notification.setType(type);
        notification.setUserBusiness(userBusiness);
        notificationRepository.save(notification);

        NotificationResponse response = buildResponse(notification);
        messagingTemplate.convertAndSendToUser(
                userBusiness.getName(),
                "/queue/notifications",
                response
        );
    }

    /**
     * Broadcast notification cho tất cả users (không lưu DB)
     * FE subscribe: /topic/notifications
     */
    public void broadcast(String message, NotificationType type) {
        NotificationResponse response = new NotificationResponse();
        response.setMessage(message);
        response.setType(type.name());
        response.setTimestamp(Instant.now());
        response.setRead(false);
        messagingTemplate.convertAndSend("/topic/notifications", response);
    }

    /**
     * Gửi notification với custom destination
     */
    public void sendToDestination(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
    }

    private NotificationResponse buildResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setMessage(notification.getMessage());
        response.setType(notification.getType().name());
        response.setTimestamp(Instant.now());
        response.setRead(false);
        return response;
    }
}
