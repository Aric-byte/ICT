package com.cloudlabcopilot.safety.application;

import com.cloudlabcopilot.common.security.SensitiveContentDetector;
import com.cloudlabcopilot.safety.domain.model.SafetyCheckResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("safety 危险规则与凭据检测")
class SafetyCheckerTest {

    private final SafetyChecker checker = new SafetyChecker(new SensitiveContentDetector());

    @Test
    @DisplayName("普通问题不判定危险")
    void normalQuestionSafe() {
        SafetyCheckResult r = checker.checkInput("为什么 openGauss 连接失败？");
        assertThat(r.dangerous()).isFalse();
    }

    @Test
    @DisplayName("检测到凭据")
    void credentialDetected() {
        SafetyCheckResult r = checker.checkInput("连接串 password=abc123 报错");
        assertThat(r.dangerous()).isTrue();
        assertThat(r.category()).isEqualTo("CREDENTIAL");
    }

    @Test
    @DisplayName("无 WHERE 的 DELETE 判为高风险")
    void deleteWithoutWhereHighRisk() {
        SafetyCheckResult r = checker.checkSuggestion("DELETE FROM students;");
        assertThat(r.dangerous()).isTrue();
        assertThat(r.riskLevel()).isEqualTo("HIGH");
        assertThat(r.safeAlternative()).isNotBlank();
    }

    @Test
    @DisplayName("DROP TABLE 判为高风险")
    void dropTableHighRisk() {
        SafetyCheckResult r = checker.checkSuggestion("DROP TABLE t_user;");
        assertThat(r.dangerous()).isTrue();
        assertThat(r.category()).isEqualTo("DROP");
    }

    @Test
    @DisplayName("带 WHERE 的 DELETE 不判危险")
    void deleteWithWhereSafe() {
        SafetyCheckResult r = checker.checkSuggestion("DELETE FROM students WHERE id = 1;");
        assertThat(r.dangerous()).isFalse();
    }
}