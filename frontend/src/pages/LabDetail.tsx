import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { checkEligibility, getLabDetail, type LabDetail as LabDetailType } from '../api/lab'
import { startSession } from '../api/session'

function LabDetail() {
    const { labCode } = useParams<{ labCode: string }>()
    const navigate = useNavigate()
    const [lab, setLab] = useState<LabDetailType | null>(null)
    const [canStart, setCanStart] = useState(false)
    const [blockReasons, setBlockReasons] = useState<string[]>([])
    const [error, setError] = useState('')

    useEffect(() => {
        if (!labCode) return
        Promise.all([getLabDetail(labCode), checkEligibility(labCode)])
            .then(([d, e]) => {
                setLab(d)
                setCanStart(e.canStart)
                setBlockReasons(e.blockReasons)
            })
            .catch((err) => setError(err.message))
    }, [labCode])

    const handleStart = () => {
        if (!lab) return
        startSession(lab.labId, lab.versionNo)
            .then((s) => navigate(`/sessions/${s.sessionId}`))
            .catch((err) => setError(err.message))
    }

    if (error) return <div style={{ padding: '2rem', color: 'red' }}>错误: {error}</div>
    if (!lab) return <div style={{ padding: '2rem' }}>加载中...</div>

    return (
        <div style={{ padding: '2rem' }}>
            <h1>{lab.name}</h1>
            <p>{lab.objective}</p>
            <p>
                技术栈: {lab.techStack} | 难度: {lab.difficulty} | 版本: v{lab.versionNo}
            </p>
            <h3>步骤</h3>
            <ol>
                {lab.steps.map((s) => (
                    <li key={s.stepNo}>
                        <b>{s.title}</b> - {s.instruction}
                    </li>
                ))}
            </ol>
            <h3>前置条件</h3>
            <ul>
                {lab.preconditions.map((p) => (
                    <li key={p.sortNo}>
                        [{p.level}] {p.description}
                    </li>
                ))}
            </ul>
            {blockReasons.length > 0 && (
                <div style={{ color: 'orange' }}>不可开始: {blockReasons.join('; ')}</div>
            )}
            <button onClick={handleStart} disabled={!canStart} style={{ marginTop: '1rem' }}>
                开始实验
            </button>
        </div>
    )
}

export default LabDetail