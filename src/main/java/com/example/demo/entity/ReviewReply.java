package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Builder
@Entity
@Getter
@Setter
public class ReviewReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== RELATIONSHIPS =====
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_reply_review"))
    private Review review;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reply_company"))
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reply_user"))
    private User user;

    // ===== CONTENT =====

    @NotBlank(message = "Reply content is required")
    @Size(min = 10, max = 5000, message = "Reply must be between 10 and 5000 characters")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // ===== METADATA =====

    @Column(name = "is_edited", nullable = false)
    @Builder.Default
    private Boolean isEdited = false;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "is_visible", nullable = false)
    @Builder.Default
    private Boolean isVisible = true; // Company có thể ẩn reply

    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean isPinned = false; // Pin reply để hiển thị đầu tiên

    // ===== ENGAGEMENT METRICS =====

    @Column(name = "helpful_count", nullable = false)
    @Builder.Default
    private Integer helpfulCount = 0;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    // ===== ADMIN/MODERATION =====

    @Column(name = "is_flagged", nullable = false)
    @Builder.Default
    private Boolean isFlagged = false; // Bị báo cáo vi phạm

    @Column(name = "flag_reason", length = 500)
    private String flagReason;

    @Column(name = "flagged_at")
    private LocalDateTime flaggedAt;

    // ===== TIMESTAMPS =====

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
