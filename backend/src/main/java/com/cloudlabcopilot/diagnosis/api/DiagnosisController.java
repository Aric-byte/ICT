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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/diagnosis")
public class DiagnosisController {

    private static final String STREAM_VERSION = "1.0";
    private static final int CHUNK_MAX = 200;

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
        int[] seq = { 0 };
        try {
            AnswerView answer = service.ask(req);
            send(emitter, seq, "stream_version", Map.of("streamVersion", STREAM_VERSION));
            send(emitter, seq, "context", Map.of(
                    "techStack", nvl(answer.techStack()),
                    "contextCompleteness", nvl(answer.contextCompleteness())));
            send(emitter, seq, "restate", Map.of("restate", nvl(answer.restate())));
            String explanation = nvl(answer.explanation());
            for (int i = 0; i < explanation.length(); i += CHUNK_MAX) {
                String delta = explanation.substring(i, Math.min(i + CHUNK_MAX, explanation.length()));
                send(emitter, seq, "chunk", Map.of("section", "EXPLANATION", "delta", delta));
            }
            send(emitter, seq, "causes", Map.of("causes", answer.causes()));
            send(emitter, seq, "citations", Map.of("citations", answer.citations()));
            send(emitter, seq, "next_step", Map.of("nextStep", answer.nextStep(), "safetyChecked", true));
            if (!"NORMAL".equals(answer.generationStatus())) {
                send(emitter, seq, "degraded", Map.of("generationStatus", answer.generationStatus()));
            }
            send(emitter, seq, "final", answer);
            emitter.complete();
        } catch (Exception e) {
            try {
                send(emitter, seq, "error",
                        Map.of("message", e.getMessage() == null ? "stream error" : e.getMessage()));
            } catch (Exception ignored) {
                // ignore send failure on error path
            }
            emitter.completeWithError(e);
        }
        return emitter;
    }

    @GetMapping("/capabilities")
    public ApiResponse<Map<String, Object>> capabilities() {
        return ApiResponse.success(Map.of(
                "provider", "NONE",
                "streamVersion", STREAM_VERSION,
                "vectorEnabled", false,
                "events", List.of(
                        "stream_version", "context", "restate", "chunk",
                        "causes", "citations", "next_step", "degraded", "final", "error"),
                "note", "AI Provider 未配置，当前走知识兜底"));
    }

    private void send(SseEmitter emitter, int[] seq, String name, Object data) throws IOException {
        seq[0]++;
        Map<String, Object> payload = new HashMap<>();
        payload.put("seq", seq[0]);
        payload.put("streamVersion", STREAM_VERSION);
        payload.put("event", name);
        payload.put("data", data);
        emitter.send(SseEmitter.event().name(name).data(payload));
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}