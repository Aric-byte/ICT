package com.cloudlabcopilot.session.api;

import com.cloudlabcopilot.common.response.ApiResponse;
import com.cloudlabcopilot.common.security.CurrentOperator;
import com.cloudlabcopilot.common.security.OperatorPrincipal;
import com.cloudlabcopilot.session.api.dto.SessionDtos.AbandonRequest;
import com.cloudlabcopilot.session.api.dto.SessionDtos.AdvanceRequest;
import com.cloudlabcopilot.session.api.dto.SessionDtos.ContextView;
import com.cloudlabcopilot.session.api.dto.SessionDtos.PauseRequest;
import com.cloudlabcopilot.session.api.dto.SessionDtos.SessionView;
import com.cloudlabcopilot.session.api.dto.SessionDtos.StartSessionRequest;
import com.cloudlabcopilot.session.api.dto.SessionDtos.TextSubmitRequest;
import com.cloudlabcopilot.session.api.dto.SessionDtos.TimelineItemView;
import com.cloudlabcopilot.session.application.SessionAppService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionAppService service;

    public SessionController(SessionAppService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<SessionView> start(
            @Valid @RequestBody StartSessionRequest req, @CurrentOperator OperatorPrincipal op) {
        return ApiResponse.success(service.start(req, op.operatorId()));
    }

    @PostMapping("/{sessionId}/advance")
    public ApiResponse<SessionView> advance(
            @PathVariable String sessionId,
            @Valid @RequestBody AdvanceRequest req,
            @CurrentOperator OperatorPrincipal op) {
        return ApiResponse.success(service.advance(sessionId, req.expectStepNo(), op.operatorId()));
    }

    @PostMapping("/{sessionId}/rollback")
    public ApiResponse<SessionView> rollback(
            @PathVariable String sessionId,
            @Valid @RequestBody AdvanceRequest req,
            @CurrentOperator OperatorPrincipal op) {
        return ApiResponse.success(service.rollback(sessionId, req.expectStepNo(), op.operatorId()));
    }

    @PostMapping("/{sessionId}/pause")
    public ApiResponse<SessionView> pause(
            @PathVariable String sessionId,
            @RequestBody(required = false) PauseRequest req,
            @CurrentOperator OperatorPrincipal op) {
        String reason = req == null ? null : req.pauseReason();
        return ApiResponse.success(service.pause(sessionId, reason, op.operatorId()));
    }

    @PostMapping("/{sessionId}/resume")
    public ApiResponse<SessionView> resume(
            @PathVariable String sessionId, @CurrentOperator OperatorPrincipal op) {
        return ApiResponse.success(service.resume(sessionId, op.operatorId()));
    }

    @PostMapping("/{sessionId}/abandon")
    public ApiResponse<SessionView> abandon(
            @PathVariable String sessionId,
            @Valid @RequestBody AbandonRequest req,
            @CurrentOperator OperatorPrincipal op) {
        return ApiResponse.success(service.abandon(sessionId, req, op.operatorId()));
    }

    @PostMapping("/{sessionId}/text-submissions")
    public ApiResponse<Void> submitText(
            @PathVariable String sessionId,
            @Valid @RequestBody TextSubmitRequest req,
            @CurrentOperator OperatorPrincipal op) {
        service.submitText(sessionId, req, op.operatorId());
        return ApiResponse.success();
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<SessionView> status(
            @PathVariable String sessionId, @CurrentOperator OperatorPrincipal op) {
        return ApiResponse.success(service.getStatus(sessionId, op.operatorId()));
    }

    @GetMapping("/{sessionId}/timeline")
    public ApiResponse<List<TimelineItemView>> timeline(
            @PathVariable String sessionId, @CurrentOperator OperatorPrincipal op) {
        return ApiResponse.success(service.getTimeline(sessionId, op.operatorId()));
    }

    @GetMapping("/{sessionId}/context")
    public ApiResponse<ContextView> context(
            @PathVariable String sessionId, @CurrentOperator OperatorPrincipal op) {
        return ApiResponse.success(service.getContext(sessionId, op.operatorId()));
    }
}