package com.cloudlabcopilot.common.trace;

import com.cloudlabcopilot.common.constant.AppConstants;
import com.cloudlabcopilot.common.id.PublicIdGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader(AppConstants.HEADER_TRACE_ID);
        if (traceId == null || traceId.isBlank() || traceId.length() > 64) {
            traceId = PublicIdGenerator.nextId();
        }
        TraceIdContext.set(traceId);
        response.setHeader(AppConstants.HEADER_TRACE_ID, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceIdContext.clear();
        }
    }
}