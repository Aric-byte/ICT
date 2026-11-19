package com.cloudlabcopilot.knowledge.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudlabcopilot.common.exception.BusinessException;
import com.cloudlabcopilot.common.response.ErrorCode;
import com.cloudlabcopilot.knowledge.api.dto.KnowledgeDtos.CardView;
import com.cloudlabcopilot.knowledge.api.dto.KnowledgeDtos.PagedCards;
import com.cloudlabcopilot.knowledge.domain.guard.KnowledgeCardGuard;
import com.cloudlabcopilot.knowledge.domain.model.KnowledgeCard;
import com.cloudlabcopilot.knowledge.infrastructure.converter.KnowledgeCardConverter;
import com.cloudlabcopilot.knowledge.infrastructure.entity.KnowledgeCardCauseEntity;
import com.cloudlabcopilot.knowledge.infrastructure.entity.KnowledgeCardEntity;
import com.cloudlabcopilot.knowledge.infrastructure.entity.KnowledgeCardErrorPatternEntity;
import com.cloudlabcopilot.knowledge.infrastructure.mapper.KnowledgeCardCauseMapper;
import com.cloudlabcopilot.knowledge.infrastructure.mapper.KnowledgeCardErrorPatternMapper;
import com.cloudlabcopilot.knowledge.infrastructure.mapper.KnowledgeCardMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class KnowledgeCardService {

    private final KnowledgeCardMapper cardMapper;
    private final KnowledgeCardErrorPatternMapper patternMapper;
    private final KnowledgeCardCauseMapper causeMapper;
    private final KnowledgeCardConverter converter;
    private final KnowledgeCardGuard guard;

    public KnowledgeCardService(
            KnowledgeCardMapper cardMapper,
            KnowledgeCardErrorPatternMapper patternMapper,
            KnowledgeCardCauseMapper causeMapper,
            KnowledgeCardConverter converter,
            KnowledgeCardGuard guard) {
        this.cardMapper = cardMapper;
        this.patternMapper = patternMapper;
        this.causeMapper = causeMapper;
        this.converter = converter;
        this.guard = guard;
    }

    @Transactional
    public KnowledgeCard createCard(KnowledgeCard input) {
        guard.validate(input);
        input.setStatus("DRAFT");
        input.setReviewStatus("PENDING");
        KnowledgeCardEntity entity = converter.toEntity(input);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        cardMapper.insert(entity);
        if (input.getErrorPatterns() != null) {
            for (KnowledgeCard.ErrorPattern p : input.getErrorPatterns()) {
                patternMapper.insert(converter.toPatternEntity(p, entity.getId()));
            }
        }
        if (input.getCauses() != null) {
            for (KnowledgeCard.Cause c : input.getCauses()) {
                causeMapper.insert(converter.toCauseEntity(c, entity.getId()));
            }
        }
        return loadCard(entity.getId());
    }

    @Transactional
    public KnowledgeCard updateStatus(Long cardId, String status, String reviewStatus) {
        KnowledgeCardEntity entity = cardMapper.selectById(cardId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识卡片不存在");
        }
        if ("PUBLISHED".equals(status) && !"APPROVED".equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "发布前必须审核通过");
        }
        entity.setStatus(status);
        entity.setReviewStatus(reviewStatus);
        entity.setUpdatedAt(OffsetDateTime.now());
        cardMapper.updateById(entity);
        return loadCard(cardId);
    }

    public PagedCards listCards(String techStack, String status, String keyword, int page, int pageSize) {
        LambdaQueryWrapper<KnowledgeCardEntity> wrapper = new LambdaQueryWrapper<>();
        if (techStack != null && !techStack.isBlank()) {
            wrapper.eq(KnowledgeCardEntity::getTechStack, techStack);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(KnowledgeCardEntity::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(KnowledgeCardEntity::getTitle, keyword)
                    .or()
                    .like(KnowledgeCardEntity::getSymptom, keyword));
        }
        wrapper.orderByDesc(KnowledgeCardEntity::getUpdatedAt);
        IPage<KnowledgeCardEntity> result = cardMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<CardView> items = result.getRecords().stream().map(this::toView).toList();
        return new PagedCards(items, result.getTotal(), page, pageSize);
    }

    private KnowledgeCard loadCard(Long id) {
        KnowledgeCardEntity entity = cardMapper.selectById(id);
        List<KnowledgeCardErrorPatternEntity> patterns = patternMapper.selectList(
                new LambdaQueryWrapper<KnowledgeCardErrorPatternEntity>()
                        .eq(KnowledgeCardErrorPatternEntity::getCardId, id)
                        .orderByAsc(KnowledgeCardErrorPatternEntity::getSortNo));
        List<KnowledgeCardCauseEntity> causes = causeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeCardCauseEntity>()
                        .eq(KnowledgeCardCauseEntity::getCardId, id)
                        .orderByAsc(KnowledgeCardCauseEntity::getSortNo));
        return converter.toDomain(entity, patterns, causes);
    }

    private CardView toView(KnowledgeCardEntity e) {
        return new CardView(
                e.getId(),
                e.getTechStack(),
                e.getTitle(),
                e.getSymptom(),
                e.getSafeNextStep(),
                e.getStatus(),
                e.getReviewStatus(),
                converter.toDomain(e, null, null).getApplicableVersions());
    }
}