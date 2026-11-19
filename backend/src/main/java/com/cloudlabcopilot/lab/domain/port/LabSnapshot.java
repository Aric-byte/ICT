package com.cloudlabcopilot.lab.domain.port;

import com.cloudlabcopilot.lab.domain.model.LabStep;
import com.cloudlabcopilot.lab.domain.model.Precondition;

import java.util.List;

public record LabSnapshot(
        Long labId,
        Long versionId,
        int versionNo,
        String objective,
        List<LabStep> steps,
        List<Precondition> preconditions,
        int totalSteps) {
}