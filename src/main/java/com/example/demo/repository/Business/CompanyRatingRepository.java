package com.example.demo.repository.Business;

import com.trustify.trustify.entity.CompanyRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRatingRepository extends JpaRepository<CompanyRating, Long> {

}
