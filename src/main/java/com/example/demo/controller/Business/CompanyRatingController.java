package com.example.demo.controller.Business;

import com.trustify.trustify.entity.Company;
import com.trustify.trustify.entity.CompanyRating;
import com.trustify.trustify.service.Business.CompanyRatingService;
import com.trustify.trustify.service.Business.CompanyService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/company-rating")
public class CompanyRatingController {

        private final CompanyRatingService companyRatingService;
        private final CompanyService companyService;
        @PostMapping("/{companyId}/{rate}")
        public String rateCompany(@PathVariable("companyId") Long companyId , @PathVariable("rate") int rate) {
                CompanyRating rating = companyRatingService.calculateRating(companyId,rate);
                Company company = companyService.getCompanyById(companyId);
                rating.setCompany(company);
                companyRatingService.newRating(rating);
                return "success";
        }
}
