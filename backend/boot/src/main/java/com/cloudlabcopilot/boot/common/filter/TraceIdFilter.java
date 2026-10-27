package com.cloudlabcopilot.boot.common.filter;

import com.cloudlabcopilot.boot.common.constant.AppConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

public class TraceIdFilter extends OncePerRequestFilter {

    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("[a-fA-F0-9-]{8,36}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader(AppConstants.DEFAULT_TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank() || !TRACE_ID_PATTERN.matcher(traceId).matches()) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(AppConstants.TRACE_ID_MDC_KEY, traceId);
        response.setHeader(AppConstants.DEFAULT_TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}