package com.example.demo.dto.Req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCompanyInfoDto {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 2000)
    private String address;

    @Pattern(
            regexp = "^(https?://)?(www\\.)?[a-zA-Z0-9-]+(\\.[a-zA-Z]{2,})+(/.*)?$",
            message = "Invalid website URL"
    )
    private String websiteUrl;

    @Size(max = 100)
    private String industry;

    @Size(max = 50)
    private String companySize;

    @Size(max = 20000)
    private String description;
}
