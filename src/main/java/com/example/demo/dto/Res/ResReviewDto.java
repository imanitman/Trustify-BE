package com.example.demo.dto.Res;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResReviewDto {
    private Long id;
    private String nameUser;
    private String nameCompany;
    private Long numberOfReport;
    private String title;
    private String description;
    private Integer rating;
    private String expDate;
}
