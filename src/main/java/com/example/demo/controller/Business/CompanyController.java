package com.example.demo.controller.Business;

import com.trustify.trustify.dto.Req.CompanyDto;
import com.trustify.trustify.dto.Req.InviteRequestDto;
import com.trustify.trustify.dto.Req.UpdateCompanyInfoDto;
import com.trustify.trustify.dto.Res.ResSubscription;
import com.trustify.trustify.entity.*;
import com.trustify.trustify.service.Business.CompanyService;
import com.trustify.trustify.service.Business.ProductService;
import com.trustify.trustify.service.Business.UserBusinessService;
import com.trustify.trustify.service.EmailService;
import com.trustify.trustify.service.ImageService;
import com.trustify.trustify.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Slf4j

public class CompanyController {

    private final CompanyService companyService;
    private final JwtUtil jwtUtil;
    private final ImageService imageService;
    private final UserBusinessService userBusinessService;
    private final EmailService emailService;
    private final ProductService productService;

    @GetMapping("/test-hosting")
    public String testHosting() {
        return "Hosting is working!";
    }


    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody CompanyDto companyDto) {
        Map<String, Object> response = new HashMap<>();
        try {
            companyService.createPendingCompany(companyDto);
            response.put("success", true);
            response.put("message", "Verification code sent to " + companyDto.getWorkEmail());
            response.put("email", companyDto.getWorkEmail());
            response.put("expiresIn", "5 minutes");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error registering company", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(
            @RequestParam String email,
            @RequestParam String code) {

        Map<String, Object> response = new HashMap<>();

        try {
            Company company = companyService.verifyAndSaveCompany(email, code);
            response.put("success", true);
            response.put("message", "Company verified successfully!");
            response.put("companyId", company.getId());
            response.put("companyName", company.getName());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error verifying company", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/resend-code")
    public ResponseEntity<Map<String, Object>> resendCode(@RequestParam String email) {

        Map<String, Object> response = new HashMap<>();

        try {
            companyService.resendVerificationCode(email);

            response.put("success", true);
            response.put("message", "Verification code resent to " + email);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error resending verification code", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Map<String, Object> response = new HashMap<>();

        try {
            Page<Company> companyPage = companyService.getAllCompanies(page, size);

            response.put("success", true);
            response.put("companies", companyPage.getContent()); // List<Company>
            response.put("currentPage", companyPage.getNumber());
            response.put("totalPages", companyPage.getTotalPages());
            response.put("totalItems", companyPage.getTotalElements());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting companies", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    @PostMapping("/{companyId}/send-invite")
    public ResponseEntity<?> sendInvite(
            @PathVariable Long companyId,
            @RequestParam String to,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String body
    ) {
        if (to == null || to.isBlank()) {
            return ResponseEntity.badRequest().body("`to` is required");
        }
        // Simple configurable review link; third-party can replace with real frontend URL.
        String reviewLink = String.format("https://trustify-pied.vercel.app/review?companyId=%d", companyId);
        String finalSubject = (subject == null || subject.isBlank()) ?
                "Please review the company" : subject;
        String finalBody = (body == null ? "" : body + "\n\n") +
                "Click to leave a review: " + reviewLink;

        try {
            emailService.sendSimpleEmail(to, finalSubject, finalBody);
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "sent");
            resp.put("to", to);
            resp.put("reviewLink", reviewLink);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send invite: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    @PostMapping("/invite/product/{companyId}")
    public ResponseEntity<?> sendInviteWithProduct(
            @javax.validation.Valid @RequestBody InviteRequestDto req,
            @PathVariable Long companyId,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body("Invalid payload: 'to' and 'productLink' are required and 'to' must be a valid email");
        }
        String reviewLink = req.getProductLink();
        String finalSubject = (req.getSubject() == null || req.getSubject().isBlank())
                ? "Please review this product" : req.getSubject();
        String finalBody = (req.getBody() == null ? "" : req.getBody() + "\n\n")
                + "Click to leave a review: " + reviewLink;
        Company company = companyService.getCompanyById(companyId);
        Product product = new Product();
        product.setName(req.getName());
        product.setUrlProduct(req.getProductLink());
        product.setEmail(req.getTo());
        product.setProductCode(req.getProductCode());
        product.setCompany(company);
        productService.saveProduct(product);
        try {
            emailService.sendSimpleEmail(req.getTo(), finalSubject, finalBody);
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "sent");
            resp.put("to", req.getTo());
            resp.put("productLink", reviewLink);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send invite: " + e.getMessage());
        }
    }
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    @GetMapping("/my-companies")
    public ResponseEntity<Map<String, Object>> getCompanyById(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("access_token".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }
        if (token == null) {
            response.put("success", false);
            response.put("error", "Missing access token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        try {
            // Extract userId from JWT
            Long userId = jwtUtil.extractUserId(token);

            // FIX: Lấy UserBusiness trước, sau đó lấy Company từ UserBusiness
            UserBusiness userBusiness = userBusinessService.getUserBusinessById(userId);
            if (userBusiness == null) {
                response.put("success", false);
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Company company = userBusiness.getCompany();
            if (company == null) {
                response.put("success", false);
                response.put("error", "Company not found for this user");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            response.put("success", true);
            response.put("company", company);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting company by id", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updateCompany(@Valid @RequestBody CompanyDto companyDto, @PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Company company = companyService.updateCompany(companyDto, id);
            response.put("success", true);
            response.put("company",  company);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating company", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    @PostMapping("/{id}/upload-verification")
    public ResponseEntity<Map<String, Object>> uploadVerificationFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Upload file sử dụng ImageService
            ImageService.ImageUploadResult result = imageService.uploadImage(file);
            // Cập nhật Company với file URL và status
            Company company = companyService.updateVerificationFile(id, result.getUrl());
            response.put("success", true);
            response.put("message", "Verification file uploaded successfully");
            response.put("fileUrl", result.getUrl());
            response.put("verifyStatus", company.getVerifyStatus());
            return ResponseEntity.ok(response);

        } catch (ImageService.ImageUploadException e) {
            log.warn("Upload verification file failed: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error uploading verification file", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    @PreAuthorize("hasAnyRole('EMPLOYEE',  'ADMIN')")
    @PutMapping("/update-info/{id}")
    public ResponseEntity<Map<String, Object>> updateCompanyInfo(
            @PathVariable Long id,
            @RequestBody UpdateCompanyInfoDto dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            Company updated = companyService.updateCompanyFields(id, dto);
            response.put("success", true);
            response.put("company", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating company info", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Company>> searchCompanies(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Company> results = companyService.searchCompanies(keyword, page, size);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/timeSubscription/{companyId}")
    public ResponseEntity<List<ResSubscription>> getTimes(@PathVariable Long companyId) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            return ResponseEntity.notFound().build();
        }
        List<ResSubscription> res = new ArrayList<>();
        List<Subscription> subscriptions = company.getSubscriptions();
        if (subscriptions != null) {
            for (Subscription s : subscriptions) {
                if (s != null && s.getPlan() != null) {
                    ResSubscription r = new ResSubscription();
                    r.setPlanName(s.getPlan().getName());
                    r.setStartDate(s.getStartDate());
                    r.setEndDate(s.getEndDate());
                    res.add(r);
                }
            }
        }
        return ResponseEntity.ok(res);
    }

    private List<String> subscriptionCompany(Long companyId) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            return java.util.Collections.emptyList();
        }
        List<Subscription> subscriptions = company.getSubscriptions();
        if (subscriptions == null || subscriptions.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return subscriptions.stream()
                .map(Subscription::getPlan)
                .filter(p -> p != null)
                .map(Plan::getName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toList());
    }

    private List<ResSubscription> subscriptionCompanyRes(Long companyId) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            return java.util.Collections.emptyList();
        }
        List<ResSubscription> resSubscriptions = new ArrayList<>(); // Initialize the list here>
        List<Subscription> subscriptions = company.getSubscriptions();
        for (Subscription subscription : subscriptions) {
            if (subscription != null) {
                ResSubscription resSubscription = new ResSubscription();
                resSubscription.setPlanName(subscription.getPlan().getName());
                resSubscription.setStartDate(subscription.getStartDate());
                resSubscription.setEndDate(subscription.getEndDate());
                resSubscriptions.add(resSubscription);
            }
        }
        return resSubscriptions;
    }
}