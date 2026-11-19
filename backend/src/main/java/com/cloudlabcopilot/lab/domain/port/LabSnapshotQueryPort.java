package com.cloudlabcopilot.lab.domain.port;

public interface LabSnapshotQueryPort {

    LabSnapshot loadPublishedSnapshot(Long labId, int versionNo);
}