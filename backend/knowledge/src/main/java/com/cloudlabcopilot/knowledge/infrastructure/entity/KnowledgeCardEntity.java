package com.cloudlabcopilot.knowledge.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("knowledge_card")
public class KnowledgeCardEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String techStack;
    private String title;
    private String symptom;
    private String safeNextStep;
    private String verificationMethod;
    private String caseOrigin;
    private String sourceType;
    private String sourceRef;
    private String applicableVersions;
    private String status;
    private String reviewStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}