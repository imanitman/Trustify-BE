package com.example.demo.dto.Req;

import lombok.Data;

@Data
public class AnalysisRequest {
    private Long companyId;
    private Integer maxReviews;      // Số review tối đa để phân tích (default: 50)
    private String dateRange;        // "last_7_days", "last_30_days", "last_6_months", "last_year", "all"
    private Boolean includeReplies;  // Có phân tích cả replies không (default: false)
    private Boolean forceRefresh;    // Bỏ qua cache và phân tích lại (default: false)
}