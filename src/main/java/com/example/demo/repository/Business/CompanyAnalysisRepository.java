package com.example.demo.repository.Business;


import com.trustify.trustify.entity.CompanyAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyAnalysisRepository extends JpaRepository<CompanyAnalysis, Long> {

    /**
     * ⭐ METHOD CHÍNH - Dùng cho cache lookup trong AIAnalysisService
     * Tìm analysis mới nhất của company sau một thời điểm
     */
    Optional<CompanyAnalysis> findFirstByCompanyIdAndAnalyzedAtAfterOrderByAnalyzedAtDesc(
            Long companyId,
            LocalDateTime analyzedAfter
    );

    /**
     * Tìm analysis mới nhất của company
     */
    Optional<CompanyAnalysis> findFirstByCompanyIdOrderByAnalyzedAtDesc(Long companyId);

    /**
     * Lấy tất cả analysis của company, sắp xếp theo thời gian mới nhất
     */
    List<CompanyAnalysis> findByCompanyIdOrderByAnalyzedAtDesc(Long companyId);

    /**
     * Lấy N analysis gần nhất của company (dùng với Pageable)
     */
    @Query("SELECT ca FROM CompanyAnalysis ca " +
            "WHERE ca.company.id = :companyId " +
            "ORDER BY ca.analyzedAt DESC")
    List<CompanyAnalysis> findTopNAnalyses(
            @Param("companyId") Long companyId,
            org.springframework.data.domain.Pageable pageable
    );

    /**
     * Tìm analysis trong khoảng thời gian
     */
    List<CompanyAnalysis> findByCompanyIdAndAnalyzedAtBetweenOrderByAnalyzedAtDesc(
            Long companyId,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * Đếm số lần analysis của company
     */
    long countByCompanyId(Long companyId);

    /**
     * Tìm analysis theo AI model
     */
    List<CompanyAnalysis> findByCompanyIdAndAiModelOrderByAnalyzedAtDesc(
            Long companyId,
            String aiModel
    );

    /**
     * Lấy overall score trung bình của company qua các lần analysis
     */
    @Query("SELECT AVG(ca.overallScore) FROM CompanyAnalysis ca " +
            "WHERE ca.company.id = :companyId")
    Double calculateAverageOverallScore(@Param("companyId") Long companyId);

    /**
     * Tìm các company có overall score trong khoảng
     * (chỉ lấy analysis mới nhất của mỗi company)
     */
    @Query("SELECT ca FROM CompanyAnalysis ca " +
            "WHERE ca.overallScore BETWEEN :minScore AND :maxScore " +
            "AND ca.analyzedAt = (SELECT MAX(ca2.analyzedAt) FROM CompanyAnalysis ca2 " +
            "                     WHERE ca2.company.id = ca.company.id) " +
            "ORDER BY ca.overallScore DESC")
    List<CompanyAnalysis> findCompaniesByScoreRange(
            @Param("minScore") Double minScore,
            @Param("maxScore") Double maxScore
    );

    /**
     * Xóa analysis cũ (trước một thời điểm) - dùng để clean up
     */
    void deleteByAnalyzedAtBefore(LocalDateTime analyzedBefore);

    /**
     * Xóa tất cả analysis của một company
     */
    void deleteByCompanyId(Long companyId);

    /**
     * Kiểm tra company đã có analysis chưa
     */
    boolean existsByCompanyId(Long companyId);

    /**
     * Lấy analysis gần nhất với số reviews được phân tích >= threshold
     */
    @Query("SELECT ca FROM CompanyAnalysis ca " +
            "WHERE ca.company.id = :companyId " +
            "AND ca.reviewsAnalyzed >= :minReviews " +
            "ORDER BY ca.analyzedAt DESC")
    List<CompanyAnalysis> findAnalysesWithMinReviews(
            @Param("companyId") Long companyId,
            @Param("minReviews") Integer minReviews,
            org.springframework.data.domain.Pageable pageable
    );

    /**
     * Thống kê số lượng analysis theo AI model
     */
    @Query("SELECT ca.aiModel, COUNT(ca) FROM CompanyAnalysis ca " +
            "GROUP BY ca.aiModel")
    List<Object[]> countAnalysesByAiModel();

    /**
     * Lấy trend overall score theo thời gian (dùng để vẽ chart)
     */
    @Query("SELECT ca.analyzedAt, ca.overallScore FROM CompanyAnalysis ca " +
            "WHERE ca.company.id = :companyId " +
            "ORDER BY ca.analyzedAt ASC")
    List<Object[]> getScoreTrend(@Param("companyId") Long companyId);

    /**
     * Tìm các company có score thay đổi đáng kể
     * So sánh 2 lần analysis gần nhất
     */
    @Query(value = """
        SELECT ca1.company_id, 
               ca1.overall_score - ca2.overall_score as score_change,
               ca1.overall_score as latest_score,
               ca2.overall_score as previous_score
        FROM company_analysis ca1
        INNER JOIN (
            SELECT company_id, MAX(analyzed_at) as max_date
            FROM company_analysis
            GROUP BY company_id
            HAVING COUNT(*) >= 2
        ) latest ON ca1.company_id = latest.company_id 
                AND ca1.analyzed_at = latest.max_date
        INNER JOIN (
            SELECT ca3.company_id, ca3.overall_score, ca3.analyzed_at
            FROM company_analysis ca3
            INNER JOIN (
                SELECT company_id, analyzed_at,
                       ROW_NUMBER() OVER (PARTITION BY company_id ORDER BY analyzed_at DESC) as rn
                FROM company_analysis
            ) ranked ON ca3.company_id = ranked.company_id 
                     AND ca3.analyzed_at = ranked.analyzed_at
            WHERE ranked.rn = 2
        ) ca2 ON ca1.company_id = ca2.company_id
        WHERE ABS(ca1.overall_score - ca2.overall_score) >= :threshold
        ORDER BY ABS(ca1.overall_score - ca2.overall_score) DESC
        """, nativeQuery = true)
    List<Object[]> findCompaniesWithSignificantScoreChange(@Param("threshold") Double threshold);

    /**
     * Lấy top companies theo overall score
     * (chỉ lấy analysis mới nhất của mỗi company)
     */
    @Query("SELECT ca FROM CompanyAnalysis ca " +
            "WHERE ca.analyzedAt = (SELECT MAX(ca2.analyzedAt) FROM CompanyAnalysis ca2 " +
            "                       WHERE ca2.company.id = ca.company.id) " +  "ORDER BY ca.overallScore DESC")
    List<CompanyAnalysis> findTopRatedCompanies(org.springframework.data.domain.Pageable pageable);

    /**
     * Lấy analysis với sentiment summary cụ thể
     */
    @Query("SELECT ca FROM CompanyAnalysis ca " +
            "WHERE ca.company.id = :companyId " +
            "AND LOWER(CAST(ca.sentimentSummary AS string)) LIKE LOWER(CONCAT('%', :sentiment, '%')) " +
            "ORDER BY ca.analyzedAt DESC")
    List<CompanyAnalysis> findBySentiment(
            @Param("companyId") Long companyId,
            @Param("sentiment") String sentiment
    );

    /**
     * Đếm số analysis theo khoảng overall score
     */
    @Query("SELECT " +
            "CASE " +
            "  WHEN ca.overallScore >= 4.5 THEN 'Excellent' " +
            "  WHEN ca.overallScore >= 3.5 THEN 'Good' " +
            "  WHEN ca.overallScore >= 2.5 THEN 'Average' " +
            "  WHEN ca.overallScore >= 1.5 THEN 'Poor' " +
            "  ELSE 'Very Poor' " +
            "END as scoreRange, " +
            "COUNT(ca) " +
            "FROM CompanyAnalysis ca " +
            "WHERE ca.analyzedAt = (SELECT MAX(ca2.analyzedAt) FROM CompanyAnalysis ca2 " +
            "                       WHERE ca2.company.id = ca.company.id) " +
            "GROUP BY " +
            "CASE " +
            "  WHEN ca.overallScore >= 4.5 THEN 'Excellent' " +
            "  WHEN ca.overallScore >= 3.5 THEN 'Good' " +
            "  WHEN ca.overallScore >= 2.5 THEN 'Average' " +
            "  WHEN ca.overallScore >= 1.5 THEN 'Poor' " +
            "  ELSE 'Very Poor' " +
            "END")
    List<Object[]> countCompaniesByScoreRange();

    /**
     * Tìm companies cần phân tích lại (đã lâu chưa analyze hoặc chưa có analysis)
     */
    @Query(value = """
        SELECT c.id, c.name, 
               COALESCE(MAX(ca.analyzed_at), c.created_at) as last_analyzed
        FROM companies c
        LEFT JOIN company_analysis ca ON c.id = ca.company_id
        WHERE c.is_active = true
        GROUP BY c.id, c.name, c.created_at
        HAVING COALESCE(MAX(ca.analyzed_at), c.created_at) < :threshold
        ORDER BY COALESCE(MAX(ca.analyzed_at), c.created_at) ASC
        """, nativeQuery = true)
    List<Object[]> findCompaniesNeedingAnalysis(@Param("threshold") LocalDateTime threshold);
}