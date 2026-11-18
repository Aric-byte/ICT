import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listLabs, type LabSummary } from '../api/lab'

function LabList() {
    const [labs, setLabs] = useState<LabSummary[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    useEffect(() => {
        listLabs()
            .then((data) => setLabs(data.items))
            .catch((e) => setError(e.message))
            .finally(() => setLoading(false))
    }, [])

    if (loading) return <div style={{ padding: '2rem' }}>加载中...</div>
    if (error) return <div style={{ padding: '2rem', color: 'red' }}>错误: {error}</div>

    return (
        <div style={{ padding: '2rem' }}>
            <h1>实验列表</h1>
            {labs.length === 0 && <p>暂无已发布实验（请先运行 dev Seeder 或创建实验）</p>}
            <ul>
                {labs.map((lab) => (
                    <li key={lab.labCode} style={{ marginBottom: '0.5rem' }}>
                        <Link to={`/labs/${lab.labCode}`}>{lab.name}</Link>
                        <span>
                            {' '}
                            [{lab.techStack} / {lab.difficulty}] v{lab.effectiveVersionNo}
                        </span>
                    </li>
                ))}
            </ul>
        </div>
    )
}

export default LabList