import { Link, useLocation } from 'react-router-dom'

interface LayoutProps {
    children: React.ReactNode
}

export default function Layout({ children }: LayoutProps) {
    const location = useLocation()

    return (
        <div>
            <header className="navbar">
                <div className="nav-brand">
                    <span className="brand-badge">华为 ICT 创新赛</span>
                    <div>
                        <div className="brand-title">CloudLab Copilot · JavaMaster Agent</div>
                        <div className="brand-subtitle">基于华为云的 Java 智能诊断与实验状态感知核验平台</div>
                    </div>
                </div>

                <nav className="nav-links">
                    <Link
                        to="/"
                        className={`nav-link ${location.pathname === '/' || location.pathname === '/diagnosis' ? 'active' : ''}`}
                    >
                        ⚡ Java 智能诊断工作台
                    </Link>
                    <Link
                        to="/labs"
                        className={`nav-link ${location.pathname.startsWith('/labs') ? 'active' : ''}`}
                    >
                        📚 实验任务与题库
                    </Link>
                </nav>

                <div className="status-badge">
                    <span className="status-dot"></span>
                    <span>Java Agent 就绪 (Port 8080)</span>
                </div>
            </header>

            <main>{children}</main>
        </div>
    )
}
