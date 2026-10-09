package br.com.sindico.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class FrontendRedirectConfig {

    @Bean
    public FilterRegistrationBean<FrontendRedirectFilter> frontendRedirectFilter(
            @Value("${app.frontend.redirect-enabled:false}") boolean enabled,
            @Value("${app.frontend.url:https://sindico-seven.vercel.app}") String frontendUrl) {
        FilterRegistrationBean<FrontendRedirectFilter> registration =
                new FilterRegistrationBean<>(new FrontendRedirectFilter(enabled, frontendUrl));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
