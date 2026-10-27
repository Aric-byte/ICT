package com.cloudlabcopilot.boot.config;

import com.cloudlabcopilot.boot.common.filter.TraceIdFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final SecurityProperties securityProperties;

    public WebMvcConfig(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        SecurityProperties.Cors cors = securityProperties.cors();
        registry.addMapping("/api/**")
                .allowedOrigins(cors.allowedOrigins().toArray(new String[0]))
                .allowedMethods(cors.allowedMethods().toArray(new String[0]))
                .allowedHeaders(cors.allowedHeaders().toArray(new String[0]))
                .allowCredentials(cors.allowCredentials())
                .maxAge(cors.maxAge());
    }

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(new TraceIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }
}