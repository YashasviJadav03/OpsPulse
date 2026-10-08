package com.opspulse.core.config;

import com.opspulse.common.tenant.TenantFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class TenantFilterConfig {

    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilterRegistration() {
        FilterRegistrationBean<TenantFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TenantFilter(true));
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registrationBean;
    }
}
