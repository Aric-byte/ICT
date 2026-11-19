package com.cloudlabcopilot.lab.domain.validation;

import com.cloudlabcopilot.common.exception.BusinessException;
import com.cloudlabcopilot.common.response.ErrorCode;
import com.cloudlabcopilot.lab.domain.model.Precondition;
import com.cloudlabcopilot.lab.domain.model.PreconditionLevel;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class PreconditionValidator {

    private static final int MAX_PRECONDITIONS = 50;

    public void validate(List<Precondition> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        if (list.size() > MAX_PRECONDITIONS) {
            throw new BusinessException(ErrorCode.LAB_PRECONDITION_INVALID, "前置条件最多 50 条");
        }
        Set<Integer> sortNos = new HashSet<>();
        for (Precondition p : list) {
            if (p.getType() == null || p.getLevel() == null) {
                throw new BusinessException(ErrorCode.LAB_PRECONDITION_INVALID);
            }
            if (p.getDescription() == null || p.getDescription().isBlank() || p.getDescription().length() > 500) {
                throw new BusinessException(ErrorCode.LAB_PRECONDITION_INVALID, "前置条件描述长度需在 1~500");
            }
            if (p.getSortNo() < 1 || !sortNos.add(p.getSortNo())) {
                throw new BusinessException(ErrorCode.LAB_PRECONDITION_INVALID, "前置条件顺序号需唯一且>=1");
            }
        }
    }

    public long countRequired(List<Precondition> list) {
        if (list == null) {
            return 0;
        }
        return list.stream().filter(p -> p.getLevel() == PreconditionLevel.REQUIRED).count();
    }
}