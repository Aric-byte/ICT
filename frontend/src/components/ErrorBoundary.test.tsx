import { render } from '@testing-library/react'
import ErrorBoundary from './ErrorBoundary'

function ThrowError(): null {
    throw new Error('test error')
}

describe('ErrorBoundary', () => {
    it('renders children when no error', () => {
        const { container } = render(
            <ErrorBoundary>
                <div>正常内容</div>
            </ErrorBoundary>
        )
        expect(container.textContent).toContain('正常内容')
    })

    it('renders fallback on error', () => {
        const { container } = render(
            <ErrorBoundary>
                <ThrowError />
            </ErrorBoundary>
        )
        expect(container.textContent).toContain('页面出现异常')
    })
})