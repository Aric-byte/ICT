package com.cloudlabcopilot.diagnosis.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RetentionCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionCleanupJob.class);

    private final RetentionExportService retentionExportService;

    public RetentionCleanupJob(RetentionExportService retentionExportService) {
        this.retentionExportService = retentionExportService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanup() {
        try {
            int deleted = retentionExportService.purgeExpired();
            log.info("诊断问答保留期清理完成，删除 {} 条过期记录", deleted);
        } catch (Exception e) {
            log.error("诊断问答保留期清理失败", e);
        }
    }
}