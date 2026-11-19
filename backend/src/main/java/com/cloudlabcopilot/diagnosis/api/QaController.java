package com.cloudlabcopilot.diagnosis.api;

import com.cloudlabcopilot.common.response.ApiResponse;
import com.cloudlabcopilot.common.security.CurrentOperator;
import com.cloudlabcopilot.common.security.OperatorPrincipal;
import com.cloudlabcopilot.diagnosis.application.QaThreadService;
import com.cloudlabcopilot.diagnosis.infrastructure.entity.DiagnosisThreadEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/diagnosis")
public class QaController {

    private final QaThreadService service;

    public QaController(QaThreadService service) {
        this.service = service;
    }

    @PostMapping("/threads")
    public ApiResponse<Map<String, Object>> createThread(
            @RequestBody Map<String, Object> req, @CurrentOperator OperatorPrincipal op) {
        Long labId = req.get("labId") == null ? null : Long.valueOf(req.get("labId").toString());
        String title = req.get("title") == null ? null : req.get("title").toString();
        DiagnosisThreadEntity t = service.createThread(labId, op.operatorId(), title);
        return ApiResponse.success(view(t));
    }

    @GetMapping("/threads/{threadId}")
    public ApiResponse<Map<String, Object>> getThread(
            @PathVariable String threadId, @CurrentOperator OperatorPrincipal op) {
        return ApiResponse.success(view(service.getThread(threadId, op.operatorId())));
    }

    @PostMapping("/threads/{threadId}/close")
    public ApiResponse<Map<String, Object>> closeThread(
            @PathVariable String threadId, @CurrentOperator OperatorPrincipal op) {
        return ApiResponse.success(view(service.closeThread(threadId, op.operatorId())));
    }

    @GetMapping("/history")
    public ApiResponse<Map<String, Object>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @CurrentOperator OperatorPrincipal op) {
        Map<String, Object> result = new HashMap<>();
        result.put("items", service.history(op.operatorId(), page, pageSize));
        result.put("page", page);
        result.put("pageSize", pageSize);
        return ApiResponse.success(result);
    }

    private Map<String, Object> view(DiagnosisThreadEntity t) {
        Map<String, Object> m = new HashMap<>();
        m.put("threadId", t.getThreadId());
        m.put("status", t.getStatus());
        m.put("turnCount", t.getTurnCount());
        m.put("labId", t.getLabId());
        m.put("title", t.getTitle());
        m.put("expiresAt", t.getExpiresAt() == null ? null : t.getExpiresAt().toString());
        m.put("lastActiveAt", t.getLastActiveAt() == null ? null : t.getLastActiveAt().toString());
        return m;
    }
}