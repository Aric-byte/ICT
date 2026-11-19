package com.cloudlabcopilot.common.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SensitiveContentDetector {

    private static final Pattern PRIVATE_KEY =
            Pattern.compile("-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----");
    private static final Pattern CONNECTION_STRING =
            Pattern.compile("(?i)[a-z][a-z0-9+.-]*://[^:/\\s]+:[^@\\s]+@[^\\s]+");
    private static final Pattern PERSONAL_ID =
            Pattern.compile("(\\b\\d{17}[0-9Xx]\\b)|(\\b1[3-9]\\d{9}\\b)");
    private static final Pattern PASSWORD =
            Pattern.compile("(?i)(password|passwd|pwd)\\s*[=:]\\s*\\S+");
    private static final Pattern CLOUD_CREDENTIAL =
            Pattern.compile("(?i)(AK|SK|access[-_]?key|secret[-_]?key|Bearer\\s+\\S+|token\\s*=\\s*\\S{8,})");
    private static final Pattern SENSITIVE_KEY =
            Pattern.compile("(?i)(password|passwd|pwd|private|secret|token|access[-_]?key|credential)");

    public SensitiveDetectionResult detect(String text) {
        if (text == null || text.isEmpty()) {
            return SensitiveDetectionResult.clean(0);
        }
        int len = text.length();
        if (PRIVATE_KEY.matcher(text).find()) {
            return SensitiveDetectionResult.hit(SensitiveCategory.PRIVATE_KEY, len);
        }
        if (CONNECTION_STRING.matcher(text).find()) {
            return SensitiveDetectionResult.hit(SensitiveCategory.CONNECTION_STRING, len);
        }
        if (PERSONAL_ID.matcher(text).find()) {
            return SensitiveDetectionResult.hit(SensitiveCategory.PERSONAL_ID, len);
        }
        if (PASSWORD.matcher(text).find()) {
            return SensitiveDetectionResult.hit(SensitiveCategory.PASSWORD, len);
        }
        if (CLOUD_CREDENTIAL.matcher(text).find()) {
            return SensitiveDetectionResult.hit(SensitiveCategory.CLOUD_CREDENTIAL, len);
        }
        return SensitiveDetectionResult.clean(len);
    }

    public SensitiveDetectionResult detectKey(String key) {
        if (key == null || key.isEmpty()) {
            return SensitiveDetectionResult.clean(0);
        }
        int len = key.length();
        if (SENSITIVE_KEY.matcher(key).find()) {
            return SensitiveDetectionResult.hit(SensitiveCategory.SENSITIVE_KEY, len);
        }
        return SensitiveDetectionResult.clean(len);
    }
}