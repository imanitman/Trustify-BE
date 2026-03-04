package com.example.demo.service.Business;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.trustify.trustify.dto.Req.AnalysisRequest;
import com.trustify.trustify.dto.Res.AIResponseDTO;
import com.trustify.trustify.dto.Res.AnalysisResponse;
import com.trustify.trustify.entity.Company;
import com.trustify.trustify.entity.CompanyAnalysis;
import com.trustify.trustify.entity.Review;
import com.trustify.trustify.repository.Business.CompanyAnalysisRepository;
import com.trustify.trustify.repository.Business.CompanyRepository;
import com.trustify.trustify.repository.Customer.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIAnalysisService {

    private final OpenAIClient openAIClient;
    private final ReviewRepository reviewRepository;
    private final CompanyRepository companyRepository;
    private final CompanyAnalysisRepository analysisRepository;
    private final ObjectMapper objectMapper;

    private static final String AI_MODEL = "gpt-4o-mini";
    private static final int MAX_REVIEWS = 50;
    private static final int MAX_REVIEW_LENGTH = 500;
    private static final int CACHE_HOURS = 24;

    /* ======================= PUBLIC ======================= */

    @Transactional
    public AnalysisResponse analyzeCompanyReviews(AnalysisRequest request) {

        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        if (!Boolean.TRUE.equals(request.getForceRefresh())) {
            Optional<CompanyAnalysis> cached = getCachedAnalysis(company.getId());
            if (cached.isPresent()) {
                return buildAnalysisResponse(cached.get(), company, true);
            }
        }

        List<Review> reviews = getReviewsForAnalysis(request);
        if (reviews.isEmpty()) {
            return buildEmptyResponse(company);
        }

        String reviewsText = prepareReviewsForAI(reviews);

        AIResponseDTO aiResult = callAI(
                company.getName(),
                reviewsText,
                reviews.size()
        );

        CompanyAnalysis saved = saveAnalysisToDB(company, reviews.size(), aiResult);
        return buildAnalysisResponse(saved, company, false);
    }

    @Async
    public CompletableFuture<AnalysisResponse> analyzeCompanyReviewsAsync(AnalysisRequest request) {
        return CompletableFuture.completedFuture(analyzeCompanyReviews(request));
    }

    /* ======================= CACHE ======================= */

    private Optional<CompanyAnalysis> getCachedAnalysis(Long companyId) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(CACHE_HOURS);
        return analysisRepository
                .findFirstByCompanyIdAndAnalyzedAtAfterOrderByAnalyzedAtDesc(companyId, threshold);
    }

    /* ======================= REVIEWS ======================= */

    private List<Review> getReviewsForAnalysis(AnalysisRequest request) {

        int limit = Math.min(
                request.getMaxReviews() != null ? request.getMaxReviews() : MAX_REVIEWS,
                MAX_REVIEWS
        );

        List<Review> reviews =
                reviewRepository.findByCompanyIdOrderByCreatedAtDesc(request.getCompanyId());

        if (request.getDateRange() != null && !"all".equalsIgnoreCase(request.getDateRange())) {
            LocalDateTime cutoff = calculateDateRangeCutoff(request.getDateRange());
            reviews = reviews.stream()
                    .filter(r -> r.getCreatedAt() != null)
                    .filter(r -> r.getCreatedAt().isAfter(cutoff))
                    .collect(Collectors.toList());
        }

        return reviews.stream().limit(limit).collect(Collectors.toList());
    }

    private LocalDateTime calculateDateRangeCutoff(String range) {
        LocalDateTime now = LocalDateTime.now();
        return switch (range.toLowerCase()) {
            case "7days", "last_7_days" -> now.minusDays(7);
            case "30days", "last_30_days" -> now.minusDays(30);
            case "6months", "last_6_months" -> now.minusMonths(6);
            case "year", "last_year" -> now.minusYears(1);
            default -> LocalDateTime.MIN;
        };
    }

    private String prepareReviewsForAI(List<Review> reviews) {
        return reviews.stream()
                .filter(review -> review.getDescription() != null && !review.getDescription().isEmpty())
                .map(review -> {
                    String description = review.getDescription();
                    if (description.length() > MAX_REVIEW_LENGTH) {
                        description = description.substring(0, MAX_REVIEW_LENGTH) + "...";
                    }
                    return "Rating: " + review.getRating() + " - " + description;
                })
                .collect(Collectors.joining("\n"));
    }



    /* ======================= OPENAI – ÉP JSON CỨNG ======================= */

    private AIResponseDTO callAI(String companyName, String reviewsText, int totalReviews) {

        String prompt = """
Bạn là hệ thống AI phân tích trải nghiệm khách hàng.

QUY TẮC BẮT BUỘC:
- CHỈ trả về JSON hợp lệ
- KHÔNG markdown
- KHÔNG giải thích
- KHÔNG text ngoài JSON
- JSON PHẢI KHỚP 100%% schema

ENUM:
- overallSentiment: POSITIVE | NEUTRAL | NEGATIVE
- priority: HIGH | MEDIUM | LOW
- sentiment: POSITIVE | NEUTRAL | NEGATIVE

SCHEMA JSON:
{
  "overallScore": number,
  "sentimentSummary": {
    "positivePercent": number,
    "neutralPercent": number,
    "negativePercent": number,
    "overallSentiment": string
  },
  "strengths": [
    {
      "category": string,
      "description": string,
      "score": number,
      "mentionCount": number,
      "sampleReviews": string[]
    }
  ],
  "weaknesses": [
    {
      "category": string,
      "description": string,
      "score": number,
      "mentionCount": number,
      "sampleReviews": string[]
    }
  ],
  "suggestions": [
    {
      "category": string,
      "title": string,
      "description": string,
      "priority": string,
      "expectedImpact": string
    }
  ],
  "topKeywords": [
    {
      "keyword": string,
      "count": number,
      "sentiment": string
    }
  ],
  "aiSummary": string
}

PHÂN TÍCH %d ĐÁNH GIÁ CHO DOANH NGHIỆP "%s":

%s
""".formatted(totalReviews, companyName, reviewsText);

        try {
            ResponseCreateParams params = ResponseCreateParams.builder()
                    .model(AI_MODEL)
                    .temperature(0.0)
                    .maxOutputTokens(2000)
                    .input(prompt) // ✅ CHUẨN 4.0.0
                    .build();

            Response response = openAIClient.responses().create(params);

            String json = extractJsonFromResponse(response);

            return objectMapper.readValue(json, AIResponseDTO.class);

        } catch (Exception e) {
            log.error("AI analysis failed", e);
            throw new RuntimeException("AI JSON parsing failed", e);
        }
    }

    /* ======================= RESPONSE PARSER (CHUẨN 4.0.0) ======================= */

    private String extractJsonFromResponse(Response response) {

        StringBuilder sb = new StringBuilder();

        response.output().forEach(item -> {
            if (item.isMessage()) {
                item.asMessage().content().forEach(content -> {

                    if (content.isOutputText()) {
                        sb.append(content.asOutputText().text());
                    }
                    // nếu muốn log refusal
                    if (content.isRefusal()) {
                        log.warn("AI refusal: {}", content.asRefusal().refusal());
                    }
                });
            }
        });

        String json = sb.toString().trim();

        if (json.isEmpty() || !json.startsWith("{")) {
            log.error("RAW AI OUTPUT: {}", json);
            throw new RuntimeException("AI did not return valid JSON");
        }
        return json;
    }

    /* ======================= DB ======================= */

    private CompanyAnalysis saveAnalysisToDB(
            Company company,
            int reviewCount,
            AIResponseDTO ai
    ) {
        return analysisRepository.save(
                CompanyAnalysis.builder()
                        .company(company)
                        .companyId(company.getId())
                        .reviewsAnalyzed(reviewCount)
                        .overallScore(ai.getOverallScore())
                        .sentimentSummary(ai.getSentimentSummary())
                        .strengths(mapAnalysisPoints(ai.getStrengths()))
                        .weaknesses(mapAnalysisPoints(ai.getWeaknesses()))
                        .suggestions(ai.getSuggestions().stream()
                                .map(s -> CompanyAnalysis.Suggestion.builder()
                                        .category(s.getCategory())
                                        .title(s.getTitle())
                                        .description(s.getDescription())
                                        .priority(s.getPriority())
                                        .expectedImpact(s.getExpectedImpact())
                                        .build())
                                .toList())
                        .topKeywords(ai.getTopKeywords().stream()
                                .map(k -> CompanyAnalysis.KeywordFrequency.builder()
                                        .keyword(k.getKeyword())
                                        .count(k.getCount())
                                        .sentiment(k.getSentiment())
                                        .build())
                                .toList())
                        .aiSummary(ai.getAiSummary())
                        .aiModel(AI_MODEL)
                        .analyzedAt(LocalDateTime.now())
                        .build()
        );
    }

    private List<CompanyAnalysis.AnalysisPoint> mapAnalysisPoints(
            List<AIResponseDTO.AnalysisPoint> points
    ) {
        return points.stream()
                .map(p -> CompanyAnalysis.AnalysisPoint.builder()
                        .category(p.getCategory())
                        .description(p.getDescription())
                        .score(p.getScore())
                        .mentionCount(p.getMentionCount())
                        .sampleReviews(
                                p.getSampleReviews() != null
                                        ? p.getSampleReviews()
                                        : new ArrayList<>()
                        )
                        .build())
                .toList();
    }

    /* ======================= RESPONSE ======================= */

    private AnalysisResponse buildAnalysisResponse(
            CompanyAnalysis analysis,
            Company company,
            boolean cached
    ) {
        return AnalysisResponse.builder()
                .status("SUCCESS")
                .companyId(company.getId())
                .companyName(company.getName())
                .overallScore(analysis.getOverallScore())
                .reviewsAnalyzed(analysis.getReviewsAnalyzed())
                .sentimentSummary(analysis.getSentimentSummary())
                .strengths(analysis.getStrengths())
                .weaknesses(analysis.getWeaknesses())
                .suggestions(analysis.getSuggestions())
                .topKeywords(analysis.getTopKeywords())
                .aiSummary(analysis.getAiSummary())
                .analyzedAt(analysis.getAnalyzedAt().toString())
                .cached(cached)
                .build();
    }

    private AnalysisResponse buildEmptyResponse(Company company) {
        return AnalysisResponse.builder()
                .status("NO_DATA")
                .companyId(company.getId())
                .companyName(company.getName())
                .message("Không có review để phân tích")
                .build();
    }
}
