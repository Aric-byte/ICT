package com.cloudlabcopilot.lab.api;

import com.cloudlabcopilot.common.response.ApiResponse;
import com.cloudlabcopilot.common.security.CurrentOperator;
import com.cloudlabcopilot.common.security.OperatorPrincipal;
import com.cloudlabcopilot.lab.api.dto.LabDtos.CreateLabRequest;
import com.cloudlabcopilot.lab.api.dto.LabDtos.LabDetailView;
import com.cloudlabcopilot.lab.api.dto.LabDtos.LabSummaryView;
import com.cloudlabcopilot.lab.api.dto.LabDtos.PagedLabView;
import com.cloudlabcopilot.lab.api.dto.LabDtos.PreconditionDto;
import com.cloudlabcopilot.lab.api.dto.LabDtos.PreconditionView;
import com.cloudlabcopilot.lab.api.dto.LabDtos.SavePreconditionsRequest;
import com.cloudlabcopilot.lab.api.dto.LabDtos.SaveStepsRequest;
import com.cloudlabcopilot.lab.api.dto.LabDtos.StartEligibilityView;
import com.cloudlabcopilot.lab.api.dto.LabDtos.StepDto;
import com.cloudlabcopilot.lab.api.dto.LabDtos.StepView;
import com.cloudlabcopilot.lab.api.dto.LabDtos.SwitchVersionRequest;
import com.cloudlabcopilot.lab.api.dto.LabDtos.UpdateLabRequest;
import com.cloudlabcopilot.lab.application.LabCommandService;
import com.cloudlabcopilot.lab.application.LabQueryService;
import com.cloudlabcopilot.lab.domain.model.Difficulty;
import com.cloudlabcopilot.lab.domain.model.LabDefinition;
import com.cloudlabcopilot.lab.domain.model.LabStep;
import com.cloudlabcopilot.lab.domain.model.Precondition;
import com.cloudlabcopilot.lab.domain.model.PreconditionLevel;
import com.cloudlabcopilot.lab.domain.model.PreconditionType;
import com.cloudlabcopilot.lab.domain.model.TechStack;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/labs")
public class LabController {

    private final LabCommandService commandService;
    private final LabQueryService queryService;

    public LabController(LabCommandService commandService, LabQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    public ApiResponse<LabSummaryView> create(
            @Valid @RequestBody CreateLabRequest req, @CurrentOperator OperatorPrincipal op) {
        LabDefinition input = LabDefinition.builder()
                .labCode(req.labCode())
                .name(req.name())
                .techStack(TechStack.valueOf(req.techStack()))
                .customTechStack(req.customTechStack())
                .difficulty(Difficulty.valueOf(req.difficulty()))
                .objective(req.objective())
                .estimatedMinutes(req.estimatedMinutes())
                .tags(req.tags())
                .internalNote(req.internalNote())
                .build();
        LabDefinition created = commandService.createLab(input, op.operatorId());
        return ApiResponse.success(toSummary(created));
    }

    @PutMapping("/{labCode}")
    public ApiResponse<LabSummaryView> update(
            @PathVariable String labCode,
            @Valid @RequestBody UpdateLabRequest req,
            @CurrentOperator OperatorPrincipal op) {
        LabDefinition input = LabDefinition.builder()
                .name(req.name())
                .techStack(TechStack.valueOf(req.techStack()))
                .customTechStack(req.customTechStack())
                .difficulty(Difficulty.valueOf(req.difficulty()))
                .objective(req.objective())
                .estimatedMinutes(req.estimatedMinutes())
                .tags(req.tags())
                .internalNote(req.internalNote())
                .rowVersion(req.rowVersion())
                .build();
        LabDefinition updated = commandService.updateLab(labCode, input, op.operatorId());
        return ApiResponse.success(toSummary(updated));
    }

    @DeleteMapping("/{labCode}")
    public ApiResponse<Void> delete(
            @PathVariable String labCode, @CurrentOperator OperatorPrincipal op) {
        commandService.deleteLab(labCode, op.operatorId());
        return ApiResponse.success();
    }

    @PutMapping("/{labCode}/steps")
    public ApiResponse<Void> saveSteps(
            @PathVariable String labCode,
            @Valid @RequestBody SaveStepsRequest req,
            @CurrentOperator OperatorPrincipal op) {
        List<LabStep> steps = req.steps().stream()
                .map(s -> LabStep.builder()
                        .stepNo(s.stepNo())
                        .title(s.title())
                        .instruction(s.instruction())
                        .completionCriteria(s.completionCriteria())
                        .hint(s.hint())
                        .build())
                .toList();
        commandService.saveSteps(labCode, steps, op.operatorId());
        return ApiResponse.success();
    }

    @GetMapping("/{labCode}/steps")
    public ApiResponse<List<StepView>> getSteps(@PathVariable String labCode) {
        LabDetailView detail = queryService.getStudentDetail(labCode);
        return ApiResponse.success(detail.steps());
    }

    @PutMapping("/{labCode}/preconditions")
    public ApiResponse<Void> savePreconditions(
            @PathVariable String labCode,
            @Valid @RequestBody SavePreconditionsRequest req,
            @CurrentOperator OperatorPrincipal op) {
        List<Precondition> list = req.preconditions().stream()
                .map(p -> Precondition.builder()
                        .sortNo(p.sortNo())
                        .type(PreconditionType.valueOf(p.type()))
                        .level(PreconditionLevel.valueOf(p.level()))
                        .description(p.description())
                        .checkMode("SELF_CONFIRM")
                        .build())
                .toList();
        commandService.savePreconditions(labCode, list, op.operatorId());
        return ApiResponse.success();
    }

    @GetMapping("/{labCode}/preconditions")
    public ApiResponse<List<PreconditionView>> getPreconditions(@PathVariable String labCode) {
        LabDetailView detail = queryService.getStudentDetail(labCode);
        return ApiResponse.success(detail.preconditions());
    }

    @PostMapping("/{labCode}/publish")
    public ApiResponse<Void> publish(
            @PathVariable String labCode, @CurrentOperator OperatorPrincipal op) {
        commandService.publish(labCode, op.operatorId());
        return ApiResponse.success();
    }

    @PostMapping("/{labCode}/unpublish")
    public ApiResponse<Void> unpublish(
            @PathVariable String labCode, @CurrentOperator OperatorPrincipal op) {
        commandService.unpublish(labCode, op.operatorId());
        return ApiResponse.success();
    }

    @PostMapping("/{labCode}/effective-version")
    public ApiResponse<Void> switchVersion(
            @PathVariable String labCode,
            @Valid @RequestBody SwitchVersionRequest req,
            @CurrentOperator OperatorPrincipal op) {
        commandService.switchEffectiveVersion(labCode, req.versionNo(), op.operatorId());
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<PagedLabView> catalog(
            @RequestParam(required = false) String techStack,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(queryService.listCatalog(techStack, difficulty, keyword, page, size));
    }

    @GetMapping("/{labCode}")
    public ApiResponse<LabDetailView> detail(@PathVariable String labCode) {
        return ApiResponse.success(queryService.getStudentDetail(labCode));
    }

    @GetMapping("/{labCode}/start-eligibility")
    public ApiResponse<StartEligibilityView> eligibility(@PathVariable String labCode) {
        return ApiResponse.success(queryService.checkEligibility(labCode));
    }

    private LabSummaryView toSummary(LabDefinition def) {
        return new LabSummaryView(
                def.getLabCode(),
                def.getName(),
                def.getTechStack() == null ? null : def.getTechStack().name(),
                def.getDifficulty() == null ? null : def.getDifficulty().name(),
                def.getObjective(),
                def.getStatus() == null ? null : def.getStatus().name(),
                def.getEffectiveVersionNo(),
                def.getTags());
    }
}