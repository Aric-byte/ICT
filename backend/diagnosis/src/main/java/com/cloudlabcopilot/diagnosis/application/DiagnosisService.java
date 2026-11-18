package com.cloudlabcopilot.diagnosis.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudlabcopilot.common.exception.BusinessException;
import com.cloudlabcopilot.common.id.PublicIdGenerator;
import com.cloudlabcopilot.common.response.ErrorCode;
import com.cloudlabcopilot.diagnosis.api.dto.DiagnosisDtos.AnswerView;
import com.cloudlabcopilot.diagnosis.api.dto.DiagnosisDtos.AskRequest;
import com.cloudlabcopilot.diagnosis.api.dto.DiagnosisDtos.CauseView;
import com.cloudlabcopilot.diagnosis.api.dto.DiagnosisDtos.CitationView;
import com.cloudlabcopilot.diagnosis.api.dto.DiagnosisDtos.NextStepView;
import com.cloudlabcopilot.knowledge.infrastructure.entity.KnowledgeCardCauseEntity;
import com.cloudlabcopilot.knowledge.infrastructure.entity.KnowledgeCardEntity;
import com.cloudlabcopilot.knowledge.infrastructure.mapper.KnowledgeCardCauseMapper;
import com.cloudlabcopilot.knowledge.infrastructure.mapper.KnowledgeCardMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DiagnosisService {

    private final KnowledgeCardMapper cardMapper;
    private final KnowledgeCardCauseMapper causeMapper;

    public DiagnosisService(KnowledgeCardMapper cardMapper, KnowledgeCardCauseMapper causeMapper) {
        this.cardMapper = cardMapper;
        this.causeMapper = causeMapper;
    }

    @Transactional(readOnly = true)
    public AnswerView ask(AskRequest req) {
        if (req.question() == null || req.question().isBlank()) {
            throw new BusinessException(ErrorCode.DIAGNOSIS_QUESTION_REQUIRED);
        }
        if (req.question().length() > 20000) {
            throw new BusinessException(ErrorCode.TEXT_TOO_LONG);
        }
        String questionId = PublicIdGenerator.nextId();
        String techStack = normalizeTech(req.techStack());
        String keywords = extractKeywords(req.question());
        String like = "%" + keywords + "%";
        List<KnowledgeCardEntity> cards = cardMapper.search(techStack, keywords, like);

        if (cards.isEmpty()) {
            return new AnswerView(
                    questionId,
                    "INSUFFICIENT_EVIDENCE",
                    restate(req.question()),
                    "未能在知识库中找到与问题高度匹配的资料，暂无法给出确定原因。",
                    new ArrayList<>(),
                    new NextStepView(
                            "请补充完整报错文本与当前步骤，并先在实验环境中核对连接参数。",
                            "预期获得更明确的错误线索",
                            "LOW",
                            new ArrayList<>()),
                    new ArrayList<>(),
                    "LOW",
                    "缺少可溯源的资料支撑",
                    List.of("缺少匹配的知识卡片"),
                    "系统不会执行任何命令，请在实验环境自行验证。",
                    techStack,
                    "PARTIAL");
        }

        KnowledgeCardEntity card = cards.get(0);
        List<KnowledgeCardCauseEntity> causes = causeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeCardCauseEntity>()
                        .eq(KnowledgeCardCauseEntity::getCardId, card.getId())
                        .orderByAsc(KnowledgeCardCauseEntity::getSortNo));

        CitationView citation = new CitationView(
                "KNOWLEDGE_CARD",
                String.valueOf(card.getId()),
                card.getTitle(),
                truncate(card.getSymptom(), 200));
        List<CitationView> citations = List.of(citation);
        List<CauseView> causeViews = causes.isEmpty()
                ? List.of(new CauseView("可能与环境或配置有关", "MEDIUM", citations))
                : causes.stream()
                        .map(c -> new CauseView(c.getCauseText(), c.getConfidence(), citations))
                        .toList();

        return new AnswerView(
                questionId,
                "KNOWLEDGE_FALLBACK",
                restate(req.question()),
                card.getSymptom(),
                causeViews,
                new NextStepView(card.getSafeNextStep(), "请观察执行后的提示信息", "LOW", citations),
                citations,
                "MEDIUM",
                "基于知识卡片匹配（AI 未启用，走知识兜底）",
                List.of("未结合实时执行记录", "AI Provider 未配置"),
                "系统不会执行任何命令，请在实验环境自行验证。",
                techStack,
                "PARTIAL");
    }

    private String normalizeTech(String tech) {
        if (tech == null) {
            return null;
        }
        return switch (tech.toUpperCase()) {
            case "OPENGAUSS", "OPEN_GAUSS" -> "OPENGAUSS";
            case "MYSQL" -> "MYSQL";
            case "MYBATIS" -> "MYBATIS";
            default -> null;
        };
    }

    private String extractKeywords(String question) {
        return question.length() > 60 ? question.substring(0, 60) : question;
    }

    private String restate(String q) {
        return "你的问题是：" + (q.length() > 100 ? q.substring(0, 100) : q);
    }

    private String truncate(String s, int max) {
        return s == null ? null : (s.length() > max ? s.substring(0, max) : s);
    }
}