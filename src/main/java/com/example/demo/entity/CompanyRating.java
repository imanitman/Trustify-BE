package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Data
public class CompanyRating {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id; // Same as company_id
        // ===== RELATIONSHIP =====
        @OneToOne(fetch = FetchType.LAZY, optional = false)
        @MapsId
        @JoinColumn(name = "company_id", foreignKey = @ForeignKey(name = "fk_rating_company"))
        @JsonIgnore
        private Company company;

        // ===== OVERALL STATISTICS =====
        @Column(name = "total_reviews", nullable = false)
        @Builder.Default
        private Integer totalReviews = 0;
        @Column(name = "average_rating", columnDefinition = "DECIMAL(3,2)", nullable = false)
        @Builder.Default
        private BigDecimal averageRating = BigDecimal.ZERO;

        // ===== RATING DISTRIBUTION (1-5 stars) =====

        @Column(name = "rating_1_count", nullable = false)
        @Builder.Default
        private Integer rating1Count = 0;
        @Column(name = "rating_2_count", nullable = false)
        @Builder.Default
        private Integer rating2Count = 0;
        @Column(name = "rating_3_count", nullable = false)
        @Builder.Default
        private Integer rating3Count = 0;
        @Column(name = "rating_4_count", nullable = false)
        @Builder.Default
        private Integer rating4Count = 0;
        @Column(name = "rating_5_count", nullable = false)
        @Builder.Default
        private Integer rating5Count = 0;

        // ===== PERCENTAGE DISTRIBUTION =====

        @Column(name = "rating_1_percentage", columnDefinition = "DECIMAL(5,2)")
        @Builder.Default
        private BigDecimal rating1Percentage = BigDecimal.ZERO;
        @Column(name = "rating_2_percentage", columnDefinition = "DECIMAL(5,2)")
        @Builder.Default
        private BigDecimal rating2Percentage = BigDecimal.ZERO;
        @Column(name = "rating_3_percentage", columnDefinition = "DECIMAL(5,2)")
        @Builder.Default
        private BigDecimal rating3Percentage = BigDecimal.ZERO;
        @Column(name = "rating_4_percentage", columnDefinition = "DECIMAL(5,2)")
        @Builder.Default
        private BigDecimal rating4Percentage = BigDecimal.ZERO;
        @Column(name = "rating_5_percentage", columnDefinition = "DECIMAL(5,2)")
        @Builder.Default
        private BigDecimal rating5Percentage = BigDecimal.ZERO;

        // ===== TIME-BASED STATISTICS =====

        @Column(name = "reviews_last_30_days", nullable = false)
        @Builder.Default
        private Integer reviewsLast30Days = 0;

        @Column(name = "reviews_last_90_days", nullable = false)
        @Builder.Default
        private Integer reviewsLast90Days = 0;

        @Column(name = "reviews_last_year", nullable = false)
        @Builder.Default
        private Integer reviewsLastYear = 0;

        @Column(name = "average_rating_last_30_days", columnDefinition = "DECIMAL(3,2)")
        @Builder.Default
        private BigDecimal averageRatingLast30Days = BigDecimal.ZERO;

        @Column(name = "average_rating_last_90_days", columnDefinition = "DECIMAL(3,2)")
        @Builder.Default
        private BigDecimal averageRatingLast90Days = BigDecimal.ZERO;

        // ===== VERIFIED REVIEWS =====

        @Column(name = "verified_reviews_count", nullable = false)
        @Builder.Default
        private Integer verifiedReviewsCount = 0;

        @Column(name = "verified_average_rating", columnDefinition = "DECIMAL(3,2)")
        @Builder.Default
        private BigDecimal verifiedAverageRating = BigDecimal.ZERO;

        // ===== RESPONSE RATE =====

        @Column(name = "total_replies", nullable = false)
        @Builder.Default
        private Integer totalReplies = 0;

        @Column(name = "response_rate", columnDefinition = "DECIMAL(5,2)")
        @Builder.Default
        private BigDecimal responseRate = BigDecimal.ZERO; // Percentage

        @Column(name = "average_response_time_hours", columnDefinition = "DECIMAL(8,2)")
        @Builder.Default
        private BigDecimal averageResponseTimeHours = BigDecimal.ZERO;

        // ===== RECOMMENDATION =====

        @Column(name = "recommendation_score", columnDefinition = "DECIMAL(5,2)")
        @Builder.Default
        private BigDecimal recommendationScore = BigDecimal.ZERO; // NPS-like score

        // ===== HELPFUL REVIEWS =====
        @Column(name = "most_helpful_review_id")
        private Long mostHelpfulReviewId;
        @Column(name = "most_recent_review_id")
        private Long mostRecentReviewId;
        // ===== TREND INDICATORS =====

        @Column(name = "rating_trend", length = 20)
        private String ratingTrend; // IMPROVING, DECLINING, STABLE
        @Column(name = "trend_percentage", columnDefinition = "DECIMAL(5,2)")
        private BigDecimal trendPercentage; // +5.2% or -3.1%

        // ===== TRUST SCORE (Giống TrustPilot) =====

        @Column(name = "trust_score", columnDefinition = "DECIMAL(3,1)")
        @Builder.Default
        private BigDecimal trustScore = BigDecimal.ZERO; // 0.0 - 10.0
        @Column(name = "trust_level", length = 20)
        private String trustLevel; // EXCELLENT, GREAT, AVERAGE, POOR, BAD

        // ===== TIMESTAMPS =====

        @Column(name = "first_review_at")
        private LocalDateTime firstReviewAt;
        @Column(name = "last_review_at")
        private LocalDateTime lastReviewAt;

        @UpdateTimestamp
        @Column(name = "updated_at", nullable = false)
        private LocalDateTime updatedAt;
}
