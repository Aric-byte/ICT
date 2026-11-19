package com.cloudlabcopilot.session.domain.statemachine;

import com.cloudlabcopilot.common.exception.BusinessException;
import com.cloudlabcopilot.session.domain.model.EventType;
import com.cloudlabcopilot.session.domain.model.SessionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("会话状态机测试")
class SessionStateMachineTest {

    private final SessionStateMachine machine = new SessionStateMachine();

    @Test
    @DisplayName("进行中可暂停")
    void pauseFromInProgress() {
        assertThat(machine.resolve(SessionStatus.IN_PROGRESS, EventType.PAUSED))
                .isEqualTo(SessionStatus.PAUSED);
    }

    @Test
    @DisplayName("暂停可恢复为进行中")
    void resumeFromPaused() {
        assertThat(machine.resolve(SessionStatus.PAUSED, EventType.RESUMED))
                .isEqualTo(SessionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("终态会话不可再变更")
    void terminalSessionImmutable() {
        assertThatThrownBy(() -> machine.resolve(SessionStatus.COMPLETED, EventType.PAUSED))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> machine.resolve(SessionStatus.ABANDONED, EventType.RESUMED))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("暂停态不可推进步骤")
    void advanceBlockedWhenPaused() {
        assertThatThrownBy(() -> machine.resolve(SessionStatus.PAUSED, EventType.STEP_COMPLETED))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("放弃进入终态")
    void abandonToTerminal() {
        assertThat(machine.resolve(SessionStatus.IN_PROGRESS, EventType.SESSION_ABANDONED))
                .isEqualTo(SessionStatus.ABANDONED);
    }
}