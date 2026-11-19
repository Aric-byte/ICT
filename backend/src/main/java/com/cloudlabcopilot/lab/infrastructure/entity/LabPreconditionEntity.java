package com.cloudlabcopilot.lab.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("lab_precondition")
public class LabPreconditionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long versionId;
    private Integer sortNo;
    private String type;
    private String level;
    private String description;
    private String checkMode;
}