package com.cloudlabcopilot.safety.application;

import com.cloudlabcopilot.common.security.SensitiveContentDetector;
import com.cloudlabcopilot.common.security.SensitiveDetectionResult;
import com.cloudlabcopilot.safety.domain.model.SafetyCheckResult;
import com.cloudlabcopilot.safety.domain.rule.DangerRule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SafetyChecker {

    private final List<DangerRule> rules = DangerRule.defaultRules();
    private final SensitiveContentDetector detector;

    public SafetyChecker(SensitiveContentDetector detector) {
        this.detector = detector;
    }

    public SafetyCheckResult checkInput(String text) {
        SensitiveDetectionResult sensitive = detector.detect(text);
        if (sensitive.hit()) {
            return new SafetyCheckResult(
                    true,
                    "CREDENTIAL",
                    "HIGH",
                    "检测到疑似凭据/敏感信息（" + sensitive.category() + "）",
                    "请删除密码/密钥/连接串口令后重新提交");
        }
        return checkDanger(text);
    }

    public SafetyCheckResult checkSuggestion(String text) {
        return checkDanger(text);
    }

    private SafetyCheckResult checkDanger(String text) {
        if (text == null || text.isBlank()) {
            return SafetyCheckResult.clean();
        }
        for (DangerRule rule : rules) {
            if (rule.pattern().matcher(text).find()) {
                return new SafetyCheckResult(
                        true,
                        rule.category(),
                        rule.riskLevel(),
                        rule.description(),
                        rule.safeAlternative());
            }
        }
        return SafetyCheckResult.clean();
    }
}