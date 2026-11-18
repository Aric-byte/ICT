package com.cloudlabcopilot.knowledge.domain.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface KnowledgeCardQueryPort {

    Optional<KnowledgeCardSnapshot> findActiveById(Long cardId);

    List<KnowledgeCardSnapshot> findActiveByIds(Collection<Long> ids);

    record KnowledgeCardSnapshot(
            Long id,
            String techStack,
            String title,
            String status,
            String reviewStatus,
            List<String> applicableVersions,
            String sourceType,
            String sourceRef) {
    }
}