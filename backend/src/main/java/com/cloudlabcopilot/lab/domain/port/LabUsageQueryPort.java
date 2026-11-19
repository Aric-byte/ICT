package com.cloudlabcopilot.lab.domain.port;

public interface LabUsageQueryPort {

    long countSessionsByLab(Long labId);

    long countActiveSessionsByVersion(Long labId, int versionNo);
}