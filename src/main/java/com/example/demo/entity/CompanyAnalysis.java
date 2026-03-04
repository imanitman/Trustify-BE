package com.example.demo.entity;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    // Số lượng reviews được phân tích
    @Column(name = "reviews_analyzed")
    private Integer reviewsAnalyzed;

    // Điểm đánh giá tổng quan (1-10)
    @Column(name = "overall_score")
    private Double overallScore;

    // Điểm mạnh (JSON array)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<AnalysisPoint> strengths;

    // Điểm yếu cần cải thiện (JSON array)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<AnalysisPoint> weaknesses;

    // Xu hướng đánh giá theo thời gian
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<TrendData> trends;

    // Đề xuất cải thiện
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<Suggestion> suggestions;

    // Phân tích sentiment tổng quan
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private SentimentSummary sentimentSummary;

    // Các từ khóa được nhắc đến nhiều
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<KeywordFrequency> topKeywords;

    // Tóm tắt AI
    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    // Thời điểm phân tích
    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    // Phiên bản model AI sử dụng
    @Column(name = "ai_model")
    private String aiModel;

    // ===== INNER CLASSES (cho JSON) =====

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnalysisPoint {
        private String category;
        private String description;
        private Double score;
        private Integer mentionCount;     // This is missing in your constructor
        private List<String> sampleReviews;  // This is missing in your constructor
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrendData {
        private String period;        // VD: "2024-Q4", "2024-12"
        private Double averageRating;
        private Integer reviewCount;
        private String sentiment;     // POSITIVE, NEGATIVE, NEUTRAL
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Suggestion {
        private String category;
        private String title;
        private String description;
        private String priority;      // HIGH, MEDIUM, LOW
        private String expectedImpact;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SentimentSummary {
        private Double positivePercent;
        private Double neutralPercent;
        private Double negativePercent;
        private String overallSentiment;  // POSITIVE, NEUTRAL, NEGATIVE
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KeywordFrequency {
        private String keyword;
        private Integer count;
        private String sentiment;
    }

    @ManyToOne
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;
}
