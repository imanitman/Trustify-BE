package com.example.demo.controller.Business;

import com.trustify.trustify.dto.Req.PaymentRequest;
import com.trustify.trustify.dto.Res.PaymentResponse;
import com.trustify.trustify.entity.Payment;
import com.trustify.trustify.service.Business.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final VNPayService vnPayService;
    @Value("${app.frontend.url:https://trustify-company.vercel.app}")
    private String frontendUrl;

    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody PaymentRequest paymentRequest,
            HttpServletRequest request) {
        PaymentResponse response = vnPayService.createPayment(paymentRequest, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/vnpay-return")
    public void paymentReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> result = vnPayService.handlePaymentReturn(request);
        String txnRef = result.get("txnRef") != null ? String.valueOf(result.get("txnRef")) : request.getParameter("vnp_TxnRef");
        String status = result.get("status") != null ? String.valueOf(result.get("status")) : "UNKNOWN";
        String redirectUrl = frontendUrl
                + "/checkout/vnpay-return?vnp_TxnRef=" + URLEncoder.encode(txnRef == null ? "" : txnRef, StandardCharsets.UTF_8)
                + "&status=" + URLEncoder.encode(status, StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> paymentIPN(HttpServletRequest request) {
        Map<String, Object> result = vnPayService.handlePaymentReturn(request);
        Map<String, String> ipnResult = new java.util.HashMap<>();
        if ("SUCCESS".equals(result.get("status"))) {
            ipnResult.put("RspCode", "00");
            ipnResult.put("Message", "Confirm Success");
        } else {
            ipnResult.put("RspCode", "99");
            ipnResult.put("Message", "Confirm Fail");
        }
        return ResponseEntity.ok(ipnResult);
    }

    @GetMapping("/history/{companyId}")
    public ResponseEntity<List<Payment>> getPaymentHistory(@PathVariable Long companyId) {
        List<Payment> payments = vnPayService.getPaymentsByCompanyId(companyId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/detail/{txnRef}")
    public ResponseEntity<Payment> getPaymentDetail(@PathVariable String txnRef) {
        return vnPayService.getPaymentByTxnRef(txnRef)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
