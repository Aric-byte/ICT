package com.cloudlabcopilot.knowledge.api;

import com.cloudlabcopilot.common.response.ApiResponse;
import com.cloudlabcopilot.common.security.CurrentOperator;
import com.cloudlabcopilot.common.security.OperatorPrincipal;
import com.cloudlabcopilot.knowledge.api.dto.KnowledgeDtos.CardView;
import com.cloudlabcopilot.knowledge.api.dto.KnowledgeDtos.CauseDto;
import com.cloudlabcopilot.knowledge.api.dto.KnowledgeDtos.CreateCardRequest;
import com.cloudlabcopilot.knowledge.api.dto.KnowledgeDtos.ErrorPatternDto;
import com.cloudlabcopilot.knowledge.api.dto.KnowledgeDtos.PagedCards;
import com.cloudlabcopilot.knowledge.api.dto.KnowledgeDtos.UpdateStatusRequest;
import com.cloudlabcopilot.knowledge.application.KnowledgeCardService;
import com.cloudlabcopilot.knowledge.domain.model.KnowledgeCard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge/cards")
public class KnowledgeCardController {

    private final KnowledgeCardService service;

    public KnowledgeCardController(KnowledgeCardService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<CardView> create(
            @RequestBody CreateCardRequest req, @CurrentOperator OperatorPrincipal op) {
        KnowledgeCard input = KnowledgeCard.builder()
                .techStack(req.techStack())
                .title(req.title())
                .symptom(req.symptom())
                .safeNextStep(req.safeNextStep())
                .verificationMethod(req.verificationMethod())
                .caseOrigin(req.caseOrigin())
                .sourceType(req.sourceType())
                .sourceRef(req.sourceRef())
                .applicableVersions(req.applicableVersions())
                .errorPatterns(req.errorPatterns() == null ? null
                        : req.errorPatterns().stream()
                                .map(p -> KnowledgeCard.ErrorPattern.builder()
                                        .pattern(p.pattern())
                                        .matchType(p.matchType())
                                        .sortNo(p.sortNo())
                                        .build())
                                .toList())
                .causes(req.causes() == null ? null
                        : req.causes().stream()
                                .map(c -> KnowledgeCard.Cause.builder()
                                        .text(c.text())
                                        .confidence(c.confidence())
                                        .sortNo(c.sortNo())
                                        .build())
                                .toList())
                .build();
        return ApiResponse.success(toView(service.createCard(input)));
    }

    @PutMapping("/{cardId}")
    public ApiResponse<CardView> updateStatus(
            @PathVariable Long cardId,
            @RequestBody UpdateStatusRequest req,
            @CurrentOperator OperatorPrincipal op) {
        return ApiResponse.success(toView(service.updateStatus(cardId, req.status(), req.reviewStatus())));
    }

    @GetMapping
    public ApiResponse<PagedCards> list(
            @RequestParam(required = false) String techStack,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(service.listCards(techStack, status, keyword, page, pageSize));
    }

    private CardView toView(KnowledgeCard card) {
        return new CardView(
                card.getId(),
                card.getTechStack(),
                card.getTitle(),
                card.getSymptom(),
                card.getSafeNextStep(),
                card.getStatus(),
                card.getReviewStatus(),
                card.getApplicableVersions());
    }
}