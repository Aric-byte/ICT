package com.cloudlabcopilot.diagnosis.api;

import com.cloudlabcopilot.common.response.ApiResponse;
import com.cloudlabcopilot.diagnosis.api.dto.DiagnosisDtos.AnswerView;
import com.cloudlabcopilot.diagnosis.api.dto.DiagnosisDtos.AskRequest;
import com.cloudlabcopilot.diagnosis.application.DiagnosisService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
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

    @PostMapping(value = "/questions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody AskRequest req) {
        SseEmitter emitter = new SseEmitter(20000L);
        try {
            AnswerView answer = service.ask(req);
            emitter.send(SseEmitter.event()
                    .name("context")
                    .data(Map.of(
                            "techStack", answer.techStack() == null ? "" : answer.techStack(),
                            "contextCompleteness", answer.contextCompleteness())));
            String explanation = answer.explanation() == null ? "" : answer.explanation();
            for (int i = 0; i < explanation.length(); i += 200) {
                String chunk = explanation.substring(i, Math.min(i + 200, explanation.length()));
                emitter.send(SseEmitter.event()
                        .name("chunk")
                        .data(Map.of("section", "EXPLANATION", "delta", chunk)));
            }
            emitter.send(SseEmitter.event().name("next_step").data(answer.nextStep()));
            emitter.send(SseEmitter.event().name("final").data(answer));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    @GetMapping("/capabilities")
    public ApiResponse<Map<String, Object>> capabilities() {
        return ApiResponse.success(Map.of(
                "provider", "NONE",
                "streamVersion", "1.0",
                "vectorEnabled", false,
                "events", List.of("context", "chunk", "next_step", "final"),
                "note", "AI Provider 未配置，当前走知识兜底"));
    }
}