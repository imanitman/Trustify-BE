package com.example.demo.service.Business;

import com.trustify.trustify.entity.Subscription;
import com.trustify.trustify.enums.SubscriptionStatus;
import com.trustify.trustify.repository.Business.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    /**
     * Lấy subscription đang active của company
     */
    public Optional<Subscription> getActiveSubscription(Long companyId) {
        return subscriptionRepository.findActiveSubscription(companyId, LocalDateTime.now());
    }

    /**
     * Lấy subscription mới nhất của company
     */
    public Optional<Subscription> getCurrentSubscription(Long companyId) {
        return subscriptionRepository.findFirstByCompanyIdOrderByCreatedAtDesc(companyId);
    }

    /**
     * Lịch sử subscription của company
     */
    public List<Subscription> getSubscriptionHistory(Long companyId) {
        return subscriptionRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
    }

    /**
     * Kiểm tra company có subscription active không
     */
    public Map<String, Object> checkSubscriptionStatus(Long companyId) {
        Map<String, Object> result = new HashMap<>();

        Optional<Subscription> subOpt = getActiveSubscription(companyId);

        if (subOpt.isPresent()) {
            Subscription sub = subOpt.get();
            long daysRemaining = ChronoUnit.DAYS.between(LocalDateTime.now(), sub.getEndDate());

            result.put("hasActiveSubscription", true);
            result.put("planName", sub.getPlan().getName());
            result.put("status", sub.getStatus());
            result.put("startDate", sub.getStartDate());
            result.put("endDate", sub.getEndDate());
            result.put("daysRemaining", daysRemaining);
            result.put("isExpiringSoon", daysRemaining <= 7); // Cảnh báo nếu còn <= 7 ngày
        } else {
            result.put("hasActiveSubscription", false);
            result.put("message", "Không có gói đăng ký nào đang hoạt động");
        }

        return result;
    }

    /**
     * Huỷ subscription
     */
    @Transactional
    public Subscription cancelSubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);

        log.info("Cancelled subscription: id={}", subscriptionId);
        return subscription;
    }

    /**
     * Lấy danh sách subscription sắp hết hạn (trong 7 ngày tới)
     */
    public List<Subscription> getExpiringSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireDate = now.plusDays(7);
        return subscriptionRepository.findExpiringSubscriptions(now, expireDate);
    }
}