package com.cloudlabcopilot.common.security;

import com.cloudlabcopilot.common.constant.AppConstants;
import com.cloudlabcopilot.common.exception.BusinessException;
import com.cloudlabcopilot.common.response.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class OperatorContextResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentOperator.class)
                && OperatorPrincipal.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String id = request == null ? null : request.getHeader(AppConstants.HEADER_OPERATOR_ID);
        String role = request == null ? null : request.getHeader(AppConstants.HEADER_OPERATOR_ROLE);
        if (id == null || id.isBlank() || role == null || role.isBlank()) {
            throw new BusinessException(ErrorCode.OPERATOR_IDENTITY_REQUIRED);
        }
        OperatorPrincipal.Role parsedRole;
        try {
            parsedRole = OperatorPrincipal.Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.OPERATOR_IDENTITY_REQUIRED);
        }
        return new OperatorPrincipal(id.trim(), parsedRole);
    }
}