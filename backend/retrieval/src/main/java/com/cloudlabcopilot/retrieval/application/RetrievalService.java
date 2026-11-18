package com.cloudlabcopilot.retrieval.application;

import com.cloudlabcopilot.knowledge.infrastructure.entity.KnowledgeCardEntity;
import com.cloudlabcopilot.knowledge.infrastructure.mapper.KnowledgeCardMapper;
import com.cloudlabcopilot.retrieval.domain.RetrievalIntentExtractor;
import com.cloudlabcopilot.retrieval.domain.model.RetrievalIntent;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RetrievalService {

    private final KnowledgeCardMapper cardMapper;
    private final RetrievalIntentExtractor extractor;

    public RetrievalService(KnowledgeCardMapper cardMapper, RetrievalIntentExtractor extractor) {
        this.cardMapper = cardMapper;
        this.extractor = extractor;
    }

    public List<KnowledgeCardEntity> retrieve(String question, String techStack) {
        RetrievalIntent intent = extractor.extract(question, techStack);
        String like = "%" + intent.keywords() + "%";
        return cardMapper.search(intent.techStack(), intent.keywords(), like);
    }
}