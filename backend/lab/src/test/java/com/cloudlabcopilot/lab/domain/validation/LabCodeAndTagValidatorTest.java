package com.cloudlabcopilot.lab.domain.validation;

import com.cloudlabcopilot.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("实验编码与标签校验测试")
class LabCodeAndTagValidatorTest {

    private final LabCodeValidator codeValidator = new LabCodeValidator();
    private final TagValidator tagValidator = new TagValidator();

    @Test
    @DisplayName("合法编码通过")
    void validCodePass() {
        assertThatCode(() -> codeValidator.validate("LAB-DEMO-01")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("非法编码被拒绝")
    void invalidCodeRejected() {
        assertThatThrownBy(() -> codeValidator.validate("a"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> codeValidator.validate("bad code!"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("合法标签通过")
    void validTagsPass() {
        assertThatCode(() -> tagValidator.validate(List.of("openGauss", "入门")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("重复标签被拒绝")
    void duplicatedTagRejected() {
        assertThatThrownBy(() -> tagValidator.validate(List.of("a", "a")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("超过10个标签被拒绝")
    void tooManyTagsRejected() {
        List<String> tags = java.util.stream.IntStream.range(0, 11)
                .mapToObj(i -> "t" + i).toList();
        assertThatThrownBy(() -> tagValidator.validate(tags))
                .isInstanceOf(BusinessException.class);
    }
}