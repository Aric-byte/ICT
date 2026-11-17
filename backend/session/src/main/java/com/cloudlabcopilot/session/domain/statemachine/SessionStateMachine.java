package com.cloudlabcopilot.session.domain.statemachine;

import com.cloudlabcopilot.common.exception.BusinessException;
import com.cloudlabcopilot.common.response.ErrorCode;
import com.cloudlabcopilot.session.domain.model.EventType;
import com.cloudlabcopilot.session.domain.model.SessionStatus;
import org.springframework.stereotype.Component;

@Component
public class SessionStateMachine {

    public SessionStatus resolve(SessionStatus current, EventType event) {
        if (current == SessionStatus.COMPLETED || current == SessionStatus.ABANDONED) {
            throw new BusinessException(ErrorCode.TERMINAL_SESSION_IMMUTABLE);
        }
        return switch (event) {
            case STEP_COMPLETED, STEP_ROLLED_BACK -> {
                if (current == SessionStatus.PAUSED) {
                    throw new BusinessException(ErrorCode.SESSION_PAUSED_CANNOT_ADVANCE);
                }
                yield current;
            }
            case PAUSED -> {
                if (current != SessionStatus.IN_PROGRESS) {
                    throw new BusinessException(ErrorCode.INVALID_PARAMETER, "仅进行中会话可暂停");
                }
                yield SessionStatus.PAUSED;
            }
            case RESUMED -> {
                if (current != SessionStatus.PAUSED) {
                    throw new BusinessException(ErrorCode.INVALID_PARAMETER, "仅暂停会话可恢复");
                }
                yield SessionStatus.IN_PROGRESS;
            }
            case SESSION_COMPLETED -> SessionStatus.COMPLETED;
            case SESSION_ABANDONED -> SessionStatus.ABANDONED;
            default -> current;
        };
    }
}