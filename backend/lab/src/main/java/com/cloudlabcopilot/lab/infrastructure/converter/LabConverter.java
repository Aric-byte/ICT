package com.cloudlabcopilot.lab.infrastructure.converter;

import com.cloudlabcopilot.lab.domain.model.Difficulty;
import com.cloudlabcopilot.lab.domain.model.LabDefinition;
import com.cloudlabcopilot.lab.domain.model.LabStatus;
import com.cloudlabcopilot.lab.domain.model.LabStep;
import com.cloudlabcopilot.lab.domain.model.Precondition;
import com.cloudlabcopilot.lab.domain.model.PreconditionLevel;
import com.cloudlabcopilot.lab.domain.model.PreconditionType;
import com.cloudlabcopilot.lab.domain.model.TechStack;
import com.cloudlabcopilot.lab.domain.model.VersionStatus;
import com.cloudlabcopilot.lab.infrastructure.entity.LabDefinitionEntity;
import com.cloudlabcopilot.lab.infrastructure.entity.LabPreconditionEntity;
import com.cloudlabcopilot.lab.infrastructure.entity.LabStepEntity;
import com.cloudlabcopilot.lab.infrastructure.entity.LabVersionEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class LabConverter {

    private static final TypeReference<List<String>> TAG_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<LabStep>> STEP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Precondition>> PRECONDITION_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public LabConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LabDefinition toDomain(LabDefinitionEntity e) {
        return LabDefinition.builder()
                .id(e.getId())
                .labCode(e.getLabCode())
                .name(e.getName())
                .techStack(e.getTechStack() == null ? null : TechStack.valueOf(e.getTechStack()))
                .customTechStack(e.getCustomTechStack())
                .difficulty(e.getDifficulty() == null ? null : Difficulty.valueOf(e.getDifficulty()))
                .objective(e.getObjective())
                .estimatedMinutes(e.getEstimatedMinutes())
                .tags(readTags(e.getTags()))
                .status(e.getStatus() == null ? null : LabStatus.valueOf(e.getStatus()))
                .effectiveVersionNo(e.getEffectiveVersionNo())
                .ownerId(e.getOwnerId())
                .internalNote(e.getInternalNote())
                .draftSteps(readSteps(e.getDraftSteps()))
                .draftPreconditions(readPreconditions(e.getDraftPreconditions()))
                .rowVersion(e.getRowVersion())
                .build();
    }

    public LabDefinitionEntity toEntity(LabDefinition d) {
        LabDefinitionEntity e = new LabDefinitionEntity();
        e.setId(d.getId());
        e.setLabCode(d.getLabCode());
        e.setName(d.getName());
        e.setTechStack(d.getTechStack() == null ? null : d.getTechStack().name());
        e.setCustomTechStack(d.getCustomTechStack());
        e.setDifficulty(d.getDifficulty() == null ? null : d.getDifficulty().name());
        e.setObjective(d.getObjective());
        e.setEstimatedMinutes(d.getEstimatedMinutes());
        e.setTags(writeJson(d.getTags()));
        e.setStatus(d.getStatus() == null ? null : d.getStatus().name());
        e.setEffectiveVersionNo(d.getEffectiveVersionNo());
        e.setOwnerId(d.getOwnerId());
        e.setInternalNote(d.getInternalNote());
        e.setDraftSteps(writeJson(d.getDraftSteps()));
        e.setDraftPreconditions(writeJson(d.getDraftPreconditions()));
        e.setRowVersion(d.getRowVersion());
        return e;
    }

    public LabVersionEntity toVersionEntity(LabDefinition d, int versionNo, String snapshotHash, String publishedBy) {
        LabVersionEntity e = new LabVersionEntity();
        e.setLabId(d.getId());
        e.setVersionNo(versionNo);
        e.setStatus(VersionStatus.PUBLISHED.name());
        e.setName(d.getName());
        e.setTechStack(d.getTechStack() == null ? null : d.getTechStack().name());
        e.setCustomTechStack(d.getCustomTechStack());
        e.setDifficulty(d.getDifficulty() == null ? null : d.getDifficulty().name());
        e.setObjective(d.getObjective());
        e.setEstimatedMinutes(d.getEstimatedMinutes());
        e.setTags(writeJson(d.getTags()));
        e.setSnapshotHash(snapshotHash);
        e.setPublishedBy(publishedBy);
        return e;
    }

    public LabStep toDomain(LabStepEntity e) {
        return LabStep.builder()
                .stepNo(e.getStepNo())
                .title(e.getTitle())
                .instruction(e.getInstruction())
                .completionCriteria(e.getCompletionCriteria())
                .hint(e.getHint())
                .build();
    }

    public LabStepEntity toEntity(LabStep s, Long versionId) {
        LabStepEntity e = new LabStepEntity();
        e.setVersionId(versionId);
        e.setStepNo(s.getStepNo());
        e.setTitle(s.getTitle());
        e.setInstruction(s.getInstruction());
        e.setCompletionCriteria(s.getCompletionCriteria());
        e.setHint(s.getHint());
        return e;
    }

    public Precondition toDomain(LabPreconditionEntity e) {
        return Precondition.builder()
                .sortNo(e.getSortNo())
                .type(PreconditionType.valueOf(e.getType()))
                .level(PreconditionLevel.valueOf(e.getLevel()))
                .description(e.getDescription())
                .checkMode(e.getCheckMode())
                .build();
    }

    public LabPreconditionEntity toEntity(Precondition p, Long versionId) {
        LabPreconditionEntity e = new LabPreconditionEntity();
        e.setVersionId(versionId);
        e.setSortNo(p.getSortNo());
        e.setType(p.getType().name());
        e.setLevel(p.getLevel().name());
        e.setDescription(p.getDescription());
        e.setCheckMode(p.getCheckMode() == null ? "SELF_CONFIRM" : p.getCheckMode());
        return e;
    }

    public String writeJson(Object o) {
        try {
            if (o == null) {
                return "[]";
            }
            return objectMapper.writeValueAsString(o);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private List<String> readTags(String json) {
        return readList(json, TAG_TYPE);
    }

    private List<LabStep> readSteps(String json) {
        return readList(json, STEP_TYPE);
    }

    private List<Precondition> readPreconditions(String json) {
        return readList(json, PRECONDITION_TYPE);
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }
}