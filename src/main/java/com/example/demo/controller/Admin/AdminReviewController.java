package com.example.demo.controller.Admin;

import com.trustify.trustify.entity.Review;
import com.trustify.trustify.enums.ReviewStatus;
import com.trustify.trustify.service.Customer.ReviewService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/review")
@AllArgsConstructor
@Slf4j
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping("/list")
    public ResponseEntity<?> getReviews(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "15") int size) {
        Page<Review> reviews = reviewService.getAllReviews(page, size);
        Map<String, Object> response = new HashMap<>();
        response.put("reviews", reviews.getContent());
        response.put("totalElements", reviews.getTotalElements());
        response.put("totalPages", reviews.getTotalPages());
        response.put("currentPage", reviews.getNumber());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingReviews(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "15") int size) {
        Page<Review> reviews = reviewService.getReviewsByStatus(ReviewStatus.PENDING, page, size);
        Map<String, Object> response = new HashMap<>();
        response.put("reviews", reviews.getContent());
        response.put("totalElements", reviews.getTotalElements());
        response.put("totalPages", reviews.getTotalPages());
        response.put("currentPage", reviews.getNumber());
        response.put("message", "Pending reviews fetched successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{reviewId}/status")
    public ResponseEntity<?> updateReviewStatus(@PathVariable Long reviewId, @RequestParam ReviewStatus status) {
        Review updatedReview = reviewService.updateReviewStatus(reviewId, status);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Review status updated successfully");
        response.put("review", updatedReview);
        return ResponseEntity.ok(response);
    }
}
