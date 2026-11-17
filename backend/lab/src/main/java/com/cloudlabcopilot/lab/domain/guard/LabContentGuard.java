package com.cloudlabcopilot.lab.domain.guard;

import com.cloudlabcopilot.common.exception.BusinessException;
import com.cloudlabcopilot.common.response.ErrorCode;
import com.cloudlabcopilot.common.security.SensitiveContentDetector;
import com.cloudlabcopilot.common.security.SensitiveDetectionResult;
import com.cloudlabcopilot.lab.domain.model.LabDefinition;
import com.cloudlabcopilot.lab.domain.model.LabStep;
import com.cloudlabcopilot.lab.domain.model.Precondition;
import org.springframework.stereotype.Component;

@Component
public class LabContentGuard {

    private final SensitiveContentDetector detector;

    public LabContentGuard(SensitiveContentDetector detector) {
        this.detector = detector;
    }

    public void check(String text) {
        SensitiveDetectionResult result = detector.detect(text);
        if (result.hit()) {
            throw new BusinessException(ErrorCode.SENSITIVE_CONTENT_REJECTED);
        }
    }

    public void checkDefinition(LabDefinition def) {
        check(def.getName());
        check(def.getObjective());
        check(def.getInternalNote());
    }

    public void checkSteps(java.util.List<LabStep> steps) {
        if (steps == null) {
            return;
        }
        for (LabStep step : steps) {
            check(step.getTitle());
            check(step.getInstruction());
            check(step.getCompletionCriteria());
            check(step.getHint());
        }
    }

    public void checkPreconditions(java.util.List<Precondition> list) {
        if (list == null) {
            return;
        }
        for (Precondition p : list) {
            check(p.getDescription());
        }
    }
}