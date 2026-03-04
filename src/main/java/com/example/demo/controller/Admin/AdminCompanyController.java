package com.example.demo.controller.Admin;

import com.trustify.trustify.entity.Company;
import com.trustify.trustify.enums.VerifyStatus;
import com.trustify.trustify.service.Business.CompanyService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/company")
@AllArgsConstructor
@Slf4j
public class AdminCompanyController {

    private final CompanyService companyService;

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllCompany(
            @RequestParam (defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            Page<Company> companyPage = companyService.getAllCompanies(page, size); // List<Company>
            response.put("success", true);
            response.put("companies", companyPage.getContent()); // List<Company>
            response.put("currentPage", companyPage.getNumber());
            response.put("totalPages", companyPage.getTotalPages());
            response.put("totalItems", companyPage.getTotalElements());
            return ResponseEntity.ok(response);
        }catch (Exception e) {
            log.error("Error getting companies", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyCompany(
            @RequestParam String email,
            @RequestParam String code
    ) {
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

    @GetMapping("/pending-verification")
    public ResponseEntity<Map<String, Object>> getPendingVerificationCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            Page<Company> companyPage = companyService.getCompaniesByVerifyStatus(
                    VerifyStatus.PENDING, page, size);
            response.put("success", true);
            response.put("companies", companyPage.getContent());
            response.put("currentPage", companyPage.getNumber());
            response.put("totalPages", companyPage.getTotalPages());
            response.put("totalItems", companyPage.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting pending verification companies", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveCompany(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Company company = companyService.approveCompanyVerification(id);
            response.put("success", true);
            response.put("message", "Company approved successfully");
            response.put("companyId", company.getId());
            response.put("companyName", company.getName());
            response.put("verifyStatus", company.getVerifyStatus());
            response.put("isVerified", company.getIsVerified());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error approving company", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectCompany(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        Map<String, Object> response = new HashMap<>();
        try {
            Company company = companyService.rejectCompanyVerification(id, reason);
            response.put("success", true);
            response.put("message", "Company rejected");
            response.put("companyId", company.getId());
            response.put("companyName", company.getName());
            response.put("verifyStatus", company.getVerifyStatus());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error rejecting company", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
