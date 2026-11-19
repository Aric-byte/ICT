package com.cloudlabcopilot.lab.domain.validation;

import com.cloudlabcopilot.common.exception.BusinessException;
import com.cloudlabcopilot.common.response.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class LabCodeValidator {

    private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9_-]{3,64}$");

    public void validate(String labCode) {
        if (labCode == null || labCode.isBlank()) {
            throw new BusinessException(ErrorCode.FIELD_REQUIRED, "labCode 不能为空");
        }
        if (!CODE.matcher(labCode).matches()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "labCode 格式不合法");
        }
    }
}