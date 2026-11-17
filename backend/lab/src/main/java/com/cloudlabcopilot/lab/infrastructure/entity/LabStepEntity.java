package com.cloudlabcopilot.lab.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("lab_step")
public class LabStepEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long versionId;
    private Integer stepNo;
    private String title;
    private String instruction;
    private String completionCriteria;
    private String hint;
}