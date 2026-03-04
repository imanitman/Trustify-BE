package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.trustify.trustify.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "txn_ref", unique = true, nullable = false, length = 50)
    private String txnRef;

    @Column(name = "vnp_transaction_no", length = 50)
    private String vnpTransactionNo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "order_info", length = 255)
    private String orderInfo;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Column(name = "pay_date", length = 20)
    private String payDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "response_code", length = 10)
    private String responseCode;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    // Company ID (không dùng @ManyToOne để tránh sửa Company)
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    // Plan ID (không dùng @ManyToOne để tránh sửa Plan)
    @Column(name = "plan_id", nullable = false)
    private Long planId;

    // Subscription được tạo sau khi thanh toán thành công
    @OneToOne(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Subscription subscription;

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}