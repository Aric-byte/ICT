package com.cloudlabcopilot.common.exception;

import com.cloudlabcopilot.common.constant.AppConstants;
import com.cloudlabcopilot.common.response.ApiResponse;
import com.cloudlabcopilot.common.response.ErrorCode;
import com.cloudlabcopilot.common.response.ResultCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String traceId() {
        String t = MDC.get(AppConstants.TRACE_ID_MDC_KEY);
        return t == null ? "" : t;
    }

    private <T> ApiResponse<T> withTrace(ApiResponse<T> resp) {
        resp.setTraceId(traceId());
        return resp;
    }

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, errorCode={}, message={}", e.getCode(), e.getErrorCode(), e.getMessage());
        ApiResponse<Void> resp = ApiResponse.error(e.getCode(), e.getMessage());
        if (e.getErrorCode() != null) {
            resp.setErrorCode(e.getErrorCode().name());
        }
        if (e.getDetails() != null) {
            resp.setDetails(e.getDetails());
        }
        return withTrace(resp);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        ApiResponse<Void> resp = ApiResponse.error(ErrorCode.FIELD_REQUIRED);
        resp.setMessage(message);
        return withTrace(resp);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("约束校验失败: {}", e.getMessage());
        return withTrace(ApiResponse.error(ErrorCode.INVALID_PARAMETER));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResponse<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {}", e.getName());
        return withTrace(ApiResponse.error(ErrorCode.INVALID_PARAMETER));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ApiResponse<Void> handleMissingHeader(MissingRequestHeaderException e) {
        log.warn("缺少请求头: {}", e.getHeaderName());
        return withTrace(ApiResponse.error(ErrorCode.OPERATOR_IDENTITY_REQUIRED));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体不可读: {}", e.getMessage());
        return withTrace(ApiResponse.error(ErrorCode.INVALID_PARAMETER));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ApiResponse<Void> handleNoHandlerFound(NoHandlerFoundException e) {
        log.warn("处理器未找到: {}", e.getRequestURL());
        return withTrace(ApiResponse.error(ResultCode.NOT_FOUND.getCode(), ResultCode.NOT_FOUND.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ApiResponse<Void> handleDataIntegrity(DataIntegrityViolationException e) {
        String cause = e.getMostSpecificCause() == null ? "" : e.getMostSpecificCause().getMessage();
        log.warn("数据完整性冲突: {}", cause);
        String lower = cause == null ? "" : cause.toLowerCase();
        if (lower.contains("unique") || lower.contains("duplicate") || lower.contains("uk_")) {
            return withTrace(ApiResponse.error(ErrorCode.LAB_CODE_CONFLICT));
        }
        return withTrace(ApiResponse.error(ErrorCode.INVALID_PARAMETER));
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ApiResponse<Void> handleLockTimeout(PessimisticLockingFailureException e) {
        log.warn("锁等待超时: {}", e.getMessage());
        return withTrace(ApiResponse.error(ErrorCode.SESSION_LOCK_TIMEOUT));
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return withTrace(ApiResponse.error(ErrorCode.SYSTEM_ERROR));
    }
}