package com.cloudlabcopilot.lab.api;

import com.cloudlabcopilot.common.response.ApiResponse;
import com.cloudlabcopilot.lab.api.dto.LabDtos.PagedLabView;
import com.cloudlabcopilot.lab.application.LabQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/labs")
public class LabAdminController {

    private final LabQueryService queryService;

    public LabAdminController(LabQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public ApiResponse<PagedLabView> listForTeaching(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(queryService.listForTeaching(status, page, size));
    }
}