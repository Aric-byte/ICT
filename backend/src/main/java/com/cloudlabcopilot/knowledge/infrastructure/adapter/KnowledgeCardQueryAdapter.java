package com.cloudlabcopilot.knowledge.infrastructure.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudlabcopilot.knowledge.domain.port.KnowledgeCardQueryPort;
import com.cloudlabcopilot.knowledge.infrastructure.converter.KnowledgeCardConverter;
import com.cloudlabcopilot.knowledge.infrastructure.entity.KnowledgeCardEntity;
import com.cloudlabcopilot.knowledge.infrastructure.mapper.KnowledgeCardMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class KnowledgeCardQueryAdapter implements KnowledgeCardQueryPort {

    private final KnowledgeCardMapper cardMapper;
    private final KnowledgeCardConverter converter;

    public KnowledgeCardQueryAdapter(KnowledgeCardMapper cardMapper, KnowledgeCardConverter converter) {
        this.cardMapper = cardMapper;
        this.converter = converter;
    }

    @Override
    public Optional<KnowledgeCardSnapshot> findActiveById(Long cardId) {
        if (cardId == null) {
            return Optional.empty();
        }
        KnowledgeCardEntity entity = cardMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeCardEntity>()
                        .eq(KnowledgeCardEntity::getId, cardId)
                        .eq(KnowledgeCardEntity::getStatus, "PUBLISHED")
                        .eq(KnowledgeCardEntity::getReviewStatus, "APPROVED"));
        return entity == null ? Optional.empty() : Optional.of(toSnapshot(entity));
    }

    @Override
    public List<KnowledgeCardSnapshot> findActiveByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<KnowledgeCardEntity> entities = cardMapper.selectList(
                new LambdaQueryWrapper<KnowledgeCardEntity>()
                        .in(KnowledgeCardEntity::getId, ids)
                        .eq(KnowledgeCardEntity::getStatus, "PUBLISHED")
                        .eq(KnowledgeCardEntity::getReviewStatus, "APPROVED"));
        return entities.stream().map(this::toSnapshot).toList();
    }

    private KnowledgeCardSnapshot toSnapshot(KnowledgeCardEntity entity) {
        return new KnowledgeCardSnapshot(
                entity.getId(),
                entity.getTechStack(),
                entity.getTitle(),
                entity.getStatus(),
                entity.getReviewStatus(),
                converter.toDomain(entity, null, null).getApplicableVersions(),
                entity.getSourceType(),
                entity.getSourceRef());
    }
}