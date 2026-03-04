package com.example.demo.dto.Res;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.trustify.trustify.entity.CompanyAnalysis;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIResponseDTO {

    private Double overallScore;
    private SentimentSummary sentimentSummary;
    private List<AnalysisPoint> strengths;
    private List<AnalysisPoint> weaknesses;
    private List<Suggestion> suggestions;
    private List<KeywordFrequency> topKeywords;
    private String aiSummary;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "analyzed_at")
    @Builder.Default
    private LocalDateTime analyzedAt = LocalDateTime.now();
    // ===== INNER CLASSES =====

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SentimentSummary extends CompanyAnalysis.SentimentSummary {
        private Double positivePercent;
        private Double neutralPercent;
        private Double negativePercent;
        private String overallSentiment;  // POSITIVE, NEUTRAL, NEGATIVE
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnalysisPoint {
        private String category;      // e.g., "Chất lượng sản phẩm"
        private String description;   // Detailed description
        private Double score;         // Score (1-10)
        private Integer mentionCount; // Number of mentions
        private List<String> sampleReviews; // Sample reviews
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
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
    public static class KeywordFrequency  extends CompanyAnalysis.KeywordFrequency {
        private String keyword;
        private Integer count;
        private String sentiment;  // POSITIVE, NEGATIVE, NEUTRAL
    }
}