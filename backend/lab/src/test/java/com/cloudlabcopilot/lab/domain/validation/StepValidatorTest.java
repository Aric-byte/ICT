package com.cloudlabcopilot.lab.domain.validation;

import com.cloudlabcopilot.common.exception.BusinessException;
import com.cloudlabcopilot.lab.domain.model.LabStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("步骤校验器测试")
class StepValidatorTest {

    private final StepValidator validator = new StepValidator();

    @Test
    @DisplayName("连续步骤通过校验")
    void validStepsPass() {
        assertThatCode(() -> validator.validate(List.of(step(1), step(2))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("空步骤被拒绝")
    void emptyStepsRejected() {
        assertThatThrownBy(() -> validator.validate(List.of()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("序号不连续被拒绝")
    void nonContinuousSeqRejected() {
        assertThatThrownBy(() -> validator.validate(List.of(step(1), step(3))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("缺少完成判据被拒绝")
    void missingCriteriaRejected() {
        LabStep invalid = LabStep.builder()
                .stepNo(1)
                .title("t")
                .instruction("i")
                .completionCriteria("")
                .build();
        assertThatThrownBy(() -> validator.validate(List.of(invalid)))
                .isInstanceOf(BusinessException.class);
    }

    private LabStep step(int no) {
        return LabStep.builder()
                .stepNo(no)
                .title("标题" + no)
                .instruction("操作说明" + no)
                .completionCriteria("完成判据" + no)
                .build();
    }
}