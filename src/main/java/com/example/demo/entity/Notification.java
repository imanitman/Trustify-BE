package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.trustify.trustify.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String message;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", nullable = true, foreignKey = @ForeignKey(name = "fk_notification_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_business_id", nullable = true, foreignKey = @ForeignKey(name = "fk_notification_user_business"))
    private UserBusiness userBusiness;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "company_id", nullable = true, foreignKey = @ForeignKey(name = "fk_notification_company"))
    @JsonIgnore
    private Company company;
}
