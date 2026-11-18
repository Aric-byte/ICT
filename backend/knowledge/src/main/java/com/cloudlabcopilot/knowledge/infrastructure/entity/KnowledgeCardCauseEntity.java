package com.cloudlabcopilot.knowledge.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("knowledge_card_cause")
public class KnowledgeCardCauseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long cardId;
    private String causeText;
    private String confidence;
    private Integer sortNo;
}