package com.example.demo.repository.Customer;

import com.trustify.trustify.entity.Company;
import com.trustify.trustify.entity.Review;
import com.trustify.trustify.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByCompanyId(Long companyId, Pageable pageable);

    @Query("SELECT r FROM Review r " +
            "WHERE r.email = :email " +
            "OR r.user.email = :email " +  // ← Check BOTH!
            "ORDER BY r.createdAt DESC")
    Page<Review> findAllByEmail(String email, Pageable pageable);
    /**
     * Tìm tất cả reviews của một company, sắp xếp theo ngày tạo mới nhất
     */
    List<Review> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    /**
     * Tìm reviews của company trong khoảng thời gian
     */
    List<Review> findByCompanyIdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long companyId,
            LocalDateTime createdAfter
    );
    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);
    Page<Review> findByCompanyIdAndStatusNot(Long companyId, ReviewStatus status, Pageable pageable);
    /**
     * Tìm reviews của company với rating cụ thể
     */
    List<Review> findByCompanyIdAndRatingOrderByCreatedAtDesc(
            Long companyId,
            Integer rating
    );

    /**
     * Đếm số lượng reviews của một company
     */
    long countByCompanyId(Long companyId);

    /**
     * Đếm reviews theo rating
     */
    long countByCompanyIdAndRating(Long companyId, Integer rating);

    /**
     * Tính điểm trung bình của company
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.company.id = :companyId")
    Double calculateAverageRating(@Param("companyId") Long companyId);

    /**
     * Lấy N reviews mới nhất của company
     */
    @Query("SELECT r FROM Review r WHERE r.company.id = :companyId ORDER BY r.createdAt DESC")
    List<Review> findTopNByCompanyIdOrderByCreatedAtDesc(
            @Param("companyId") Long companyId,
            Pageable pageable
    );

    /**
     * Tìm reviews có reply (đã được phản hồi)
     */
    @Query("SELECT r FROM Review r WHERE r.company.id = :companyId AND r.reply IS NOT NULL AND r.reply != ''")
    List<Review> findReviewsWithReply(@Param("companyId") Long companyId);

    /**
     * Tìm reviews chưa có reply
     */
    @Query("SELECT r FROM Review r WHERE r.company.id = :companyId AND (r.reply IS NULL OR r.reply = '')")
    List<Review> findReviewsWithoutReply(@Param("companyId") Long companyId);

    /**
     * Tìm reviews của user cho một company
     */
    List<Review> findByCompanyIdAndUserIdOrderByCreatedAtDesc(Long companyId, Long userId);

    /**
     * Tìm reviews của user
     */
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Kiểm tra user đã review company chưa
     */
    boolean existsByCompanyIdAndUserId(Long companyId, Long userId);

    /**
     * Tìm review cụ thể của user cho company
     */
    Optional<Review> findByCompanyIdAndUserId(Long companyId, Long userId);

    /**
     * Đếm reviews trong khoảng thời gian
     */
    long countByCompanyIdAndCreatedAtBetween(
            Long companyId,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * Lấy reviews theo nhiều criteria
     */
    @Query("SELECT r FROM Review r WHERE r.company.id = :companyId " +
            "AND (:rating IS NULL OR r.rating = :rating) " +
            "AND (:startDate IS NULL OR r.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR r.createdAt <= :endDate) " +
            "ORDER BY r.createdAt DESC")
    List<Review> findReviewsByCriteria(
            @Param("companyId") Long companyId,
            @Param("rating") Integer rating,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Thống kê reviews theo rating
     */
    @Query("SELECT r.rating, COUNT(r) FROM Review r " +
            "WHERE r.company.id = :companyId " +
            "GROUP BY r.rating " +
            "ORDER BY r.rating DESC")
    List<Object[]> countReviewsByRating(@Param("companyId") Long companyId);

    /**
     * Lấy reviews mới nhất có phân trang
     */
    @Query("SELECT r FROM Review r WHERE r.company.id = :companyId " +
            "ORDER BY r.createdAt DESC")
    List<Review> findRecentReviews(
            @Param("companyId") Long companyId,
            Pageable pageable
    );
    Page<Review> findByCompanyIdAndStatus(Long companyId, ReviewStatus status, Pageable pageable);

    Object findByProductId(Long productId);

    Page<Review> findByCompanyIdAndStatusIn(Long companyId, List<ReviewStatus> statuses, Pageable pageable);
}
