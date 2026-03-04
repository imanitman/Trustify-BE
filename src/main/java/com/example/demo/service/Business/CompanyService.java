package com.example.demo.service.Business;

import com.trustify.trustify.dto.Req.CompanyDto;
import com.trustify.trustify.dto.Req.UpdateCompanyInfoDto;
import com.trustify.trustify.entity.Company;
import com.trustify.trustify.entity.UserBusiness;
import com.trustify.trustify.enums.BusinessRole;
import com.trustify.trustify.enums.VerifyStatus;
import com.trustify.trustify.repository.Business.CompanyRepository;
import com.trustify.trustify.service.EmailService;
import com.trustify.trustify.service.RedisService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final RedisService redisService;
    private final EmailService emailService;
    private final UserBusinessService userBusinessService;

    public Page<Company> getAllCompanies(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return companyRepository.findAll(pageable);
    }

    public Company findCompanyByName(String name) {
        return companyRepository.findByName(name);
    }
    /**
     * Convert DTO sang Entity (giữ nguyên)
     */
    public Company companyFromDto(CompanyDto companyDto) {
        Company company = new Company();
        company.setSlug("Welcome to " + companyDto.getName());
        company.setName(companyDto.getName());
        company.setWebsiteUrl(companyDto.getWebsiteUrl());
        company.setContactPhone(companyDto.getContactPhone());
        company.setContactEmail(companyDto.getWorkEmail());
        company.setIndustry(companyDto.getIndustry());
        company.setCompanySize(companyDto.getCompanySize());
        company.setFoundedYear(companyDto.getFoundedYear());
        company.setCountry(companyDto.getCountry());
        company.setAnnualRevenue(companyDto.getAnnualRevenue());
        company.setIsVerified(false); // Mặc định chưa verify
        return company;
    }
    public void cancelPendingRegistration(String email) {
        redisService.deletePendingCompany(email);
        log.info("Cancelled pending registration for: {}", email);
    }

    /**
     * Step 1: Nhận DTO → Convert sang Entity → Lưu Redis → Gửi code
     */
    public void createPendingCompany(CompanyDto companyDto) throws MessagingException {
        String email = companyDto.getWorkEmail();

        // Kiểm tra email đã tồn tại trong DB chưa
        if (companyRepository.existsByContactEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        // Nếu đang pending trong Redis, xóa đi và tạo mới
        if (redisService.existsPendingCompany(email)) {
            log.info("Overriding existing pending registration for: {}", email);
            redisService.deletePendingCompany(email);
        }

        // Generate 6-digit verification code
        String verificationCode = generateVerificationCode();

        // Lưu vào Redis (Company object + code) - expire sau 10 phút
        redisService.savePendingCompanyWithCode(companyDto, verificationCode);

        // Gửi email verification code
        sendVerificationEmail(email, companyDto.getName(), verificationCode);

        log.info("Created pending company in Redis: {}", email);
    }


    /**
     * Step 2: Verify code → Lấy Company từ Redis → Lưu vào DB
     */
    @Transactional
    public Company verifyAndSaveCompany(String email, String code) {
        // Verify code và lấy company từ Redis
        CompanyDto companyDto = redisService.verifyAndGetCompany(email, code);
        if (companyDto == null) {
            throw new RuntimeException("Company data not found or expired");
        }
        // Double check email đã tồn tại chưa
        if (companyRepository.existsByContactEmail(email)) {
            redisService.deletePendingCompany(email);
            throw new RuntimeException("Email already registered");
        }
        Company company = companyFromDto(companyDto);
        company.setIsVerified(true);
        Company savedCompany = companyRepository.save(company);
        UserBusiness userBusiness = new UserBusiness();
        userBusiness.setCompany(savedCompany);
        userBusiness.setRole(BusinessRole.EMPLOYEE);
        userBusiness.setName(companyDto.getName());
        userBusiness.setEmail(email);
        userBusiness.setPhone(companyDto.getContactPhone());
        userBusiness.setJobTitle(companyDto.getJobTitle());
        savedCompany.getUserBusinesses().add(userBusiness);
        companyRepository.save(savedCompany);
        // Xóa khỏi Redis
        redisService.deletePendingCompany(email);
        log.info("Company verified and saved to database: {}", email);
        return savedCompany;
    }

    /**
     * Resend verification code
     */
    public void resendVerificationCode(String email) throws MessagingException {
        // Lấy company từ Redis
        CompanyDto company = redisService.getPendingCompanyFromMap(email);

        if (company == null) {
            throw new RuntimeException("No pending registration found. Please register again.");
        }

        // Generate code mới
        String newCode = generateVerificationCode();

        // Cập nhật trong Redis
        redisService.savePendingCompanyWithCode(company, newCode);

        // Gửi email
        sendVerificationEmail(email, company.getName(), newCode);

        log.info("Resent verification code to: {}", email);
    }

    /**
     * Tạo company trực tiếp (không cần verify) - method cũ
     */
    @Transactional
    public Company createCompany(Company company) {
        if (companyRepository.existsByContactEmail(company.getContactEmail())) {
            throw new RuntimeException("Email already registered");
        }
        return companyRepository.save(company);
    }

    /**
     * Generate 6-digit verification code
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Gửi email verification code
     */
    private void sendVerificationEmail(String email, String companyName, String code)
            throws MessagingException {

        String subject = "Verify Your Company - Trustify";
        String text = String.format(
                "Hello %s,\n\n" +
                        "Thank you for registering with Trustify!\n\n" +
                        "Your verification code is: %s\n\n" +
                        "This code will expire in 5 minutes.\n\n" +
                        "If you didn't request this, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "Trustify Team",
                companyName, code
        );

        emailService.sendSimpleEmail(email, subject, text);
    }

    public Page<Company> getCompaniesByIndustry(String name, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return companyRepository.findByIndustry(name, pageable);
    }
    public Company getCompanyById(Long id) {
        return companyRepository.findById(id).orElse(null);
    }

    public Company updateCompany(@Valid CompanyDto companyDto, Long id) {
        Company company = companyRepository.findById(id).orElse(null);
        if (company == null) {
            throw new RuntimeException("Company not found");
        }
        company.setName(companyDto.getName());
        company.setContactPhone(companyDto.getContactPhone());
        company.setWebsiteUrl(companyDto.getWebsiteUrl());
        company.setIndustry(companyDto.getIndustry());
        company.setLogoUrl(companyDto.getAvatarUrl());
        company.setCompanySize(companyDto.getCompanySize());
        company.setFoundedYear(companyDto.getFoundedYear());
        company.setCountry(companyDto.getCountry());
        company.setAnnualRevenue(companyDto.getAnnualRevenue());
        company.setIsVerified(false);
        return companyRepository.save(company);
    }

    public Page<Company> getCompaniesByVerifyStatus(VerifyStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return companyRepository.findByVerifyStatus(status, pageable);
    }


    public Company updateVerificationFile(Long companyId, String fileUrl) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        company.setFileVerificationUrl(fileUrl);
        company.setVerifyStatus(VerifyStatus.PENDING);

        return companyRepository.save(company);
    }

    public Company approveCompanyVerification(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        company.setVerifyStatus(VerifyStatus.APPROVED);
        company.setIsVerified(true);
        company.setVerifiedAt(LocalDateTime.now());

        return companyRepository.save(company);
    }

    public Company rejectCompanyVerification(Long companyId, String reason) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        company.setVerifyStatus(VerifyStatus.REJECTED);
        company.setIsVerified(false);

        return companyRepository.save(company);
    }

    public Company updateCompanyFields(Long id, @Valid UpdateCompanyInfoDto dto) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        company.setName(dto.getName());
        company.setAddress(dto.getAddress());
        company.setWebsiteUrl(dto.getWebsiteUrl());
        company.setIndustry(dto.getIndustry());
        company.setCompanySize(dto.getCompanySize());
        company.setDescription(dto.getDescription());
        return companyRepository.save(company);
    }

    public Page<Company> searchCompanies(String keyword, int page, int size) {
        Sort sort = Sort.by(
                Sort.Order.desc("isVerified"),
                Sort.Order.desc("averageRating"),
                Sort.Order.desc("totalReviews")
        );
        Pageable pageable = PageRequest.of(page, size, sort);

        if (keyword == null || keyword.trim().isEmpty()) {
            return companyRepository.findByDeletedAtIsNull(pageable);
        }

        return companyRepository.findByNameContainingIgnoreCaseAndDeletedAtIsNull(
                keyword.trim(), pageable);
    }

    public Object getCompanyByProductId(Long productId) {
        return companyRepository.getCompanyByProductId(productId);
    }

    @Transactional(readOnly = true)
    public Company getCompanyByIdWithSubscriptions(Long companyId) {
        return companyRepository.findByIdWithSubscriptions(companyId);
    }

    @Transactional(readOnly = true)
    public boolean checkPremium(Long companyId) {
        Company company = companyRepository.findByIdWithSubscriptions(companyId);

        if (company == null || company.getSubscriptions() == null) {
            return false;
        }

        return company.getSubscriptions().stream()
                .anyMatch(sub -> sub.getPlan() != null &&
                        "PREMIUM".equals(sub.getPlan().getName()));
    }
}
