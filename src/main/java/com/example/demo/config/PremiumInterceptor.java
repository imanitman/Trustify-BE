// java
package com.trustify.trustify.config;

import com.trustify.trustify.entity.Company;
import com.trustify.trustify.entity.Subscription;
import com.trustify.trustify.service.Business.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PremiumInterceptor implements HandlerInterceptor {

    @Lazy
    private final CompanyService companyService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        Long companyId = extractCompanyId(request);

        if (companyId != null) {
            boolean hasPremium = companyService.checkPremium(companyId);
            request.setAttribute("hasPremium", hasPremium);
        }

        return true;
    }

    private Long extractCompanyId(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String[] parts = uri.split("/");

        // Try to find the last numeric segment as the company ID
        for (int i = parts.length - 1; i >= 0; i--) {
            try {
                return Long.parseLong(parts[i]);
            } catch (NumberFormatException e) {
                // Continue searching
            }
        }

        return null; // or throw exception if ID is required
    }

}

