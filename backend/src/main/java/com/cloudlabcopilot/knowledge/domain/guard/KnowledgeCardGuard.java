package com.cloudlabcopilot.knowledge.domain.guard;

import com.cloudlabcopilot.common.exception.BusinessException;
import com.cloudlabcopilot.common.response.ErrorCode;
import com.cloudlabcopilot.knowledge.domain.model.KnowledgeCard;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeCardGuard {

    public void validate(KnowledgeCard card) {
        if (card.getTechStack() == null || card.getTechStack().isBlank()) {
            throw new BusinessException(ErrorCode.FIELD_REQUIRED, "techStack 不能为空");
        }
        if (card.getTitle() == null || card.getTitle().isBlank() || card.getTitle().length() > 200) {
            throw new BusinessException(ErrorCode.FIELD_REQUIRED, "标题长度需在 1~200");
        }
        if (card.getSymptom() == null || card.getSymptom().isBlank() || card.getSymptom().length() > 2000) {
            throw new BusinessException(ErrorCode.FIELD_REQUIRED, "现象描述长度需在 1~2000");
        }
        if (card.getSafeNextStep() == null || card.getSafeNextStep().isBlank()
                || card.getSafeNextStep().length() > 1000) {
            throw new BusinessException(ErrorCode.FIELD_REQUIRED, "最小安全下一步长度需在 1~1000");
        }
        if (card.getCaseOrigin() == null || !List.of("REAL", "DERIVED").contains(card.getCaseOrigin())) {
            throw new BusinessException(ErrorCode.FIELD_REQUIRED, "案例来源必须为 REAL 或 DERIVED");
        }
        if (card.getSourceRef() == null || card.getSourceRef().isBlank()
                || card.getSourceRef().contains("百度") || card.getSourceRef().contains("网络")) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "来源不可核验");
        }
        if (card.getErrorPatterns() == null || card.getErrorPatterns().isEmpty()) {
            throw new BusinessException(ErrorCode.FIELD_REQUIRED, "至少需要一条错误模式");
        }
        if (card.getCauses() == null || card.getCauses().isEmpty()) {
            throw new BusinessException(ErrorCode.FIELD_REQUIRED, "至少需要一条可能原因");
        }
        if ("PUBLISHED".equals(card.getStatus()) && !"APPROVED".equals(card.getReviewStatus())) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "发布前必须审核通过");
        }
    }
}