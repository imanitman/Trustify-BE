package com.example.demo.service.Business;

import com.trustify.trustify.entity.Company;
import com.trustify.trustify.entity.CompanyRating;
import com.trustify.trustify.repository.Business.CompanyRatingRepository;
import com.trustify.trustify.repository.Business.CompanyRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@AllArgsConstructor
public class CompanyRatingService {

    private final CompanyRatingRepository companyRatingRepository;
    private final CompanyRepository companyRepository;

    public CompanyRating newRating(CompanyRating rating) {
        return companyRatingRepository.save(rating);
    }

    public CompanyRating calculateRating(Long companyId, int rating) {
        // Load company trước
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        // Lấy rating hiện tại hoặc tạo mới
        CompanyRating companyRating = companyRatingRepository.findById(companyId)
                .orElseGet(() -> initializeNewRating(company));

        // Trường hợp totalReviews = 0 (rating lần đầu)
        if (companyRating.getTotalReviews() == 0) {
            companyRating.setAverageRating(BigDecimal.valueOf(rating));
            companyRating.setTotalReviews(1);
            incrementRatingCount(companyRating, rating);
            setAllPercentages(companyRating);
            return companyRatingRepository.save(companyRating);
        }

        // Tính average rating mới
        BigDecimal totalPoints = companyRating.getAverageRating()
                .multiply(BigDecimal.valueOf(companyRating.getTotalReviews()))
                .add(BigDecimal.valueOf(rating));

        int newTotalReviews = companyRating.getTotalReviews() + 1;
        companyRating.setAverageRating(
                totalPoints.divide(BigDecimal.valueOf(newTotalReviews), 2, RoundingMode.HALF_UP)
        );
        companyRating.setTotalReviews(newTotalReviews);

        // Tăng count cho rating tương ứng
        incrementRatingCount(companyRating, rating);

        // Tính phần trăm cho từng rating
        setAllPercentages(companyRating);

        return companyRatingRepository.save(companyRating);
    }

    private CompanyRating initializeNewRating(Company company) {
        CompanyRating rating = new CompanyRating();
        rating.setCompany(company); // QUAN TRỌNG: Phải set company để có ID
        rating.setTotalReviews(0);
        rating.setAverageRating(BigDecimal.ZERO);
        rating.setRating1Count(0);
        rating.setRating2Count(0);
        rating.setRating3Count(0);
        rating.setRating4Count(0);
        rating.setRating5Count(0);
        return rating;
    }

    private void incrementRatingCount(CompanyRating companyRating, int rating) {
        switch (rating) {
            case 1 -> companyRating.setRating1Count(companyRating.getRating1Count() + 1);
            case 2 -> companyRating.setRating2Count(companyRating.getRating2Count() + 1);
            case 3 -> companyRating.setRating3Count(companyRating.getRating3Count() + 1);
            case 4 -> companyRating.setRating4Count(companyRating.getRating4Count() + 1);
            case 5 -> companyRating.setRating5Count(companyRating.getRating5Count() + 1);
        }
    }

    private void setAllPercentages(CompanyRating companyRating) {
        BigDecimal total = BigDecimal.valueOf(companyRating.getTotalReviews());
        BigDecimal hundred = BigDecimal.valueOf(100);

        companyRating.setRating1Percentage(
                BigDecimal.valueOf(companyRating.getRating1Count())
                        .multiply(hundred)
                        .divide(total, 2, RoundingMode.HALF_UP)
        );
        companyRating.setRating2Percentage(
                BigDecimal.valueOf(companyRating.getRating2Count())
                        .multiply(hundred)
                        .divide(total, 2, RoundingMode.HALF_UP)
        );
        companyRating.setRating3Percentage(
                BigDecimal.valueOf(companyRating.getRating3Count())
                        .multiply(hundred)
                        .divide(total, 2, RoundingMode.HALF_UP)
        );
        companyRating.setRating4Percentage(
                BigDecimal.valueOf(companyRating.getRating4Count())
                        .multiply(hundred)
                        .divide(total, 2, RoundingMode.HALF_UP)
        );
        companyRating.setRating5Percentage(
                BigDecimal.valueOf(companyRating.getRating5Count())
                        .multiply(hundred)
                        .divide(total, 2, RoundingMode.HALF_UP)
        );
    }
}
