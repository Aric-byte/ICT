package com.cloudlabcopilot.lab.infrastructure.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudlabcopilot.common.exception.BusinessException;
import com.cloudlabcopilot.common.response.ErrorCode;
import com.cloudlabcopilot.lab.domain.model.LabStep;
import com.cloudlabcopilot.lab.domain.model.Precondition;
import com.cloudlabcopilot.lab.domain.port.LabSnapshot;
import com.cloudlabcopilot.lab.domain.port.LabSnapshotQueryPort;
import com.cloudlabcopilot.lab.infrastructure.converter.LabConverter;
import com.cloudlabcopilot.lab.infrastructure.entity.LabPreconditionEntity;
import com.cloudlabcopilot.lab.infrastructure.entity.LabStepEntity;
import com.cloudlabcopilot.lab.infrastructure.entity.LabVersionEntity;
import com.cloudlabcopilot.lab.infrastructure.mapper.LabPreconditionMapper;
import com.cloudlabcopilot.lab.infrastructure.mapper.LabStepMapper;
import com.cloudlabcopilot.lab.infrastructure.mapper.LabVersionMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LabSnapshotQueryAdapter implements LabSnapshotQueryPort {

    private final LabVersionMapper versionMapper;
    private final LabStepMapper stepMapper;
    private final LabPreconditionMapper preconditionMapper;
    private final LabConverter converter;

    public LabSnapshotQueryAdapter(
            LabVersionMapper versionMapper,
            LabStepMapper stepMapper,
            LabPreconditionMapper preconditionMapper,
            LabConverter converter) {
        this.versionMapper = versionMapper;
        this.stepMapper = stepMapper;
        this.preconditionMapper = preconditionMapper;
        this.converter = converter;
    }

    @Override
    public LabSnapshot loadPublishedSnapshot(Long labId, int versionNo) {
        LabVersionEntity version = versionMapper.selectOne(
                new LambdaQueryWrapper<LabVersionEntity>()
                        .eq(LabVersionEntity::getLabId, labId)
                        .eq(LabVersionEntity::getVersionNo, versionNo));
        if (version == null) {
            throw new BusinessException(ErrorCode.VERSION_SNAPSHOT_UNAVAILABLE);
        }
        List<LabStepEntity> stepEntities = stepMapper.selectList(
                new LambdaQueryWrapper<LabStepEntity>()
                        .eq(LabStepEntity::getVersionId, version.getId())
                        .orderByAsc(LabStepEntity::getStepNo));
        List<LabPreconditionEntity> preconditionEntities = preconditionMapper.selectList(
                new LambdaQueryWrapper<LabPreconditionEntity>()
                        .eq(LabPreconditionEntity::getVersionId, version.getId())
                        .orderByAsc(LabPreconditionEntity::getSortNo));
        List<LabStep> steps = stepEntities.stream().map(converter::toDomain).toList();
        List<Precondition> preconditions = preconditionEntities.stream().map(converter::toDomain).toList();
        return new LabSnapshot(
                version.getLabId(),
                version.getId(),
                version.getVersionNo(),
                version.getObjective(),
                steps,
                preconditions,
                steps.size());
    }
}