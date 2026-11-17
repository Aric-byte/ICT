package com.cloudlabcopilot.lab.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudlabcopilot.common.exception.BusinessException;
import com.cloudlabcopilot.common.response.ErrorCode;
import com.cloudlabcopilot.lab.domain.guard.LabContentGuard;
import com.cloudlabcopilot.lab.domain.model.LabDefinition;
import com.cloudlabcopilot.lab.domain.model.LabStatus;
import com.cloudlabcopilot.lab.domain.model.LabStep;
import com.cloudlabcopilot.lab.domain.model.Precondition;
import com.cloudlabcopilot.lab.domain.model.TechStack;
import com.cloudlabcopilot.lab.domain.model.VersionStatus;
import com.cloudlabcopilot.lab.domain.validation.LabCodeValidator;
import com.cloudlabcopilot.lab.domain.validation.PreconditionValidator;
import com.cloudlabcopilot.lab.domain.validation.StepValidator;
import com.cloudlabcopilot.lab.domain.validation.TagValidator;
import com.cloudlabcopilot.lab.infrastructure.converter.LabConverter;
import com.cloudlabcopilot.lab.infrastructure.entity.LabDefinitionEntity;
import com.cloudlabcopilot.lab.infrastructure.entity.LabVersionEntity;
import com.cloudlabcopilot.lab.infrastructure.mapper.LabDefinitionMapper;
import com.cloudlabcopilot.lab.infrastructure.mapper.LabPreconditionMapper;
import com.cloudlabcopilot.lab.infrastructure.mapper.LabStepMapper;
import com.cloudlabcopilot.lab.infrastructure.mapper.LabVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class LabCommandService {

    private final LabDefinitionMapper definitionMapper;
    private final LabVersionMapper versionMapper;
    private final LabStepMapper stepMapper;
    private final LabPreconditionMapper preconditionMapper;
    private final LabConverter converter;
    private final LabCodeValidator labCodeValidator;
    private final TagValidator tagValidator;
    private final StepValidator stepValidator;
    private final PreconditionValidator preconditionValidator;
    private final LabContentGuard contentGuard;

    public LabCommandService(
            LabDefinitionMapper definitionMapper,
            LabVersionMapper versionMapper,
            LabStepMapper stepMapper,
            LabPreconditionMapper preconditionMapper,
            LabConverter converter,
            LabCodeValidator labCodeValidator,
            TagValidator tagValidator,
            StepValidator stepValidator,
            PreconditionValidator preconditionValidator,
            LabContentGuard contentGuard) {
        this.definitionMapper = definitionMapper;
        this.versionMapper = versionMapper;
        this.stepMapper = stepMapper;
        this.preconditionMapper = preconditionMapper;
        this.converter = converter;
        this.labCodeValidator = labCodeValidator;
        this.tagValidator = tagValidator;
        this.stepValidator = stepValidator;
        this.preconditionValidator = preconditionValidator;
        this.contentGuard = contentGuard;
    }

    @Transactional
    public LabDefinition createLab(LabDefinition input, String operatorId) {
        labCodeValidator.validate(input.getLabCode());
        tagValidator.validate(input.getTags());
        if (input.getTechStack() == null) {
            throw new BusinessException(ErrorCode.FIELD_REQUIRED, "techStack 不能为空");
        }
        if (input.getTechStack() == TechStack.OTHER
                && (input.getCustomTechStack() == null || input.getCustomTechStack().isBlank())) {
            throw new BusinessException(ErrorCode.FIELD_REQUIRED, "选择其他技术栈时必须填写自定义名称");
        }
        if (input.getDifficulty() == null) {
            throw new BusinessException(ErrorCode.FIELD_REQUIRED, "difficulty 不能为空");
        }
        if (input.getObjective() == null || input.getObjective().isBlank()) {
            throw new BusinessException(ErrorCode.LAB_OBJECTIVE_REQUIRED);
        }
        contentGuard.checkDefinition(input);
        Long count = definitionMapper.selectCount(
                new LambdaQueryWrapper<LabDefinitionEntity>()
                        .eq(LabDefinitionEntity::getLabCode, input.getLabCode()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.LAB_CODE_CONFLICT);
        }
        LabDefinitionEntity entity = converter.toEntity(input);
        entity.setId(null);
        entity.setStatus(LabStatus.DRAFT.name());
        entity.setEffectiveVersionNo(null);
        entity.setOwnerId(operatorId);
        entity.setRowVersion(0);
        entity.setDeleted(0);
        entity.setDraftSteps("[]");
        entity.setDraftPreconditions("[]");
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        definitionMapper.insert(entity);
        return converter.toDomain(entity);
    }

    @Transactional
    public LabDefinition updateLab(String labCode, LabDefinition input, String operatorId) {
        LabDefinitionEntity existing = loadByCode(labCode);
        if (!LabStatus.DRAFT.name().equals(existing.getStatus())) {
            throw new BusinessException(ErrorCode.LAB_NOT_DRAFT);
        }
        tagValidator.validate(input.getTags());
        if (input.getObjective() == null || input.getObjective().isBlank()) {
            throw new BusinessException(ErrorCode.LAB_OBJECTIVE_REQUIRED);
        }
        contentGuard.checkDefinition(input);
        existing.setName(input.getName());
        existing.setTechStack(input.getTechStack() == null ? null : input.getTechStack().name());
        existing.setCustomTechStack(input.getCustomTechStack());
        existing.setDifficulty(input.getDifficulty() == null ? null : input.getDifficulty().name());
        existing.setObjective(input.getObjective());
        existing.setEstimatedMinutes(input.getEstimatedMinutes());
        existing.setTags(converter.writeJson(input.getTags()));
        existing.setInternalNote(input.getInternalNote());
        existing.setUpdatedAt(OffsetDateTime.now());
        if (input.getRowVersion() != null) {
            existing.setRowVersion(input.getRowVersion());
        }
        int rows = definitionMapper.updateById(existing);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
        }
        return converter.toDomain(existing);
    }

    @Transactional
    public void saveSteps(String labCode, List<LabStep> steps, String operatorId) {
        LabDefinitionEntity existing = loadByCode(labCode);
        if (!LabStatus.DRAFT.name().equals(existing.getStatus())) {
            throw new BusinessException(ErrorCode.LAB_NOT_DRAFT);
        }
        stepValidator.validate(steps);
        contentGuard.checkSteps(steps);
        existing.setDraftSteps(converter.writeJson(steps));
        existing.setUpdatedAt(OffsetDateTime.now());
        definitionMapper.updateById(existing);
    }

    @Transactional
    public void savePreconditions(String labCode, List<Precondition> list, String operatorId) {
        LabDefinitionEntity existing = loadByCode(labCode);
        if (!LabStatus.DRAFT.name().equals(existing.getStatus())) {
            throw new BusinessException(ErrorCode.LAB_NOT_DRAFT);
        }
        preconditionValidator.validate(list);
        contentGuard.checkPreconditions(list);
        existing.setDraftPreconditions(converter.writeJson(list));
        existing.setUpdatedAt(OffsetDateTime.now());
        definitionMapper.updateById(existing);
    }

    @Transactional
    public void publish(String labCode, String operatorId) {
        LabDefinitionEntity existing = loadByCode(labCode);
        if (!LabStatus.DRAFT.name().equals(existing.getStatus())) {
            throw new BusinessException(ErrorCode.LAB_NOT_DRAFT);
        }
        LabDefinition def = converter.toDomain(existing);
        List<LabStep> steps = def.getDraftSteps();
        List<Precondition> preconditions = def.getDraftPreconditions();
        stepValidator.validate(steps);
        preconditionValidator.validate(preconditions);
        if (preconditionValidator.countRequired(preconditions) == 0) {
            throw new BusinessException(ErrorCode.LAB_PRECONDITION_REQUIRED_MISSING);
        }
        if (def.getObjective() == null || def.getObjective().isBlank()) {
            throw new BusinessException(ErrorCode.LAB_OBJECTIVE_REQUIRED);
        }
        contentGuard.checkSteps(steps);
        contentGuard.checkPreconditions(preconditions);
        LabVersionEntity maxVersion = versionMapper.selectOne(
                new LambdaQueryWrapper<LabVersionEntity>()
                        .eq(LabVersionEntity::getLabId, existing.getId())
                        .orderByDesc(LabVersionEntity::getVersionNo)
                        .last("limit 1"));
        int nextNo = (maxVersion == null ? 0 : maxVersion.getVersionNo()) + 1;
        String hash = computeHash(steps, preconditions);
        LabVersionEntity version = converter.toVersionEntity(def, nextNo, hash, operatorId);
        version.setPublishedAt(OffsetDateTime.now());
        version.setCreatedAt(OffsetDateTime.now());
        versionMapper.insert(version);
        for (LabStep step : steps) {
            stepMapper.insert(converter.toEntity(step, version.getId()));
        }
        for (Precondition p : preconditions) {
            preconditionMapper.insert(converter.toEntity(p, version.getId()));
        }
        existing.setStatus(LabStatus.PUBLISHED.name());
        existing.setEffectiveVersionNo(nextNo);
        existing.setUpdatedAt(OffsetDateTime.now());
        definitionMapper.updateById(existing);
    }

    @Transactional
    public void unpublish(String labCode, String operatorId) {
        LabDefinitionEntity existing = loadByCode(labCode);
        if (!LabStatus.PUBLISHED.name().equals(existing.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "仅已发布实验可下架");
        }
        existing.setStatus(LabStatus.UNPUBLISHED.name());
        existing.setUpdatedAt(OffsetDateTime.now());
        definitionMapper.updateById(existing);
    }

    @Transactional
    public void switchEffectiveVersion(String labCode, int versionNo, String operatorId) {
        LabDefinitionEntity existing = loadByCode(labCode);
        LabVersionEntity version = versionMapper.selectOne(
                new LambdaQueryWrapper<LabVersionEntity>()
                        .eq(LabVersionEntity::getLabId, existing.getId())
                        .eq(LabVersionEntity::getVersionNo, versionNo));
        if (version == null) {
            throw new BusinessException(ErrorCode.VERSION_NOT_FOUND);
        }
        if (!VersionStatus.PUBLISHED.name().equals(version.getStatus())) {
            throw new BusinessException(ErrorCode.VERSION_ARCHIVE_NOT_EFFECTIVE);
        }
        existing.setEffectiveVersionNo(versionNo);
        existing.setStatus(LabStatus.PUBLISHED.name());
        existing.setUpdatedAt(OffsetDateTime.now());
        definitionMapper.updateById(existing);
    }

    @Transactional
    public void deleteLab(String labCode, String operatorId) {
        LabDefinitionEntity existing = loadByCode(labCode);
        if (!LabStatus.DRAFT.name().equals(existing.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "已发布实验不可删除，请先下架");
        }
        definitionMapper.deleteById(existing.getId());
    }

    public LabDefinitionEntity loadByCode(String labCode) {
        LabDefinitionEntity entity = definitionMapper.selectOne(
                new LambdaQueryWrapper<LabDefinitionEntity>()
                        .eq(LabDefinitionEntity::getLabCode, labCode));
        if (entity == null) {
            throw new BusinessException(ErrorCode.LAB_NOT_FOUND);
        }
        return entity;
    }

    private String computeHash(List<LabStep> steps, List<Precondition> preconditions) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String content = converter.writeJson(steps) + converter.writeJson(preconditions);
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }
}