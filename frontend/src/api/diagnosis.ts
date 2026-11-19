import request from './request'
import type { ApiResponse } from '../types/api'

export interface CitationView {
    sourceType: string
    sourceRef: string
    title: string
    excerpt: string
}

export interface CauseView {
    text: string
    confidence: string
    citations: CitationView[]
}

export interface NextStepView {
    action: string
    expectedObservation: string
    riskLevel: string
    citations: CitationView[]
}

export interface AnswerView {
    questionId: string
    generationStatus: string
    restate: string
    explanation: string
    causes: CauseView[]
    nextStep: NextStepView
    citations: CitationView[]
    confidence: string
    confidenceReason: string
    evidenceGaps: string[]
    safetyNotice: string
    techStack: string
    contextCompleteness: string
}

export function ask(question: string, techStack?: string) {
    return request
        .post<ApiResponse<AnswerView>>('/api/v1/diagnosis/questions', { question, techStack })
        .then((r) => r.data.data)
}

export interface StreamHandlers {
    onContext?: (data: Record<string, unknown>) => void
    onChunk?: (data: { section: string; delta: string }) => void
    onNextStep?: (data: NextStepView) => void
    onFinal?: (data: AnswerView) => void
    onError?: (err: Error) => void
}

export async function askStream(
    question: string,
    techStack: string | undefined,
    handlers: StreamHandlers,
) {
    const base = import.meta.env.VITE_API_BASE_URL || ''
    try {
        const resp = await fetch(`${base}/api/v1/diagnosis/questions/stream`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Operator-Id': localStorage.getItem('operatorId') || 'student-1',
                'X-Operator-Role': localStorage.getItem('operatorRole') || 'STUDENT',
            },
            body: JSON.stringify({ question, techStack }),
        })
        if (!resp.body) throw new Error('流式响应为空')
        const reader = resp.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        for (; ;) {
            const { done, value } = await reader.read()
            if (done) break
            buffer += decoder.decode(value, { stream: true })
            const parts = buffer.split('\n\n')
            buffer = parts.pop() || ''
            for (const part of parts) parseEvent(part, handlers)
        }
    } catch (e) {
        handlers.onError?.(e instanceof Error ? e : new Error(String(e)))
    }
}

function parseEvent(raw: string, handlers: StreamHandlers) {
    const lines = raw.split('\n')
    let event = ''
    let data = ''
    for (const line of lines) {
        if (line.startsWith('event:')) event = line.substring(6).trim()
        else if (line.startsWith('data:')) data += line.substring(5).trim()
    }
    if (!event || !data) return
    try {
        const parsed = JSON.parse(data)
        const payload =
            parsed && typeof parsed === 'object' && 'data' in parsed ? (parsed as { data: unknown }).data : parsed
        if (event === 'context') handlers.onContext?.(payload as Record<string, unknown>)
        else if (event === 'chunk') handlers.onChunk?.(payload as { section: string; delta: string })
        else if (event === 'next_step') handlers.onNextStep?.(payload as NextStepView)
        else if (event === 'final') handlers.onFinal?.(payload as AnswerView)
    } catch {
        // ignore malformed event
    }
}