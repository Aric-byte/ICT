package com.cloudlabcopilot.diagnosis.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiagnosisPageQuery {

    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(50)
    private int pageSize = 20;

    public int offset() {
        return (page - 1) * pageSize;
    }
}