import { useState } from 'react'
import { askStream, type AnswerView } from '../api/diagnosis'

function DiagnosisPage() {
    const [question, setQuestion] = useState('')
    const [techStack, setTechStack] = useState('OPENGAUSS')
    const [streaming, setStreaming] = useState(false)
    const [explanation, setExplanation] = useState('')
    const [answer, setAnswer] = useState<AnswerView | null>(null)
    const [error, setError] = useState('')

    const handleAsk = () => {
        if (!question.trim()) return
        setStreaming(true)
        setExplanation('')
        setAnswer(null)
        setError('')
        askStream(question, techStack, {
            onChunk: (c) => setExplanation((prev) => prev + c.delta),
            onFinal: (a) => {
                setAnswer(a)
                setStreaming(false)
            },
            onError: (e) => {
                setError(e.message)
                setStreaming(false)
            },
        })
    }

    return (
        <div style={{ padding: '2rem' }}>
            <h1>智能诊断问答</h1>
            <p style={{ color: '#666' }}>结合实验状态与知识库，给出可溯源的诊断建议（系统不执行任何命令）</p>
            <div>
                <label>技术栈：</label>
                <select value={techStack} onChange={(e) => setTechStack(e.target.value)}>
                    <option value="OPENGAUSS">openGauss</option>
                    <option value="MYSQL">MySQL</option>
                    <option value="MYBATIS">MyBatis</option>
                </select>
            </div>
            <textarea
                value={question}
                onChange={(e) => setQuestion(e.target.value)}
                placeholder="描述你遇到的问题，如：为什么 openGauss 连接失败？"
                style={{ width: '100%', height: '100px', marginTop: '0.5rem' }}
            />
            <button onClick={handleAsk} disabled={streaming} style={{ marginTop: '0.5rem' }}>
                {streaming ? '诊断中...' : '提问'}
            </button>

            {error && <div style={{ color: 'red', marginTop: '1rem' }}>错误: {error}</div>}

            {explanation && (
                <div style={{ marginTop: '1rem', background: '#f5f5f5', padding: '1rem' }}>
                    <b>解释：</b>
                    {explanation}
                </div>
            )}

            {answer && (
                <div style={{ marginTop: '1rem' }}>
                    <p>
                        状态: <b>{answer.generationStatus}</b> | 置信度: {answer.confidence}（
                        {answer.confidenceReason}）
                    </p>
                    <h3>可能原因</h3>
                    <ul>
                        {answer.causes.map((c, i) => (
                            <li key={i}>
                                [{c.confidence}] {c.text}
                            </li>
                        ))}
                    </ul>
                    <h3>建议下一步</h3>
                    <p>
                        {answer.nextStep.action}（风险: {answer.nextStep.riskLevel}）
                    </p>
                    <h3>依据</h3>
                    <ul>
                        {answer.citations.map((c, i) => (
                            <li key={i}>
                                [{c.sourceType}] {c.title}
                            </li>
                        ))}
                    </ul>
                    <p style={{ color: '#888' }}>{answer.safetyNotice}</p>
                </div>
            )}
        </div>
    )
}

export default DiagnosisPage