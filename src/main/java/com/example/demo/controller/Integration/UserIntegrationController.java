package com.example.demo.controller.Integration;
import com.trustify.trustify.dto.Req.InviteRequestDto;
import com.trustify.trustify.entity.*;
import com.trustify.trustify.repository.Business.CompanyRatingRepository;
import com.trustify.trustify.repository.Business.UserBusinessRepository;
import com.trustify.trustify.service.Business.CompanyService;
import com.trustify.trustify.service.EmailService;
import com.trustify.trustify.service.Customer.ReviewService;
import com.trustify.trustify.service.RedisService;
import com.trustify.trustify.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/integration/companies")
@RequiredArgsConstructor
public class UserIntegrationController {
    private final EmailService emailService;
    private final CompanyRatingRepository companyRatingRepository;
    private final ReviewService reviewService;
    private final CompanyService companyService;
    private final RedisService redisService;
    private final JwtUtil jwtUtil;
    private UserBusiness userBusiness;
    private UserBusinessRepository userBusinessRepository;
    /**
     * Return integration helper information for third-party devs.
     * Contains URLs to call to send email invites and to fetch rating stats,
     * and the current CompanyRating (if present).
     */
    @GetMapping("/{companyId}")
    public ResponseEntity<?> getIntegrationManifest(@PathVariable Long companyId) {
        String base = "/integration/companies";
        Map<String, Object> payload = new HashMap<>();
        payload.put("sendInviteApi", String.format("%s/%d/send-invite", base, companyId));
        payload.put("ratingApi", String.format("%s/%d/rating", base, companyId));
        payload.put("reviewsApi", String.format("%s/%d/reviews", base, companyId));
        CompanyRating rating = companyRatingRepository.findById(companyId).orElse(null);
        payload.put("companyRating", rating); // will be null if missing
        return ResponseEntity.ok(payload);
    }
    /**
     * Send an invite email to the given recipient with a link to review the company.
     * Query/body:
     *  - to (required)
     *  - subject (optional)
     *  - body (optional) : custom text; a review link will be appended
     */
    /**
     * Return the full CompanyRating entity for the company if present.
     * This exposes all rating fields available in `CompanyRating`.
     */
    @GetMapping("/{companyId}/rating")
    public ResponseEntity<?> getCompanyRating(@PathVariable Long companyId) {
        return companyRatingRepository.findById(companyId)
                .map(rating -> ResponseEntity.<Object>ok(rating))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .<Object>body("CompanyRating not found for id: " + companyId));
    }
    /**
     * Return paginated reviews for the given company.
     * Query params:
     *  - page (default 0)
     *  - size (default 20)
     */
    @Transactional(readOnly = true)
    @GetMapping("/{companyId}/reviews")
    public ResponseEntity<?> getCompanyReviews(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<Review> reviews = reviewService.getReviewsByCompanyId(companyId, page, size);

        // Convert to DTO to avoid lazy loading issues
        Page<Map<String, Object>> reviewDtos = reviews.map(review -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", review.getId());
            dto.put("rating", review.getRating());
            dto.put("description", review.getDescription());
            dto.put("email", review.getEmail());
            dto.put("title", review.getTitle());
            dto.put("user", review.getUser().getName());
            dto.put("expDate", review.getExpDate());
            dto.put("createdAt", review.getCreatedAt());
            dto.put("updatedAt", review.getUpdatedAt());
            // Add other fields you need

            // If you need company info, only include basic fields
            if (review.getCompany() != null) {
                Map<String, Object> companyInfo = new HashMap<>();
                companyInfo.put("id", review.getCompany().getId());
                companyInfo.put("name", review.getCompany().getName());
                dto.put("company", companyInfo);
            }

            return dto;
        });

        return ResponseEntity.ok(reviewDtos);
    }

    private List<String> subscriptionCompany(Long companyId) {
        Company company = companyService.getCompanyByIdWithSubscriptions(companyId);
        if (company == null || company.getSubscriptions() == null) {
            return java.util.Collections.emptyList();
        }
        return company.getSubscriptions().stream()
                .map(Subscription::getPlan)
                .filter(p -> p != null)
                .map(Plan::getName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toList());
    }


    @PostMapping("/getCode/{id}")
    public String code(@PathVariable Long id) {
        Company company = companyService.getCompanyById(id);
        if (company != null) {
            List<UserBusiness> userBusiness = company.getUserBusinesses();
            UserBusiness user = userBusiness.get(0);
            if (userBusiness != null) {
                String token = jwtUtil.generateBusinessToken(user);
                String stateCode = UUID.randomUUID().toString();
                redisService.saveStateCode(stateCode, token);
                return stateCode;
            }
        }
        return null;
    }
}
