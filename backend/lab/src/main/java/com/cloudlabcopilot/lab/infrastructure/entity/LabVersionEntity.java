package com.cloudlabcopilot.lab.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("lab_version")
public class LabVersionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long labId;
    private Integer versionNo;
    private String status;
    private String name;
    private String techStack;
    private String customTechStack;
    private String difficulty;
    private String objective;
    private Integer estimatedMinutes;
    private String tags;
    private String changeNote;
    private String snapshotHash;
    private OffsetDateTime publishedAt;
    private String publishedBy;
    private OffsetDateTime createdAt;
}