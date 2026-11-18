import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
    advanceSession,
    getSessionContext,
    getTimeline,
    type ContextView,
    type TimelineItem,
} from '../api/session'

function SessionDetail() {
    const { sessionId } = useParams<{ sessionId: string }>()
    const [ctx, setCtx] = useState<ContextView | null>(null)
    const [timeline, setTimeline] = useState<TimelineItem[]>([])
    const [error, setError] = useState('')

    const refresh = () => {
        if (!sessionId) return
        Promise.all([getSessionContext(sessionId), getTimeline(sessionId)])
            .then(([c, t]) => {
                setCtx(c)
                setTimeline(t)
            })
            .catch((err) => setError(err.message))
    }

    useEffect(() => {
        refresh()
    }, [sessionId])

    const handleAdvance = () => {
        if (!ctx || !sessionId) return
        advanceSession(sessionId, ctx.currentStepNo)
            .then(() => refresh())
            .catch((err) => setError(err.message))
    }

    if (error) return <div style={{ padding: '2rem', color: 'red' }}>错误: {error}</div>
    if (!ctx) return <div style={{ padding: '2rem' }}>加载中...</div>

    return (
        <div style={{ padding: '2rem' }}>
            <h1>会话状态</h1>
            <p>
                状态: <b>{ctx.stateLabel}</b> | 当前步骤: {ctx.currentStepNo}/{ctx.totalSteps}
            </p>
            <p>已完成步骤: {ctx.completedSteps.join(', ') || '无'}</p>
            <p>实验目标: {ctx.objective}</p>
            <button onClick={handleAdvance} disabled={ctx.stateLabel !== 'IN_PROGRESS'}>
                完成当前步骤
            </button>
            <h3>时间线</h3>
            <ul>
                {timeline.map((t) => (
                    <li key={t.recordId}>
                        {t.occurredAt} - [{t.eventType}] {t.summary}
                    </li>
                ))}
            </ul>
        </div>
    )
}

export default SessionDetail