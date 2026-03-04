package com.example.demo.dto.Res;

import com.trustify.trustify.entity.CompanyAnalysis;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AnalysisResponse {
    private String status;
    private String message;
    private Long companyId;
    private String companyName;

    // Tổng quan
    private Double overallScore;
    private Integer reviewsAnalyzed;
    private CompanyAnalysis.SentimentSummary sentimentSummary;

    // Chi tiết
    private List<CompanyAnalysis.AnalysisPoint> strengths;
    private List<CompanyAnalysis.AnalysisPoint> weaknesses;
    private List<CompanyAnalysis.Suggestion> suggestions;
    private List<CompanyAnalysis.TrendData> trends;
    private List<CompanyAnalysis.KeywordFrequency> topKeywords;

    // AI Summary
    private String aiSummary;
    private String analyzedAt;
    
    // Cache status
    private Boolean cached;
}
