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
import com.cloudlabcopilot.diagnosis.domain.parser.JavaErrorInfo;
import com.cloudlabcopilot.diagnosis.domain.parser.JavaErrorParser;
import com.cloudlabcopilot.diagnosis.domain.port.ChatProviderPort;
import com.cloudlabcopilot.knowledge.infrastructure.entity.KnowledgeCardCauseEntity;
import com.cloudlabcopilot.knowledge.infrastructure.entity.KnowledgeCardEntity;
import com.cloudlabcopilot.knowledge.infrastructure.mapper.KnowledgeCardCauseMapper;
import com.cloudlabcopilot.knowledge.infrastructure.mapper.KnowledgeCardMapper;
import com.cloudlabcopilot.safety.application.SafetyChecker;
import com.cloudlabcopilot.safety.domain.model.SafetyCheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DiagnosisService {

    private static final Logger log = LoggerFactory.getLogger(DiagnosisService.class);

    private final KnowledgeCardMapper cardMapper;
    private final KnowledgeCardCauseMapper causeMapper;
    private final SafetyChecker safetyChecker;
    private final JavaErrorParser javaErrorParser;
    private final Optional<ChatProviderPort> chatProvider;

    @Autowired
    public DiagnosisService(
            KnowledgeCardMapper cardMapper,
            KnowledgeCardCauseMapper causeMapper,
            SafetyChecker safetyChecker,
            JavaErrorParser javaErrorParser,
            Optional<ChatProviderPort> chatProvider) {
        this.cardMapper = cardMapper;
        this.causeMapper = causeMapper;
        this.safetyChecker = safetyChecker;
        this.javaErrorParser = javaErrorParser;
        this.chatProvider = chatProvider;
    }

    @Transactional(readOnly = true)
    public AnswerView ask(AskRequest req) {
        if (req.question() == null || req.question().isBlank()) {
            throw new BusinessException(ErrorCode.DIAGNOSIS_QUESTION_REQUIRED);
        }
        if (req.question().length() > 20000) {
            throw new BusinessException(ErrorCode.TEXT_TOO_LONG);
        }

        // 1. 安全护栏：输入敏感信息与凭据检查
        SafetyCheckResult inputCheck = safetyChecker.checkInput(req.question());
        if (inputCheck.dangerous() && "CREDENTIAL".equals(inputCheck.category())) {
            throw new BusinessException(ErrorCode.SENSITIVE_CONTENT_REJECTED);
        }

        String questionId = PublicIdGenerator.nextId();
        String fullInputText = req.question() + (req.pastedText() != null ? "\n" + req.pastedText() : "");

        // 2. Java 专属语义与堆栈感知解析
        JavaErrorInfo javaInfo = javaErrorParser.parse(fullInputText);

        // 3. 确定技术栈与关键词检索
        String techStack = resolveTechStack(req.techStack(), javaInfo);
        String primaryKeyword = !javaInfo.keywords().isEmpty() ? javaInfo.keywords().get(0) : "Java";
        String likeQuery = "%" + primaryKeyword + "%";

        List<KnowledgeCardEntity> cards = cardMapper.search(techStack, primaryKeyword, likeQuery);
        if (cards.isEmpty() && !"Java".equalsIgnoreCase(primaryKeyword)) {
            // 尝试更宽泛的检索
            cards = cardMapper.search(null, primaryKeyword, likeQuery);
        }

        // 4. 尝试通过 AI Provider 进行推理
        if (chatProvider.isPresent() && chatProvider.get().isConfigured()) {
            try {
                AnswerView aiAnswer = tryAiDiagnosis(questionId, req, javaInfo, cards, techStack);
                if (aiAnswer != null) {
                    return aiAnswer;
                }
            } catch (Exception e) {
                log.warn("AI Provider 推理失败，自动降级为规则与知识增强模式: {}", e.getMessage());
            }
        }

        // 5. 规则与知识库增强诊断（本地高可用引擎）
        return buildRuleAndKnowledgeAnswer(questionId, req, javaInfo, cards, techStack);
    }

    private AnswerView tryAiDiagnosis(
            String questionId,
            AskRequest req,
            JavaErrorInfo javaInfo,
            List<KnowledgeCardEntity> cards,
            String techStack) {

        ChatProviderPort provider = chatProvider.orElseThrow();
        StringBuilder sysPrompt = new StringBuilder();
        sysPrompt.append("你是一位精通 Java、Spring Boot、MyBatis 与华为云架构的专家级诊断智能体。\n");
        sysPrompt.append("请针对学生的提问或报错进行分析，回答必须包含：1. 核心原因定位；2. 最小安全修复下一步（提供代码修正片段）；3. 验证步骤。\n");
        if (!cards.isEmpty()) {
            sysPrompt.append("参考以下知识条目：\n");
            for (int i = 0; i < Math.min(2, cards.size()); i++) {
                sysPrompt.append("- ").append(cards.get(i).getTitle()).append(": ").append(cards.get(i).getSymptom()).append("\n");
            }
        }

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("【用户问题】: ").append(req.question()).append("\n");
        if (javaInfo.detected()) {
            userPrompt.append("【堆栈感知】: 异常类型=").append(javaInfo.exceptionClass())
                    .append(", 根本原因=").append(javaInfo.rootCause())
                    .append(javaInfo.userCodeLocation() != null ? ", 源码位置=" + javaInfo.userCodeLocation() : "").append("\n");
        }

        ChatProviderPort.ChatResponse resp = provider.chat(new ChatProviderPort.ChatRequest(
                sysPrompt.toString(),
                List.of(new ChatProviderPort.ChatMessage("user", userPrompt.toString())),
                2000));

        if (resp != null && resp.content() != null && !resp.content().isBlank()) {
            List<CitationView> citations = buildCitations(cards);
            String safeNextStep = "按照上述建议修改代码并重新编译运行验证";
            SafetyCheckResult nextStepCheck = safetyChecker.checkSuggestion(resp.content());
            if (nextStepCheck.dangerous()) {
                safeNextStep = nextStepCheck.safeAlternative();
            }

            return new AnswerView(
                    questionId,
                    "NORMAL",
                    restate(req.question()),
                    resp.content(),
                    List.of(new CauseView(javaInfo.detected() ? javaInfo.rootCause() : "大模型综合诊断结论", "HIGH", citations)),
                    new NextStepView(safeNextStep, "观察编译与启动日志是否正常", "LOW", citations),
                    citations,
                    "HIGH",
                    "基于华为云智能模型与 Java 异常感知联合推理",
                    new ArrayList<>(),
                    nextStepCheck.dangerous() ? "注意安全提示: " + nextStepCheck.description() : "系统不会自动执行破坏性操作，请自行验证。",
                    techStack,
                    javaInfo.detected() ? "COMPLETE" : "PARTIAL");
        }
        return null;
    }

    private AnswerView buildRuleAndKnowledgeAnswer(
            String questionId,
            AskRequest req,
            JavaErrorInfo javaInfo,
            List<KnowledgeCardEntity> cards,
            String techStack) {

        List<CitationView> citations = buildCitations(cards);
        List<CauseView> causes = new ArrayList<>();

        String explanation;
        String safeNextStep;
        String confidence;
        String confidenceReason;

        if (javaInfo.detected()) {
            // 成功解析到 Java 堆栈或典型错误
            explanation = "【Java 异常定位与诊断】\n"
                    + "• 异常分类: " + javaInfo.errorCategory() + "\n"
                    + "• 根本原因: " + javaInfo.rootCause() + "\n"
                    + (javaInfo.userCodeLocation() != null ? "• 目标源码定位: " + javaInfo.userCodeLocation() + "\n" : "")
                    + "• 排查建议: " + javaInfo.heuristicHint();

            causes.add(new CauseView(javaInfo.rootCause(), "HIGH", citations));
            safeNextStep = javaInfo.userCodeLocation() != null
                    ? "前往 " + javaInfo.userCodeLocation() + " 检查变量赋值或注解配置，重新编译运行。"
                    : javaInfo.heuristicHint();
            confidence = "HIGH";
            confidenceReason = "成功提取 Java 错误堆栈与源码定位（本地专家感知引擎）";
        } else if (!cards.isEmpty()) {
            // 匹配到预置知识卡片
            KnowledgeCardEntity card = cards.get(0);
            List<KnowledgeCardCauseEntity> dbCauses = causeMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeCardCauseEntity>()
                            .eq(KnowledgeCardCauseEntity::getCardId, card.getId())
                            .orderByAsc(KnowledgeCardCauseEntity::getSortNo));

            explanation = card.getSymptom();
            if (!dbCauses.isEmpty()) {
                dbCauses.forEach(c -> causes.add(new CauseView(c.getCauseText(), c.getConfidence(), citations)));
            } else {
                causes.add(new CauseView("可能与配置或参数有关", "MEDIUM", citations));
            }
            safeNextStep = card.getSafeNextStep();
            confidence = "MEDIUM";
            confidenceReason = "匹配到预置技术知识卡片";
        } else {
            // 未匹配到
            explanation = "未能在知识库中找到高度吻合的记录。已对问题进行关键词提取，建议提供完整代码或详细异常堆栈。";
            causes.add(new CauseView("线索不充分，需补充上下文", "LOW", citations));
            safeNextStep = "请提供具体的报错日志（含完整 StackTrace）或当前调用的 Java 类代码。";
            confidence = "LOW";
            confidenceReason = "未包含标准堆栈且知识库未命中";
        }

        SafetyCheckResult nextStepCheck = safetyChecker.checkSuggestion(safeNextStep);
        if (nextStepCheck.dangerous()) {
            safeNextStep = nextStepCheck.safeAlternative();
        }

        return new AnswerView(
                questionId,
                "KNOWLEDGE_FALLBACK",
                restate(req.question()),
                explanation,
                causes,
                new NextStepView(safeNextStep, "观察编译或执行输出，确认问题是否解除", "LOW", citations),
                citations,
                confidence,
                confidenceReason,
                javaInfo.detected() ? List.of("当前运行在离线智能感知模式") : List.of("缺少完整异常堆栈"),
                nextStepCheck.dangerous() ? "安全护栏拦截: " + nextStepCheck.description() : "系统不会自动执行任何命令，请在安全环境中验证。",
                techStack,
                javaInfo.detected() ? "COMPLETE" : "PARTIAL");
    }

    private List<CitationView> buildCitations(List<KnowledgeCardEntity> cards) {
        if (cards == null || cards.isEmpty()) {
            return new ArrayList<>();
        }
        return cards.stream().limit(3).map(card -> new CitationView(
                "KNOWLEDGE_CARD",
                String.valueOf(card.getId()),
                card.getTitle(),
                truncate(card.getSymptom(), 150)
        )).toList();
    }

    private String resolveTechStack(String requestedTech, JavaErrorInfo javaInfo) {
        if (requestedTech != null && !requestedTech.isBlank()) {
            return requestedTech.toUpperCase();
        }
        if (javaInfo.detected()) {
            if ("SPRING_FRAMEWORK".equals(javaInfo.errorCategory())) return "SPRING";
            if ("MYBATIS_DATABASE".equals(javaInfo.errorCategory())) return "MYBATIS";
            return "JAVA";
        }
        return "JAVA";
    }

    private String restate(String q) {
        return "你的问题是：" + (q.length() > 80 ? q.substring(0, 80) + "..." : q);
    }

    private String truncate(String s, int max) {
        return s == null ? null : (s.length() > max ? s.substring(0, max) : s);
    }
}
