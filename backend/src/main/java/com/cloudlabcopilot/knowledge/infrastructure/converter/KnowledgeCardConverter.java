package com.cloudlabcopilot.knowledge.infrastructure.converter;

import com.cloudlabcopilot.knowledge.domain.model.KnowledgeCard;
import com.cloudlabcopilot.knowledge.infrastructure.entity.KnowledgeCardCauseEntity;
import com.cloudlabcopilot.knowledge.infrastructure.entity.KnowledgeCardEntity;
import com.cloudlabcopilot.knowledge.infrastructure.entity.KnowledgeCardErrorPatternEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class KnowledgeCardConverter {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public KnowledgeCardConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public KnowledgeCard toDomain(
            KnowledgeCardEntity entity,
            List<KnowledgeCardErrorPatternEntity> patterns,
            List<KnowledgeCardCauseEntity> causes) {
        return KnowledgeCard.builder()
                .id(entity.getId())
                .techStack(entity.getTechStack())
                .title(entity.getTitle())
                .symptom(entity.getSymptom())
                .safeNextStep(entity.getSafeNextStep())
                .verificationMethod(entity.getVerificationMethod())
                .caseOrigin(entity.getCaseOrigin())
                .sourceType(entity.getSourceType())
                .sourceRef(entity.getSourceRef())
                .applicableVersions(readVersions(entity.getApplicableVersions()))
                .status(entity.getStatus())
                .reviewStatus(entity.getReviewStatus())
                .errorPatterns(patterns == null ? new ArrayList<>()
                        : patterns.stream()
                                .map(p -> KnowledgeCard.ErrorPattern.builder()
                                        .pattern(p.getPattern())
                                        .matchType(p.getMatchType())
                                        .sortNo(p.getSortNo() == null ? 1 : p.getSortNo())
                                        .build())
                                .toList())
                .causes(causes == null ? new ArrayList<>()
                        : causes.stream()
                                .map(c -> KnowledgeCard.Cause.builder()
                                        .text(c.getCauseText())
                                        .confidence(c.getConfidence())
                                        .sortNo(c.getSortNo() == null ? 1 : c.getSortNo())
                                        .build())
                                .toList())
                .build();
    }

    public KnowledgeCardEntity toEntity(KnowledgeCard card) {
        KnowledgeCardEntity entity = new KnowledgeCardEntity();
        entity.setId(card.getId());
        entity.setTechStack(card.getTechStack());
        entity.setTitle(card.getTitle());
        entity.setSymptom(card.getSymptom());
        entity.setSafeNextStep(card.getSafeNextStep());
        entity.setVerificationMethod(card.getVerificationMethod());
        entity.setCaseOrigin(card.getCaseOrigin());
        entity.setSourceType(card.getSourceType());
        entity.setSourceRef(card.getSourceRef());
        entity.setApplicableVersions(writeVersions(card.getApplicableVersions()));
        entity.setStatus(card.getStatus());
        entity.setReviewStatus(card.getReviewStatus());
        return entity;
    }

    public KnowledgeCardErrorPatternEntity toPatternEntity(KnowledgeCard.ErrorPattern p, Long cardId) {
        KnowledgeCardErrorPatternEntity entity = new KnowledgeCardErrorPatternEntity();
        entity.setCardId(cardId);
        entity.setPattern(p.getPattern());
        entity.setMatchType(p.getMatchType() == null ? "KEYWORD" : p.getMatchType());
        entity.setSortNo(p.getSortNo() == 0 ? 1 : p.getSortNo());
        return entity;
    }

    public KnowledgeCardCauseEntity toCauseEntity(KnowledgeCard.Cause c, Long cardId) {
        KnowledgeCardCauseEntity entity = new KnowledgeCardCauseEntity();
        entity.setCardId(cardId);
        entity.setCauseText(c.getText());
        entity.setConfidence(c.getConfidence() == null ? "MEDIUM" : c.getConfidence());
        entity.setSortNo(c.getSortNo() == 0 ? 1 : c.getSortNo());
        return entity;
    }

    private List<String> readVersions(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String writeVersions(List<String> versions) {
        try {
            return versions == null ? "[]" : objectMapper.writeValueAsString(versions);
        } catch (Exception e) {
            return "[]";
        }
    }
}