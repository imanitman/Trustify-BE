package com.example.demo.controller.Business;

import com.trustify.trustify.dto.Req.AnalysisRequest;
import com.trustify.trustify.dto.Res.AnalysisResponse;
import com.trustify.trustify.entity.Company;
import com.trustify.trustify.entity.Plan;
import com.trustify.trustify.entity.Subscription;
import com.trustify.trustify.service.Business.AIAnalysisService;
import com.trustify.trustify.service.Business.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('EMPLOYEE')")
public class AIAnalysisController {

    private final AIAnalysisService aiAnalysisService;
    private final CompanyService companyService;

    @PostMapping("/companies/{companyId}/analyze")
    public ResponseEntity<AnalysisResponse> analyzeSync(
            @PathVariable Long companyId,
            @Valid @RequestBody AnalysisRequest request
    ) {
        List<String> plans = subscriptionCompany(companyId);
        if (plans.isEmpty()) {
            return ResponseEntity.status(403).build();
        }
        request.setCompanyId(companyId);
        log.info("Requesting synchronous analysis for companyId={}", companyId);
        AnalysisResponse response = aiAnalysisService.analyzeCompanyReviews(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/companies/{companyId}/analyze/async")
    public CompletableFuture<ResponseEntity<AnalysisResponse>> analyzeAsync(
            @PathVariable Long companyId,
            @RequestBody AnalysisRequest request
    ) {
        request.setCompanyId(companyId);
        log.info("Requesting asynchronous analysis for companyId={}", companyId);
        return aiAnalysisService
                .analyzeCompanyReviewsAsync(request)
                .thenApply(ResponseEntity::ok);
    }
    private List<String> subscriptionCompany(Long companyId) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            return java.util.Collections.emptyList();
        }
        List<Subscription> subscriptions = company.getSubscriptions();
        if (subscriptions == null || subscriptions.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return subscriptions.stream()
                .map(Subscription::getPlan)
                .filter(p -> p != null)
                .map(Plan::getName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toList());
    }
}
