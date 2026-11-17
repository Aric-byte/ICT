package com.cloudlabcopilot.common.security;

public record OperatorPrincipal(String operatorId, Role role) {

    public enum Role {
        AUTHOR,
        TEACHER,
        STUDENT,
        INTERNAL
    }
}