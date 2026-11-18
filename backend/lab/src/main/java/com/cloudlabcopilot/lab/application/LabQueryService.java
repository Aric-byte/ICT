package com.cloudlabcopilot.lab.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudlabcopilot.common.exception.BusinessException;
import com.cloudlabcopilot.common.response.ErrorCode;
import com.cloudlabcopilot.lab.api.dto.LabDtos.LabDetailView;
import com.cloudlabcopilot.lab.api.dto.LabDtos.LabSummaryView;
import com.cloudlabcopilot.lab.api.dto.LabDtos.PagedLabView;
import com.cloudlabcopilot.lab.api.dto.LabDtos.PreconditionView;
import com.cloudlabcopilot.lab.api.dto.LabDtos.StartEligibilityView;
import com.cloudlabcopilot.lab.api.dto.LabDtos.StepDiffView;
import com.cloudlabcopilot.lab.api.dto.LabDtos.StepView;
import com.cloudlabcopilot.lab.api.dto.LabDtos.VersionDiffView;
import com.cloudlabcopilot.lab.api.dto.LabDtos.VersionView;
import com.cloudlabcopilot.lab.domain.model.LabDefinition;
import com.cloudlabcopilot.lab.domain.model.LabStatus;
import com.cloudlabcopilot.lab.domain.model.LabStep;
import com.cloudlabcopilot.lab.domain.model.Precondition;
import com.cloudlabcopilot.lab.infrastructure.converter.LabConverter;
import com.cloudlabcopilot.lab.infrastructure.entity.LabDefinitionEntity;
import com.cloudlabcopilot.lab.infrastructure.entity.LabPreconditionEntity;
import com.cloudlabcopilot.lab.infrastructure.entity.LabStepEntity;
import com.cloudlabcopilot.lab.infrastructure.entity.LabVersionEntity;
import com.cloudlabcopilot.lab.infrastructure.mapper.LabDefinitionMapper;
import com.cloudlabcopilot.lab.infrastructure.mapper.LabPreconditionMapper;
import com.cloudlabcopilot.lab.infrastructure.mapper.LabStepMapper;
import com.cloudlabcopilot.lab.infrastructure.mapper.LabVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Service
@Transactional(readOnly = true)
public class LabQueryService {

    private final LabDefinitionMapper definitionMapper;
    private final LabVersionMapper versionMapper;
    private final LabStepMapper stepMapper;
    private final LabPreconditionMapper preconditionMapper;
    private final LabConverter converter;

    public LabQueryService(
            LabDefinitionMapper definitionMapper,
            LabVersionMapper versionMapper,
            LabStepMapper stepMapper,
            LabPreconditionMapper preconditionMapper,
            LabConverter converter) {
        this.definitionMapper = definitionMapper;
        this.versionMapper = versionMapper;
        this.stepMapper = stepMapper;
        this.preconditionMapper = preconditionMapper;
        this.converter = converter;
    }

    public PagedLabView listCatalog(String techStack, String difficulty, String keyword, int page, int size) {
        LambdaQueryWrapper<LabDefinitionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LabDefinitionEntity::getStatus, LabStatus.PUBLISHED.name());
        if (techStack != null && !techStack.isBlank()) {
            wrapper.eq(LabDefinitionEntity::getTechStack, techStack);
        }
        if (difficulty != null && !difficulty.isBlank()) {
            wrapper.eq(LabDefinitionEntity::getDifficulty, difficulty);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(LabDefinitionEntity::getName, keyword)
                    .or()
                    .like(LabDefinitionEntity::getObjective, keyword));
        }
        wrapper.orderByDesc(LabDefinitionEntity::getUpdatedAt).orderByDesc(LabDefinitionEntity::getId);
        IPage<LabDefinitionEntity> result = definitionMapper.selectPage(new Page<>(page, size), wrapper);
        List<LabSummaryView> items = result.getRecords().stream().map(this::toSummary).toList();
        return new PagedLabView(items, result.getTotal(), page, size);
    }

    public LabDetailView getStudentDetail(String labCode) {
        LabDefinitionEntity entity = definitionMapper.selectOne(
                new LambdaQueryWrapper<LabDefinitionEntity>()
                        .eq(LabDefinitionEntity::getLabCode, labCode)
                        .eq(LabDefinitionEntity::getStatus, LabStatus.PUBLISHED.name()));
        if (entity == null || entity.getEffectiveVersionNo() == null) {
            throw new BusinessException(ErrorCode.LAB_NOT_FOUND);
        }
        LabVersionEntity version = versionMapper.selectOne(
                new LambdaQueryWrapper<LabVersionEntity>()
                        .eq(LabVersionEntity::getLabId, entity.getId())
                        .eq(LabVersionEntity::getVersionNo, entity.getEffectiveVersionNo()));
        if (version == null) {
            throw new BusinessException(ErrorCode.LAB_NOT_FOUND);
        }
        List<StepView> steps = stepMapper.selectList(
                new LambdaQueryWrapper<LabStepEntity>()
                        .eq(LabStepEntity::getVersionId, version.getId())
                        .orderByAsc(LabStepEntity::getStepNo))
                .stream().map(this::toStepView).toList();
        List<PreconditionView> preconditions = preconditionMapper.selectList(
                new LambdaQueryWrapper<LabPreconditionEntity>()
                        .eq(LabPreconditionEntity::getVersionId, version.getId())
                        .orderByAsc(LabPreconditionEntity::getSortNo))
                .stream().map(this::toPreconditionView).toList();
        LabDefinition domain = converter.toDomain(entity);
        return new LabDetailView(
                entity.getId(),
                entity.getLabCode(),
                entity.getName(),
                entity.getTechStack(),
                entity.getDifficulty(),
                entity.getObjective(),
                entity.getEstimatedMinutes(),
                domain.getTags(),
                entity.getEffectiveVersionNo(),
                steps,
                preconditions);
    }

    public StartEligibilityView checkEligibility(String labCode) {
        LabDefinitionEntity entity = definitionMapper.selectOne(
                new LambdaQueryWrapper<LabDefinitionEntity>()
                        .eq(LabDefinitionEntity::getLabCode, labCode));
        if (entity == null) {
            throw new BusinessException(ErrorCode.LAB_NOT_FOUND);
        }
        List<String> blockReasons = new ArrayList<>();
        if (!LabStatus.PUBLISHED.name().equals(entity.getStatus())) {
            blockReasons.add("实验当前不可开始（未发布或已下架）");
        }
        if (entity.getEffectiveVersionNo() == null) {
            blockReasons.add("实验无生效版本");
        }
        List<PreconditionView> preconditions = new ArrayList<>();
        Integer versionNo = entity.getEffectiveVersionNo();
        if (versionNo != null) {
            LabVersionEntity version = versionMapper.selectOne(
                    new LambdaQueryWrapper<LabVersionEntity>()
                            .eq(LabVersionEntity::getLabId, entity.getId())
                            .eq(LabVersionEntity::getVersionNo, versionNo));
            if (version != null) {
                preconditions = preconditionMapper.selectList(
                        new LambdaQueryWrapper<LabPreconditionEntity>()
                                .eq(LabPreconditionEntity::getVersionId, version.getId())
                                .orderByAsc(LabPreconditionEntity::getSortNo))
                        .stream().map(this::toPreconditionView).toList();
            }
        }
        return new StartEligibilityView(
                entity.getLabCode(), versionNo, blockReasons.isEmpty(), null, blockReasons, preconditions);
    }

    public PagedLabView listForTeaching(String status, int page, int size) {
        LambdaQueryWrapper<LabDefinitionEntity> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(LabDefinitionEntity::getStatus, status);
        }
        wrapper.orderByDesc(LabDefinitionEntity::getUpdatedAt).orderByDesc(LabDefinitionEntity::getId);
        IPage<LabDefinitionEntity> result = definitionMapper.selectPage(new Page<>(page, size), wrapper);
        List<LabSummaryView> items = result.getRecords().stream().map(this::toSummary).toList();
        return new PagedLabView(items, result.getTotal(), page, size);
    }

    public List<VersionView> listVersions(String labCode) {
        LabDefinitionEntity def = definitionMapper.selectOne(
                new LambdaQueryWrapper<LabDefinitionEntity>()
                        .eq(LabDefinitionEntity::getLabCode, labCode));
        if (def == null) {
            throw new BusinessException(ErrorCode.LAB_NOT_FOUND);
        }
        List<LabVersionEntity> versions = versionMapper.selectList(
                new LambdaQueryWrapper<LabVersionEntity>()
                        .eq(LabVersionEntity::getLabId, def.getId())
                        .orderByDesc(LabVersionEntity::getVersionNo));
        return versions.stream().map(v -> new VersionView(
                v.getVersionNo(), v.getStatus(), v.getSnapshotHash(),
                v.getPublishedAt() == null ? null : v.getPublishedAt().toString(),
                v.getPublishedBy(), v.getChangeNote())).toList();
    }

    public LabDetailView getVersionDetail(String labCode, int versionNo) {
        LabDefinitionEntity def = definitionMapper.selectOne(
                new LambdaQueryWrapper<LabDefinitionEntity>()
                        .eq(LabDefinitionEntity::getLabCode, labCode));
        if (def == null) {
            throw new BusinessException(ErrorCode.LAB_NOT_FOUND);
        }
        LabVersionEntity version = versionMapper.selectOne(
                new LambdaQueryWrapper<LabVersionEntity>()
                        .eq(LabVersionEntity::getLabId, def.getId())
                        .eq(LabVersionEntity::getVersionNo, versionNo));
        if (version == null) {
            throw new BusinessException(ErrorCode.VERSION_NOT_FOUND);
        }
        List<StepView> steps = stepMapper.selectList(
                new LambdaQueryWrapper<LabStepEntity>()
                        .eq(LabStepEntity::getVersionId, version.getId())
                        .orderByAsc(LabStepEntity::getStepNo))
                .stream().map(this::toStepView).toList();
        List<PreconditionView> preconditions = preconditionMapper.selectList(
                new LambdaQueryWrapper<LabPreconditionEntity>()
                        .eq(LabPreconditionEntity::getVersionId, version.getId())
                        .orderByAsc(LabPreconditionEntity::getSortNo))
                .stream().map(this::toPreconditionView).toList();
        LabDefinition domain = converter.toDomain(def);
        return new LabDetailView(
                def.getId(), def.getLabCode(), version.getName(), version.getTechStack(),
                version.getDifficulty(), version.getObjective(), version.getEstimatedMinutes(),
                domain.getTags(), versionNo, steps, preconditions);
    }

    public VersionDiffView diffVersions(String labCode, int fromVersion, int toVersion) {
        LabDefinitionEntity def = definitionMapper.selectOne(
                new LambdaQueryWrapper<LabDefinitionEntity>()
                        .eq(LabDefinitionEntity::getLabCode, labCode));
        if (def == null) {
            throw new BusinessException(ErrorCode.LAB_NOT_FOUND);
        }
        LabVersionEntity v1 = versionMapper.selectOne(
                new LambdaQueryWrapper<LabVersionEntity>()
                        .eq(LabVersionEntity::getLabId, def.getId())
                        .eq(LabVersionEntity::getVersionNo, fromVersion));
        LabVersionEntity v2 = versionMapper.selectOne(
                new LambdaQueryWrapper<LabVersionEntity>()
                        .eq(LabVersionEntity::getLabId, def.getId())
                        .eq(LabVersionEntity::getVersionNo, toVersion));
        if (v1 == null || v2 == null) {
            throw new BusinessException(ErrorCode.VERSION_NOT_FOUND);
        }
        Map<Integer, LabStepEntity> m1 = new HashMap<>();
        for (LabStepEntity s : stepMapper.selectList(
                new LambdaQueryWrapper<LabStepEntity>()
                        .eq(LabStepEntity::getVersionId, v1.getId()))) {
            m1.put(s.getStepNo(), s);
        }
        Map<Integer, LabStepEntity> m2 = new HashMap<>();
        for (LabStepEntity s : stepMapper.selectList(
                new LambdaQueryWrapper<LabStepEntity>()
                        .eq(LabStepEntity::getVersionId, v2.getId()))) {
            m2.put(s.getStepNo(), s);
        }
        Set<Integer> allNo = new TreeSet<>();
        allNo.addAll(m1.keySet());
        allNo.addAll(m2.keySet());
        List<StepDiffView> stepDiffs = new ArrayList<>();
        for (Integer no : allNo) {
            LabStepEntity a = m1.get(no);
            LabStepEntity b = m2.get(no);
            if (a == null) {
                stepDiffs.add(new StepDiffView(no, "ADDED", null, b.getTitle()));
            } else if (b == null) {
                stepDiffs.add(new StepDiffView(no, "REMOVED", a.getTitle(), null));
            } else if (!Objects.equals(a.getTitle(), b.getTitle())
                    || !Objects.equals(a.getInstruction(), b.getInstruction())
                    || !Objects.equals(a.getCompletionCriteria(), b.getCompletionCriteria())
                    || !Objects.equals(a.getHint(), b.getHint())) {
                stepDiffs.add(new StepDiffView(no, "MODIFIED", a.getTitle(), b.getTitle()));
            }
        }
        List<String> preDiffs = new ArrayList<>();
        List<LabPreconditionEntity> p1 = preconditionMapper.selectList(
                new LambdaQueryWrapper<LabPreconditionEntity>()
                        .eq(LabPreconditionEntity::getVersionId, v1.getId())
                        .orderByAsc(LabPreconditionEntity::getSortNo));
        List<LabPreconditionEntity> p2 = preconditionMapper.selectList(
                new LambdaQueryWrapper<LabPreconditionEntity>()
                        .eq(LabPreconditionEntity::getVersionId, v2.getId())
                        .orderByAsc(LabPreconditionEntity::getSortNo));
        String joined1 = p1.stream().map(LabPreconditionEntity::getDescription)
                .reduce("", (x, y) -> x + "|" + y);
        String joined2 = p2.stream().map(LabPreconditionEntity::getDescription)
                .reduce("", (x, y) -> x + "|" + y);
        if (!joined1.equals(joined2)) {
            preDiffs.add("前置条件集合发生变化");
        }
        return new VersionDiffView(fromVersion, toVersion, stepDiffs, preDiffs);
    }

    private LabSummaryView toSummary(LabDefinitionEntity entity) {
        LabDefinition domain = converter.toDomain(entity);
        return new LabSummaryView(
                entity.getLabCode(),
                entity.getName(),
                entity.getTechStack(),
                entity.getDifficulty(),
                entity.getObjective(),
                entity.getStatus(),
                entity.getEffectiveVersionNo(),
                domain.getTags());
    }

    private StepView toStepView(LabStepEntity e) {
        LabStep step = converter.toDomain(e);
        return new StepView(step.getStepNo(), step.getTitle(), step.getInstruction(),
                step.getCompletionCriteria(), step.getHint());
    }

    private PreconditionView toPreconditionView(LabPreconditionEntity e) {
        Precondition p = converter.toDomain(e);
        return new PreconditionView(p.getSortNo(), p.getType().name(), p.getLevel().name(), p.getDescription());
    }
}