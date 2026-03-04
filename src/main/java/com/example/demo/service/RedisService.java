package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustify.trustify.dto.Req.CompanyDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    // Chỉ cần inject RedisTemplate<String, Object>
    // StringRedisTemplate tự động có sẵn từ Spring Boot nếu cần
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    private static final String INACTIVE_USER_PREFIX = "inactive_user:";
    private static final String STATE_PREFIX = "oauth2_state:";
    private static final String COMPANY_PREFIX = "company:pending:";
    private static final String MAGIC_LINK_PREFIX = "magic_link:";

    /**
     * Lưu OAuth2 state (String)
     */
    public void saveStateCode(String stateCode, String jwtToken) {
        redisTemplate.opsForValue().set(
                STATE_PREFIX + stateCode,
                jwtToken,  // String vẫn hoạt động bình thường
                Duration.ofMinutes(15)
        );
        log.debug("Saved OAuth2 state to Redis: {}", stateCode);
    }

    public String getAndRemoveStateToken(String stateCode) {
        String key = STATE_PREFIX + stateCode;
        Object value = redisTemplate.opsForValue().get(key);

        if (value != null) {
            redisTemplate.delete(key);
            log.debug("Retrieved and removed OAuth2 state: {}", stateCode);
            return value.toString();
        }
        return null;
    }

    /**
     * Lưu Company + verification code
     */
    public void savePendingCompanyWithCode(CompanyDto company, String verificationCode) {
        String key = COMPANY_PREFIX + company.getWorkEmail();

        Map<String, Object> data = new HashMap<>();
        data.put("company", company);
        data.put("code", verificationCode);
        data.put("timestamp", System.currentTimeMillis());

        redisTemplate.opsForValue().set(key, data, Duration.ofMinutes(5));
        log.info("Saved pending company with code to Redis: {}", company.getWorkEmail());
    }

    /**
     * Lấy Company từ Redis
     */
    @SuppressWarnings("unchecked")
    public CompanyDto getPendingCompanyFromMap(String email) {
        String key = COMPANY_PREFIX + email;
        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        Map<String, Object> data = (Map<String, Object>) value;
        Object companyObj = data.get("company");

        return objectMapper.convertValue(companyObj, CompanyDto.class);
    }

    /**
     * Lấy verification code
     */
    @SuppressWarnings("unchecked")
    public String getVerificationCode(String email) {
        String key = COMPANY_PREFIX + email;
        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        Map<String, Object> data = (Map<String, Object>) value;
        return (String) data.get("code");
    }

    /**
     * Verify và lấy company
     */
    public CompanyDto verifyAndGetCompany(String email, String code) {
        String savedCode = getVerificationCode(email);

        if (savedCode == null) {
            throw new RuntimeException("Verification session expired. Please register again.");
        }

        if (!code.equals(savedCode)) {
            throw new RuntimeException("Invalid verification code");
        }

        return getPendingCompanyFromMap(email);
    }

    public void sendMagicLink(String email){
        String key =  saveCodeMagicLink(email);
        String magicLink = "https://trustify-company.vercel.app/magic-link?code=" + key;
        emailService.sendSimpleEmail(email, "Magic Link", magicLink);
    }

    public String verifyMagicLink(String code) {
        String email = (String) redisTemplate.opsForValue().get(code);
        if (email == null) {
            return "";
        }
        redisTemplate.delete(MAGIC_LINK_PREFIX + code);
        return email;
    }

    public String saveCodeMagicLink(String email){
        String key = MAGIC_LINK_PREFIX + generateCode();
        redisTemplate.opsForValue().set(key, email, Duration.ofMinutes(5));
        log.info("Saved generated code to Redis: {}", key);
        return key;
    }

    /**
     * Xóa pending company
     */
    public void deletePendingCompany(String email) {
        String key = COMPANY_PREFIX + email;
        Boolean deleted = redisTemplate.delete(key);
        log.info("Deleted pending company from Redis: {} (success: {})", email, deleted);
    }

    /**
     * Kiểm tra tồn tại
     */
    public boolean existsPendingCompany(String email) {
        String key = COMPANY_PREFIX + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public String generateCode (){
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return code;
    }

    public void saveInactiveUser(String email) {
        String key = INACTIVE_USER_PREFIX + email;
        redisTemplate.opsForValue().set(key, "inactive", Duration.ofDays(7));
        log.info("Saved inactive user to Redis for 7 days: {}", email);
    }

    public boolean isUserInactive(String email) {
        String key = INACTIVE_USER_PREFIX + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void removeInactiveUser(String email) {
        String key = INACTIVE_USER_PREFIX + email;
        redisTemplate.delete(key);
        log.info("Removed inactive user from Redis: {}", email);
    }
}