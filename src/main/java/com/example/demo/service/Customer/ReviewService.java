package com.example.demo.service.Customer;

import com.trustify.trustify.dto.Req.ReviewDto;
import com.trustify.trustify.entity.Product;
import com.trustify.trustify.entity.Review;
import com.trustify.trustify.entity.User;
import com.trustify.trustify.enums.ReviewStatus;
import com.trustify.trustify.enums.UserStatus;
import com.trustify.trustify.repository.Customer.ReviewRepository;
import com.trustify.trustify.service.Business.CompanyService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@AllArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserService userService;
    private final CompanyService companyService;

    public Page<Review> getReviewsByStatus(ReviewStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewRepository.findByStatus(status, pageable);
    }

    public Review updateReviewStatus(Long reviewId, ReviewStatus status) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + reviewId));
        review.setStatus(status);
        return reviewRepository.save(review);
    }


    public Review createReview(ReviewDto reviewDto) {
        Review review = new Review();
        review.setTitle(reviewDto.getTitle());
        review.setDescription(reviewDto.getDescription());
        review.setEmail(reviewDto.getEmail());
        review.setRating(reviewDto.getRating());
        review.setExpDate(reviewDto.getExpDate());
        User user = userService.findByEmail(reviewDto.getEmail());
        review.setUser(user);
        review.setCompany(companyService.findCompanyByName(reviewDto.getCompanyName()));
        return reviewRepository.save(review);
    }

    public Page<Review> getReviewsByCompanyId(Long companyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<ReviewStatus> statuses = Arrays.asList(ReviewStatus.APPROVED, ReviewStatus.REPORTED);
        return reviewRepository.findByCompanyIdAndStatusIn(companyId, statuses, pageable);
    }


    public Page<Review> getReviewsByEmail(String email, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewRepository.findAllByEmail(email, pageable) ;
    }

    public Review updateReview(ReviewDto reviewDto) {
        Review review = reviewRepository.findById(reviewDto.getId()).orElse(null);
        if (review == null) {
            throw new IllegalArgumentException("Review not found with id: " + reviewDto.getId());
        }
        // Since likes is a boolean field in ReviewDto, check if it's true
        if (reviewDto.isLove()){
            if (reviewDto.isLikes()) {
                if (review.getLikes() == null) {
                    review.setLikes(1L); // Initialize to 1 if null
                } else {
                    review.setLikes(review.getLikes() + 1); // Increment by 1
                }
            }
            else{
                if (review.getLikes() == null) {
                    review.setLikes(null); // Initialize to 1 if null
                } else {
                    review.setLikes(review.getLikes() - 1); // Increment by 1
                }
            }
        }

        if (reviewDto.getTitle() != null) {
            review.setTitle(reviewDto.getTitle());
        }
        if (reviewDto.getReply() != null) {
            review.setReply(reviewDto.getReply());
        }

        if (reviewDto.getStatus() != null) {
            String status = reviewDto.getStatus();
            User user = userService.findByEmail(reviewDto.getEmail());
            if (user.getNumberOfReport() == 0) {
                user.setNumberOfReport(1);
            }
            else{
                if ("reported".equalsIgnoreCase(status)){
                    user.setNumberOfReport(user.getNumberOfReport() + 1);
                        if (user.getNumberOfReport() > 5) {
                            user.setStatus(UserStatus.SUSPENDED);
                        }
                }
            }
            userService.save(user);
            if ("reported".equalsIgnoreCase(status)) {
                Long currentReports = review.getNumberOfReport();
                long newCount = (currentReports == null) ? 1L : currentReports.longValue() + 1L;
                review.setNumberOfReport(newCount); // autobox to Long
                review.setStatus(ReviewStatus.REPORTED);
            } else if ("approved".equalsIgnoreCase(status)) {
                review.setStatus(ReviewStatus.APPROVED);
            } else if ("rejected".equalsIgnoreCase(status)) {
                review.setStatus(ReviewStatus.REJECTED);
            }
        }
        if (reviewDto.getContendReport() != null) {
            review.setContendReport(reviewDto.getContendReport());
        }
        if (reviewDto.getDescription() != null) {
            review.setDescription(reviewDto.getDescription());
        }
        if (reviewDto.getRating() != null) {
            review.setRating(reviewDto.getRating());
        }
        if (reviewDto.getExpDate() != null) {
            review.setExpDate(reviewDto.getExpDate());
        }
        return reviewRepository.save(review);
    }


    public Page<Review> getAllReviews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewRepository.findAll(pageable);
    }

    public Page<Review> getAllReportReviews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewRepository.findByStatus(ReviewStatus.REPORTED, pageable);
    }

    public Object getReviewsByProductId(Long productId) {
        return reviewRepository.findByProductId(productId);
    }

    public void saveReviewProduct(String review, Product product, String email) {
        Review review1 = new Review();
        review1.setDescription(review);
        review1.setProduct(product);
        review1.setEmail(email);
        reviewRepository.save(review1);
    }
}
