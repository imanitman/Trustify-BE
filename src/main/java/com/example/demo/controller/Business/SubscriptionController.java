package com.example.demo.controller.Business;

import com.trustify.trustify.entity.Subscription;
import com.trustify.trustify.service.Business.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * Lấy subscription đang active của company
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<?> getActiveSubscription(@PathVariable Long companyId) {
        return subscriptionService.getActiveSubscription(companyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Lịch sử subscription của company
     */
    @GetMapping("/company/{companyId}/history")
    public ResponseEntity<List<Subscription>> getSubscriptionHistory(@PathVariable Long companyId) {
        List<Subscription> history = subscriptionService.getSubscriptionHistory(companyId);
        return ResponseEntity.ok(history);
    }

    /**
     * Kiểm tra trạng thái subscription
     */
    @GetMapping("/check/{companyId}")
    public ResponseEntity<Map<String, Object>> checkSubscription(@PathVariable Long companyId) {
        Map<String, Object> status = subscriptionService.checkSubscriptionStatus(companyId);
        return ResponseEntity.ok(status);
    }

    /**
     * Huỷ subscription
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Subscription> cancelSubscription(@PathVariable Long id) {
        Subscription subscription = subscriptionService.cancelSubscription(id);
        return ResponseEntity.ok(subscription);
    }

    /**
     * Lấy danh sách subscription sắp hết hạn (admin)
     */
    @GetMapping("/expiring")
    public ResponseEntity<List<Subscription>> getExpiringSubscriptions() {
        List<Subscription> expiring = subscriptionService.getExpiringSubscriptions();
        return ResponseEntity.ok(expiring);
    }
}