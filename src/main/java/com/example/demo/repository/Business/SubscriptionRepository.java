package com.example.demo.repository.Business;

import com.trustify.trustify.entity.Subscription;
import com.trustify.trustify.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // Subscription đang active của company
    Optional<Subscription> findByCompanyIdAndStatus(Long companyId, SubscriptionStatus status);

    // Subscription mới nhất của company
    Optional<Subscription> findFirstByCompanyIdOrderByCreatedAtDesc(Long companyId);

    // Tất cả subscription của company (lịch sử)
    List<Subscription> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    // Subscription đang active và chưa hết hạn
    @Query("SELECT s FROM Subscription s WHERE s.company.id = :companyId " +
            "AND s.status = 'ACTIVE' AND s.endDate > :now")
    Optional<Subscription> findActiveSubscription(Long companyId, LocalDateTime now);

    // Tìm các subscription sắp hết hạn (để gửi notification)
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' " +
            "AND s.endDate BETWEEN :now AND :expireDate")
    List<Subscription> findExpiringSubscriptions(LocalDateTime now, LocalDateTime expireDate);
}
