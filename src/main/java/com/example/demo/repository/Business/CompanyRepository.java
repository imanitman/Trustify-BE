package com.example.demo.repository.Business;

import com.trustify.trustify.entity.Company;
import com.trustify.trustify.entity.Product;
import com.trustify.trustify.enums.VerifyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByContactEmail(String email);

    Company findByName(String name);

    Page<Company> findByIndustry(String name, Pageable pageable);

    Page<Company> findByVerifyStatus(VerifyStatus verifyStatus, Pageable pageable);

    Page<Company> findByNameContainingIgnoreCaseAndDeletedAtIsNull(
            String name, Pageable pageable);

    Page<Company> findByDeletedAtIsNull(Pageable pageable);

    @Query("select p.company from Product p where p.id = :productId")
    Company getCompanyByProductId(@Param("productId") Long productId);

    @Query("SELECT c FROM Company c " +
            "LEFT JOIN FETCH c.subscriptions s " +
            "LEFT JOIN FETCH s.plan " +
            "WHERE c.id = :id")
    Company findByIdWithSubscriptions(@Param("id") Long id);
}
