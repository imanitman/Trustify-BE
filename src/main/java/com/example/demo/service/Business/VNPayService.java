package com.example.demo.service.Business;

import com.trustify.trustify.config.VNPayConfig;
import com.trustify.trustify.dto.Req.PaymentRequest;
import com.trustify.trustify.dto.Res.PaymentResponse;
import com.trustify.trustify.entity.Company;
import com.trustify.trustify.entity.Payment;
import com.trustify.trustify.entity.Plan;
import com.trustify.trustify.entity.Subscription;
import com.trustify.trustify.enums.PaymentStatus;
import com.trustify.trustify.enums.SubsPlan;
import com.trustify.trustify.enums.SubscriptionStatus;
import com.trustify.trustify.repository.Business.CompanyRepository;
import com.trustify.trustify.repository.Business.PaymentRepository;
import com.trustify.trustify.repository.Business.PlanRepository;
import com.trustify.trustify.repository.Business.SubscriptionRepository;
import com.trustify.trustify.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {

    private final VNPayConfig vnPayConfig;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CompanyRepository companyRepository;
    private final PlanRepository planRepository;

    private static final ZoneId VNP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh"); // VN timezone (GMT+7)
    private static final DateTimeFormatter VNP_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int DEFAULT_EXPIRE_MINUTES = 15;

    @Transactional
    public PaymentResponse createPayment(PaymentRequest paymentRequest, HttpServletRequest request) {
        Plan plan = planRepository.findById(paymentRequest.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        Company company = companyRepository.findById(paymentRequest.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        String vnp_TxnRef = VNPayUtil.getRandomNumber(8);
        String vnp_IpAddr = VNPayUtil.getIpAddress(request);
        BigDecimal price = plan.getPrice() == null ? BigDecimal.ZERO : plan.getPrice();
        long amount = price.longValue();

        Map<String, String> vnp_Params = new LinkedHashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100));
        vnp_Params.put("vnp_CurrCode", "VND");

        if (paymentRequest.getBankCode() != null && !paymentRequest.getBankCode().isEmpty()) {
            vnp_Params.put("vnp_BankCode", paymentRequest.getBankCode());
        }

        String orderInfo = "Thanh toan goi " + plan.getName() + " cho " + company.getName();
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", "billpayment");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        ZonedDateTime now = ZonedDateTime.now(VNP_ZONE);
        vnp_Params.put("vnp_CreateDate", now.format(VNP_DATE_FORMATTER));

        int expireMinutes = DEFAULT_EXPIRE_MINUTES;
        // If VNPayConfig has a getter for expire minutes, prefer it (avoid compile error if absent).
        try {
            Integer configured = (Integer) vnPayConfig.getClass().getMethod("getExpireMinutes").invoke(vnPayConfig);
            if (configured != null && configured > 0) {
                expireMinutes = configured;
            }
        } catch (Exception ignored) {
            // method not present or inaccessible: keep default
        }

        ZonedDateTime expire = now.plusMinutes(expireMinutes);
        vnp_Params.put("vnp_ExpireDate", expire.format(VNP_DATE_FORMATTER));

        String queryUrl = VNPayUtil.getPaymentURL(vnp_Params, true);
        String hashData = VNPayUtil.getPaymentURL(vnp_Params, false);
        String vnp_SecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        String paymentUrl = vnPayConfig.getPaymentUrl() + "?" + queryUrl;

        Payment payment = Payment.builder()
                .txnRef(vnp_TxnRef)
                .amount(price)
                .orderInfo(orderInfo)
                .status(PaymentStatus.PENDING)
                .companyId(paymentRequest.getCompanyId())
                .planId(paymentRequest.getPlanId())
                .ipAddress(vnp_IpAddr)
                .build();

        paymentRepository.save(payment);
        log.info("Created payment: txnRef={}, company={}, plan={}", vnp_TxnRef, company.getName(), plan.getName());

        return PaymentResponse.builder()
                .status("OK")
                .message("Tạo URL thanh toán thành công")
                .paymentUrl(paymentUrl)
                .txnRef(vnp_TxnRef)
                .build();
    }

    @Transactional
    public Map<String, Object> handlePaymentReturn(HttpServletRequest request) {
        Map<String, String> vnp_Params = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            if (paramValue != null && !paramValue.isEmpty()) {
                vnp_Params.put(paramName, paramValue);
            }
        }

        String incomingSecureHash = vnp_Params.get("vnp_SecureHash");
        vnp_Params.remove("vnp_SecureHashType");
        vnp_Params.remove("vnp_SecureHash");

        String computedHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(),
                VNPayUtil.getPaymentURL(vnp_Params, false));

        Map<String, Object> result = new HashMap<>();
        String txnRef = vnp_Params.get("vnp_TxnRef");

        Optional<Payment> paymentOpt = paymentRepository.findByTxnRef(txnRef);
        if (paymentOpt.isEmpty()) {
            result.put("status", "ERROR");
            result.put("message", "Không tìm thấy giao dịch");
            result.put("txnRef", txnRef);
            return result;
        }

        Payment payment = paymentOpt.get();

        if (computedHash != null && computedHash.equalsIgnoreCase(incomingSecureHash)) {
            String vnp_ResponseCode = vnp_Params.get("vnp_ResponseCode");

            payment.setVnpTransactionNo(vnp_Params.get("vnp_TransactionNo"));
            payment.setBankCode(vnp_Params.get("vnp_BankCode"));
            payment.setPayDate(vnp_Params.get("vnp_PayDate"));
            payment.setResponseCode(vnp_ResponseCode);

            if ("00".equals(vnp_ResponseCode)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                Subscription subscription = createSubscription(payment);
                result.put("subscription", subscription);
                result.put("status", "SUCCESS");
                result.put("message", "Thanh toán thành công");
                log.info("Payment SUCCESS: txnRef={}", txnRef);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                result.put("status", "FAILED");
                result.put("message", "Thanh toán thất bại. Mã lỗi: " + vnp_ResponseCode);
                log.info("Payment FAILED: txnRef={}, code={}", txnRef, vnp_ResponseCode);
            }
            paymentRepository.save(payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setResponseCode("INVALID_SIGNATURE");
            paymentRepository.save(payment);

            result.put("status", "INVALID");
            result.put("message", "Chữ ký không hợp lệ");
            log.warn("Invalid VNPay signature: txnRef={}", txnRef);
        }

        result.put("txnRef", txnRef);
        result.put("amount", payment.getAmount());
        result.put("bankCode", vnp_Params.get("vnp_BankCode"));

        return result;
    }

    private Subscription createSubscription(Payment payment) {
        Company company = companyRepository.findById(payment.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        Plan plan = planRepository.findById(payment.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime endDate = now.plusDays(plan.getDurationDays());

        Subscription subscription = Subscription.builder()
                .company(company)
                .plan(plan)
                .payment(payment)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now)
                .endDate(endDate)
                .build();

        subscriptionRepository.save(subscription);

        // Update company planName based on plan name
        SubsPlan newPlanName = mapPlanNameToSubsPlan(plan.getName());
        company.setPlanName(newPlanName);
        company.setPlanStart(now);
        company.setPlanEnd(endDate);
        companyRepository.save(company);

        log.info("Updated company plan: company={}, planName={}, endDate={}",
                company.getName(), newPlanName, endDate);
        log.info("Created subscription: company={}, plan={}, endDate={}",
                company.getName(), plan.getName(), endDate);

        return subscription;
    }

    private SubsPlan mapPlanNameToSubsPlan(String planName) {
        if (planName == null) return SubsPlan.NONE;

        String upperPlanName = planName.toUpperCase();
        if (upperPlanName.contains("PRO")) return SubsPlan.PRO;
        if (upperPlanName.contains("PREMIUM")) return SubsPlan.PREMIUM;
        if (upperPlanName.contains("PLUS")) return SubsPlan.PLUS;

        return SubsPlan.NONE;
    }


    public List<Payment> getPaymentsByCompanyId(Long companyId) {
        return paymentRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
    }

    public Optional<Payment> getPaymentByTxnRef(String txnRef) {
        return paymentRepository.findByTxnRef(txnRef);
    }
}
