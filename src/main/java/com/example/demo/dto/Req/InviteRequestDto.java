// java
package com.example.demo.dto.Req;

import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class InviteRequestDto {
    @NotBlank
    @Email
    private String to;
    @NotBlank
    private String name;
    private String productLink;
    private String subject;
    private String productCode;
    private String body;
}
