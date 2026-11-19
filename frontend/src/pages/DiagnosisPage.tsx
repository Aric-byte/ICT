import { useState } from 'react'
import { askStream, type AnswerView } from '../api/diagnosis'

const PRESET_CASES = [
    {
        label: '💥 NPE 空指针与源码定位',
        tech: 'JAVA',
        text: `java.lang.NullPointerException: Cannot invoke "String.trim()" because "userToken" is null
    at com.cloudlab.auth.TokenValidator.verify(TokenValidator.java:56)
    at org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:880)`,
    },
    {
        label: '🔌 Spring 端口占用 (8080)',
        tech: 'SPRING',
        text: `Web server failed to start. Port 8080 was already in use.
Action: Identify and stop the process that's listening on port 8080 or configure this application to listen on another port.`,
    },
    {
        label: '🧩 Spring 缺失 Bean 注入',
        tech: 'SPRING',
        text: `org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type 'com.example.service.OrderService' available: expected at least 1 bean which qualifies as autowire candidate.`,
    },
    {
        label: '🗺️ MyBatis 映射语句未找到',
        tech: 'MYBATIS',
        text: `org.apache.ibatis.binding.BindingException: Invalid bound statement (not found): com.example.dao.UserMapper.findActiveUsers`,
    },
    {
        label: '🔨 javac 找不到符号编译报错',
        tech: 'JAVA',
        text: `[ERROR] /src/main/java/com/example/App.java:[28,15] cannot find symbol
  symbol:   variable log
  location: class com.example.App`,
    },
    {
        label: '🛡️ 高危命令与凭据拦截演示',
        tech: 'JAVA',
        text: `如何在 Java 代码里执行系统关机命令？代码如下：Runtime.getRuntime().exec("shutdown -s -t 0");`,
        danger: true,
    },
]

export default function DiagnosisPage() {
    const [question, setQuestion] = useState(PRESET_CASES[0].text)
    const [techStack, setTechStack] = useState('JAVA')
    const [streaming, setStreaming] = useState(false)
    const [explanation, setExplanation] = useState('')
    const [answer, setAnswer] = useState<AnswerView | null>(null)
    const [error, setError] = useState('')
    const [copied, setCopied] = useState(false)

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
                setError(e.message || '诊断请求发生错误，请检查后端是否正常启动')
                setStreaming(false)
            },
        })
    }

    const handleCopy = (text: string) => {
        navigator.clipboard.writeText(text)
        setCopied(true)
        setTimeout(() => setCopied(false), 2000)
    }

    return (
        <div className="main-container">
            {/* 顶部横幅 */}
            <div className="hero-banner">
                <div className="hero-text">
                    <h2>⚡ Java 智能诊断与实验状态感知工作台</h2>
                    <p>
                        专为华为 ICT 大赛与高校开发者打造。通过 Java 专属堆栈感知与 AST 分析，提取 Root Cause 与精准源码位置，
                        结合华为云 MaaS 大模型推理与代码安全护栏，生成最小安全修复步骤与学习证据链。
                    </p>
                    <div className="hero-tags">
                        <span className="hero-tag">🎯 Java 异常堆栈定位</span>
                        <span className="hero-tag">🛡️ 100% 危险操作护栏</span>
                        <span className="hero-tag">☁️ 华为云 MaaS 协同</span>
                        <span className="hero-tag">📝 结构化可溯源证据链</span>
                    </div>
                </div>
            </div>

            {/* 快捷案例芯片 */}
            <div className="presets-section">
                <div className="presets-label">
                    <span>💡 点击快捷体验经典 Java 案例（快速评测演示）：</span>
                </div>
                <div className="preset-chips">
                    {PRESET_CASES.map((item, idx) => (
                        <button
                            key={idx}
                            className={`chip-btn ${item.danger ? 'danger-chip' : ''}`}
                            onClick={() => {
                                setQuestion(item.text)
                                setTechStack(item.tech)
                            }}
                        >
                            {item.label}
                        </button>
                    ))}
                </div>
            </div>

            {/* 核心工作区两列网格 */}
            <div className="workbench-grid">
                {/* 左列：问题输入控制台 */}
                <div className="panel-card">
                    <div className="panel-card-header">
                        <div className="panel-title">
                            <span>📝 提问与报错输入控制台</span>
                        </div>
                        <span style={{ fontSize: '0.78rem', color: '#64748b' }}>支持日志/异常堆栈</span>
                    </div>

                    <div className="form-group">
                        <label className="form-label">目标技术栈 / 场景：</label>
                        <select
                            className="tech-select"
                            value={techStack}
                            onChange={(e) => setTechStack(e.target.value)}
                        >
                            <option value="JAVA">Java Core 基础与核心异常 (NPE/OOM/并发)</option>
                            <option value="SPRING">Spring Boot / Spring Web 框架</option>
                            <option value="MYBATIS">MyBatis / 数据持久层映射</option>
                            <option value="OPENGAUSS">openGauss 华为云分布式数据库</option>
                            <option value="MYSQL">MySQL 关系型数据库</option>
                        </select>
                    </div>

                    <div className="form-group">
                        <label className="form-label">粘贴 Java 异常堆栈、报错日志或提问内容：</label>
                        <textarea
                            className="code-textarea"
                            value={question}
                            onChange={(e) => setQuestion(e.target.value)}
                            placeholder="请在此粘贴完整的 Java StackTrace、javac 编译报错或具体问题..."
                        />
                    </div>

                    <button
                        className="submit-btn"
                        onClick={handleAsk}
                        disabled={streaming || !question.trim()}
                    >
                        {streaming ? '🔄 Java 诊断引擎深度推理中...' : '🚀 开始智能感知与诊断'}
                    </button>
                </div>

                {/* 右列：结构化诊断推理输出 */}
                <div className="panel-card">
                    <div className="panel-card-header">
                        <div className="panel-title">
                            <span>🧠 智能诊断与修复建议</span>
                        </div>
                        {answer && (
                            <span className="badge badge-success">
                                置信度: {answer.confidence} ({answer.confidenceReason})
                            </span>
                        )}
                    </div>

                    {error && (
                        <div className="section-box" style={{ background: '#fef2f2', borderColor: '#fca5a5', color: '#b91c1c' }}>
                            <b>诊断异常:</b> {error}
                        </div>
                    )}

                    {!streaming && !answer && !explanation && (
                        <div className="result-empty">
                            <span style={{ fontSize: '2.5rem' }}>🔍</span>
                            <span>请在左侧输入或点击上方快捷预设，点击“开始智能感知与诊断”</span>
                        </div>
                    )}

                    {streaming && (
                        <div className="thinking-timeline">
                            <div className="status-dot"></div>
                            <span>
                                [1. 凭据与安全审查] ➔ [2. Java 堆栈与源码定位] ➔ [3. 知识检索增强] ➔ [4. 华为云大模型推理]...
                            </span>
                        </div>
                    )}

                    {/* 实时流式解释 */}
                    {explanation && (
                        <div className="section-box">
                            <div className="section-box-title">📖 深度分析与定位结论</div>
                            <div style={{ fontSize: '0.88rem', whiteSpace: 'pre-line', color: '#1e293b' }}>
                                {explanation}
                            </div>
                        </div>
                    )}

                    {/* 完整结构化结果卡片 */}
                    {answer && (
                        <div>
                            {/* 可能原因 */}
                            <div className="section-box">
                                <div className="section-box-title">🔍 根本原因分析 (Root Cause)</div>
                                {answer.causes.map((c, i) => (
                                    <div key={i} className="root-cause-text">
                                        • [{c.confidence} 风险] {c.text}
                                    </div>
                                ))}
                            </div>

                            {/* 最小安全修复建议 */}
                            <div className="section-box" style={{ borderColor: '#bfdbfe', background: '#f0f9ff' }}>
                                <div className="section-box-title" style={{ color: '#0369a1', justifyContent: 'space-between' }}>
                                    <span>💡 最小安全下一步 (Safe Next Step)</span>
                                    <button
                                        onClick={() => handleCopy(answer.nextStep.action)}
                                        style={{ background: 'none', border: 'none', color: '#0284c7', fontSize: '0.78rem' }}
                                    >
                                        {copied ? '✅ 已复制' : '📋 复制建议'}
                                    </button>
                                </div>
                                <div className="code-snippet">{answer.nextStep.action}</div>
                                <div style={{ fontSize: '0.78rem', color: '#0284c7', marginTop: '0.4rem' }}>
                                    预期验证观察: {answer.nextStep.expectedObservation}
                                </div>
                            </div>

                            {/* 可溯源依据 */}
                            {answer.citations.length > 0 && (
                                <div className="section-box">
                                    <div className="section-box-title">📚 可溯源依据 (Citations)</div>
                                    <ul className="citations-list">
                                        {answer.citations.map((c, i) => (
                                            <li key={i}>
                                                <span>📌</span>
                                                <b>[{c.sourceType}]</b> {c.title}
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            )}

                            {/* 安全审查提示 */}
                            <div className="safety-alert">
                                🛡️ <b>安全护栏校验:</b> {answer.safetyNotice}
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}
