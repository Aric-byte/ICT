package com.cloudlabcopilot.lab.domain.validation;

import com.cloudlabcopilot.common.exception.BusinessException;
import com.cloudlabcopilot.common.response.ErrorCode;
import com.cloudlabcopilot.lab.domain.model.LabStep;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StepValidator {

    private static final int MAX_STEPS = 50;

    public void validate(List<LabStep> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new BusinessException(ErrorCode.LAB_PUBLISH_NO_STEP);
        }
        if (steps.size() > MAX_STEPS) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "步骤最多 50 个");
        }
        int expected = 1;
        for (LabStep step : steps) {
            if (step.getStepNo() != expected) {
                throw new BusinessException(ErrorCode.LAB_STEP_SEQUENCE_INVALID, "缺少步骤序号 " + expected);
            }
            expected++;
            if (step.getTitle() == null || step.getTitle().isBlank() || step.getTitle().length() > 120) {
                throw new BusinessException(ErrorCode.FIELD_REQUIRED, "步骤标题长度需在 1~120");
            }
            if (step.getInstruction() == null
                    || step.getInstruction().isBlank()
                    || step.getInstruction().length() > 5000) {
                throw new BusinessException(ErrorCode.FIELD_REQUIRED, "步骤说明长度需在 1~5000");
            }
            if (step.getCompletionCriteria() == null
                    || step.getCompletionCriteria().isBlank()
                    || step.getCompletionCriteria().length() > 1000) {
                throw new BusinessException(ErrorCode.LAB_STEP_CRITERIA_MISSING);
            }
            if (step.getHint() != null && step.getHint().length() > 1000) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "提示长度不能超过 1000");
            }
        }
    }
}