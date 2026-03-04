package com.example.demo.scheduler;

import com.trustify.trustify.entity.User;
import com.trustify.trustify.enums.UserStatus;
import com.trustify.trustify.repository.Customer.UserRepository;
import com.trustify.trustify.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserStatusScheduler {

    private final UserRepository userRepository;
    private final RedisService redisService;

    // Chạy mỗi giờ kiểm tra user inactive
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void reactivateExpiredInactiveUsers() {
        log.info("Running scheduled task to reactivate expired inactive users");

        List<User> inactiveUsers = userRepository.findByStatus(UserStatus.INACTIVE);

        for (User user : inactiveUsers) {
            // Nếu không còn trong Redis (đã hết 7 ngày) -> chuyển về ACTIVE
            if (!redisService.isUserInactive(user.getEmail())) {
                user.setStatus(UserStatus.ACTIVE);
                userRepository.save(user);
                log.info("Reactivated user: {}", user.getEmail());
            }
        }
    }
}
