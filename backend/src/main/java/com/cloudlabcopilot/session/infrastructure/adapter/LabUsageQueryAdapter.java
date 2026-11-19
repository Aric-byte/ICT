package com.cloudlabcopilot.session.infrastructure.adapter;

import com.cloudlabcopilot.lab.domain.port.LabUsageQueryPort;
import com.cloudlabcopilot.session.infrastructure.mapper.SessionMapper;
import org.springframework.stereotype.Component;

@Component
public class LabUsageQueryAdapter implements LabUsageQueryPort {

    private final SessionMapper sessionMapper;

    public LabUsageQueryAdapter(SessionMapper sessionMapper) {
        this.sessionMapper = sessionMapper;
    }

    @Override
    public long countSessionsByLab(Long labId) {
        return sessionMapper.countByLab(labId);
    }

    @Override
    public long countActiveSessionsByVersion(Long labId, int versionNo) {
        return sessionMapper.countActiveByVersion(labId, versionNo);
    }
}