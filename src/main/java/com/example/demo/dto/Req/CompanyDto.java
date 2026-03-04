package com.example.demo.dto.Req;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompanyDto {

    private static final long serialVersionUID = 1L;
    private String name;
    private String websiteUrl;
    private String avatarUrl;
    private String jobTitle;
    private String contactPhone;
    private String industry;
    private String workEmail;
    private String companySize;
    private Integer foundedYear;
    private String country;
    private String annualRevenue;
}
