package com.example.demo.controller;

import com.trustify.trustify.entity.Notification;
import com.trustify.trustify.entity.User;
import com.trustify.trustify.entity.UserBusiness;
import com.trustify.trustify.repository.NotificatonRepository;
import com.trustify.trustify.repository.Business.UserBusinessRepository;
import com.trustify.trustify.repository.Customer.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificatonRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserBusinessRepository userBusinessRepository;

    /**
     * Lấy tất cả notifications của User theo email
     */
    @GetMapping("/user/{email}")
    public ResponseEntity<List<Notification>> getNotificationsByUser(@PathVariable String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Notification> notifications = notificationRepository.findByUserOrderByIdDesc(user);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Lấy tất cả notifications của UserBusiness theo email
     */
    @GetMapping("/business/{email}")
    public ResponseEntity<List<Notification>> getNotificationsByUserBusiness(@PathVariable String email) {
        UserBusiness userBusiness = new UserBusiness();
        try {
             userBusiness = userBusinessRepository.findByEmail(email)  ;
        }catch (Exception e) {
            throw new RuntimeException("UserBusiness not found");
        }
        List<Notification> notifications = notificationRepository.findByUserBusinessOrderByIdDesc(userBusiness);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Xóa notification
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
