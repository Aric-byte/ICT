package com.cloudlabcopilot.lab.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Precondition {

    private int sortNo;
    private PreconditionType type;
    private PreconditionLevel level;
    private String description;
    private String checkMode;
}