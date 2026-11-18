import request from './request'
import type { ApiResponse } from '../types/api'

export interface SessionView {
    sessionId: string
    labId: number
    versionNo: number
    status: string
    stateLabel: string
    currentStepNo: number
    totalSteps: number
    blockedFlag: boolean
    startedAt: string
    lastActiveAt: string
    overdueFlag: boolean
    duplicated: boolean
}

export interface TimelineItem {
    recordId: string
    eventType: string
    occurredAt: string
    summary: string
    stepNo: number | null
    result: string
}

export interface ContextView {
    sessionId: string
    labId: number
    versionNo: number
    objective: string
    currentStepNo: number
    totalSteps: number
    completedSteps: number[]
    recentRecords: TimelineItem[]
    stateLabel: string
    blockedFlag: boolean
    overdueFlag: boolean
    readOnly: boolean
}

export function startSession(labId: number, versionNo: number) {
    return request
        .post<ApiResponse<SessionView>>('/api/v1/sessions', {
            labId,
            versionNo,
            preconditionConfirmed: true,
        })
        .then((r) => r.data.data)
}

export function advanceSession(sessionId: string, expectStepNo: number) {
    return request
        .post<ApiResponse<SessionView>>(`/api/v1/sessions/${sessionId}/advance`, { expectStepNo })
        .then((r) => r.data.data)
}

export function getSessionStatus(sessionId: string) {
    return request
        .get<ApiResponse<SessionView>>(`/api/v1/sessions/${sessionId}`)
        .then((r) => r.data.data)
}

export function getSessionContext(sessionId: string) {
    return request
        .get<ApiResponse<ContextView>>(`/api/v1/sessions/${sessionId}/context`)
        .then((r) => r.data.data)
}

export function getTimeline(sessionId: string) {
    return request
        .get<ApiResponse<TimelineItem[]>>(`/api/v1/sessions/${sessionId}/timeline`)
        .then((r) => r.data.data)
}