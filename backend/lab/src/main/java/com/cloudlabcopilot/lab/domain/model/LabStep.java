package com.cloudlabcopilot.lab.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabStep {

    private int stepNo;
    private String title;
    private String instruction;
    private String completionCriteria;
    private String hint;
}