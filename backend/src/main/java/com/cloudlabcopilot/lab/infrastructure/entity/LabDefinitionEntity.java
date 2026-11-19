package com.cloudlabcopilot.lab.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("lab_definition")
public class LabDefinitionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String labCode;
    private String name;
    private String techStack;
    private String customTechStack;
    private String difficulty;
    private String objective;
    private Integer estimatedMinutes;
    private String tags;
    private String status;
    private Integer effectiveVersionNo;
    private String ownerId;
    private String internalNote;
    private String draftSteps;
    private String draftPreconditions;
    @Version
    private Integer rowVersion;
    @TableLogic
    private Integer deleted;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}