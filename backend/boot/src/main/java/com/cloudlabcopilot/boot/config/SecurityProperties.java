package com.cloudlabcopilot.boot.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        Cors cors,
        Multipart multipart
) {
    public record Cors(
            @NotEmpty List<String> allowedOrigins,
            boolean allowCredentials,
            List<String> allowedMethods,
            List<String> allowedHeaders,
            long maxAge
    ) {}

    public record Multipart(
            @NotBlank String maxFileSize,
            @NotBlank String maxRequestSize
    ) {}
}