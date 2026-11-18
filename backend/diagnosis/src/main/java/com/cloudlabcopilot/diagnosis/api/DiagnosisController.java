package com.cloudlabcopilot.diagnosis.api;

import com.cloudlabcopilot.common.response.ApiResponse;
import com.cloudlabcopilot.diagnosis.api.dto.DiagnosisDtos.AnswerView;
import com.cloudlabcopilot.diagnosis.api.dto.DiagnosisDtos.AskRequest;
import com.cloudlabcopilot.diagnosis.application.DiagnosisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/diagnosis")
public class DiagnosisController {

    private final DiagnosisService service;

    public DiagnosisController(DiagnosisService service) {
        this.service = service;
    }

    @PostMapping("/questions")
    public ApiResponse<AnswerView> ask(@RequestBody AskRequest req) {
        return ApiResponse.success(service.ask(req));
    }

    @GetMapping("/capabilities")
    public ApiResponse<Map<String, Object>> capabilities() {
        return ApiResponse.success(Map.of(
                "provider", "NONE",
                "streamVersion", "1.0",
                "vectorEnabled", false,
                "note", "AI Provider 未配置，当前走知识兜底"));
    }
}