package com.cloudlabcopilot.lab.domain.validation;

import com.cloudlabcopilot.common.exception.BusinessException;
import com.cloudlabcopilot.common.response.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class TagValidator {

    public void validate(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        if (tags.size() > 10) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "标签最多 10 个");
        }
        Set<String> seen = new HashSet<>();
        for (String tag : tags) {
            if (tag == null || tag.isBlank() || tag.length() > 32) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "标签长度需在 1~32");
            }
            if (!seen.add(tag)) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "标签不可重复");
            }
        }
    }
}