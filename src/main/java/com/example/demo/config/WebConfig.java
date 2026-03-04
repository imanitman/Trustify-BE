// java
package com.trustify.trustify.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@AllArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final PremiumInterceptor premiumInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // adjust path pattern to match your IntegrationController endpoints
        registry.addInterceptor(premiumInterceptor)
                .addPathPatterns("/integration/**");
    }
}
