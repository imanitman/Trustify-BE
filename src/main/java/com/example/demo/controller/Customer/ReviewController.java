package com.example.demo.controller.Customer;

import com.trustify.trustify.dto.Req.ReviewDto;
import com.trustify.trustify.dto.Res.ResReviewDto;
import com.trustify.trustify.entity.Company;
import com.trustify.trustify.entity.Review;
import com.trustify.trustify.entity.User;
import com.trustify.trustify.service.Business.CompanyService;
import com.trustify.trustify.service.Customer.ReviewService;
import com.trustify.trustify.service.Customer.UserService;
import com.trustify.trustify.service.RedisService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/review")
@AllArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;
    private final RedisService redisService;
    private final CompanyService companyService;

    private void checkUserNotInactive(String email) {
        if (redisService.isUserInactive(email)) {
            throw new IllegalStateException("User is inactive and cannot perform this action");
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createReview(@RequestBody ReviewDto reviewDto) {
        try {
            checkUserNotInactive(reviewDto.getEmail());
            User user = userService.findByEmail(reviewDto.getEmail());
            userService.validateUserCanModifyReview(user);
            Review review = reviewService.createReview(reviewDto);
            ResReviewDto resReviewDto = new ResReviewDto();
            resReviewDto.setId(review.getId());
            resReviewDto.setTitle(review.getTitle());
            resReviewDto.setDescription(review.getDescription());
            resReviewDto.setRating(review.getRating());
            resReviewDto.setNameUser(review.getUser().getName());
            resReviewDto.setNameCompany(review.getCompany().getName());
            resReviewDto.setExpDate(review.getExpDate().toString());
            return ResponseEntity.ok(resReviewDto);
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(403).body(error);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/industry/{name}")
    public ResponseEntity<Map<String, Object>> getCompaniesByIndustry(
            @PathVariable String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size) {
        Map<String, Object> response = new HashMap<>();
        try {
            Page<Company> companyPage = companyService.getCompaniesByIndustry(name, page, size); // List<Company>
            response.put("success", true);
            response.put("companies", companyPage.getContent()); // List<Company>
            response.put("currentPage", companyPage.getNumber());
            response.put("totalPages", companyPage.getTotalPages());
            response.put("totalItems", companyPage.getTotalElements());
            return ResponseEntity.ok(response);
        }catch (Exception e) {
            log.error("Error getting companies by industry", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateReview(@PathVariable Long id, @RequestBody ReviewDto reviewDto) {
        try {
            checkUserNotInactive(reviewDto.getEmail());
            User user = userService.findByEmail(reviewDto.getEmail());
            userService.validateUserCanModifyReview(user);
            return ResponseEntity.ok(reviewService.updateReview(reviewDto));
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(403).body(error);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable Long id, @RequestParam String email) {
        try {
            checkUserNotInactive(email);
            User user = userService.findByEmail(email);
            userService.validateUserCanModifyReview(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Review deleted successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(403).body(error);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // GET methods remain unchanged...
    @GetMapping("/company/{id}")
    public ResponseEntity<Map<String, Object>> getCompaniesByIndustry(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size) {
        Map<String, Object> response = new HashMap<>();
        try {
            Page<Review> reviewPage = reviewService.getReviewsByCompanyId(id, page, size);
            response.put("success", true);

            response.put("reviews", reviewPage.getContent());
            response.put("currentPage", reviewPage.getNumber());
            response.put("totalPages", reviewPage.getTotalPages());
            response.put("totalItems", reviewPage.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting reviews by id", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/user-review")
    public ResponseEntity<Map<String, Object>> getReviewsByUserId(
            @RequestParam(defaultValue = "") String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> response = new HashMap<>();
        try {
            Page<Review> reviewPage = reviewService.getReviewsByEmail(email, page, size);
            response.put("success", true);
            response.put("reviews", reviewPage.getContent());
            response.put("currentPage", reviewPage.getNumber());
            response.put("totalPages", reviewPage.getTotalPages());
            response.put("totalItems", reviewPage.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting reviews by id", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/allReports")
    public ResponseEntity<Map<String, Object>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> response = new HashMap<>();
        try {
            Page<Review> reviewPage = reviewService.getAllReportReviews(page, size);
            response.put("success", true);
            response.put("reviews", reviewPage.getContent());
            response.put("currentPage", reviewPage.getNumber());
            response.put("totalPages", reviewPage.getTotalPages());
            response.put("totalItems", reviewPage.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting reported reviews", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
