import { lazy, Suspense } from 'react'
import { createBrowserRouter } from 'react-router-dom'

const Home = lazy(() => import('../pages/Home'))
const NotFound = lazy(() => import('../pages/NotFound'))

const router = createBrowserRouter([
    {
        path: '/',
        element: (
            <Suspense fallback={<div>加载中...</div>}>
                <Home />
            </Suspense>
        ),
    },
    {
        path: '*',
        element: (
            <Suspense fallback={<div>加载中...</div>}>
                <NotFound />
            </Suspense>
        ),
    },
])

export default router