import request from './request'
import type { ApiResponse } from '../types/api'

export interface LabSummary {
    labCode: string
    name: string
    techStack: string
    difficulty: string
    objective: string
    status: string
    effectiveVersionNo: number | null
    tags: string[]
}

export interface StepView {
    stepNo: number
    title: string
    instruction: string
    completionCriteria: string
    hint: string
}

export interface PreconditionView {
    sortNo: number
    type: string
    level: string
    description: string
}

export interface LabDetail {
    labId: number
    labCode: string
    name: string
    techStack: string
    difficulty: string
    objective: string
    estimatedMinutes: number | null
    tags: string[]
    versionNo: number
    steps: StepView[]
    preconditions: PreconditionView[]
}

export interface PagedLab {
    items: LabSummary[]
    total: number
    page: number
    size: number
}

export interface EligibilityView {
    labCode: string
    versionNo: number
    canStart: boolean
    blockReasons: string[]
    preconditions: PreconditionView[]
}

export function listLabs(params?: {
    techStack?: string
    difficulty?: string
    keyword?: string
    page?: number
    size?: number
}) {
    return request
        .get<ApiResponse<PagedLab>>('/api/v1/labs', { params })
        .then((r) => r.data.data)
}

export function getLabDetail(labCode: string) {
    return request.get<ApiResponse<LabDetail>>(`/api/v1/labs/${labCode}`).then((r) => r.data.data)
}

export function checkEligibility(labCode: string) {
    return request
        .get<ApiResponse<EligibilityView>>(`/api/v1/labs/${labCode}/start-eligibility`)
        .then((r) => r.data.data)
}