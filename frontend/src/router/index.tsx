import { createBrowserRouter } from 'react-router-dom'
import Layout from '../components/Layout'
import DiagnosisPage from '../pages/DiagnosisPage'
import LabList from '../pages/LabList'
import LabDetail from '../pages/LabDetail'
import SessionDetail from '../pages/SessionDetail'

const router = createBrowserRouter([
    {
        path: '/',
        element: (
            <Layout>
                <DiagnosisPage />
            </Layout>
        ),
    },
    {
        path: '/diagnosis',
        element: (
            <Layout>
                <DiagnosisPage />
            </Layout>
        ),
    },
    {
        path: '/labs',
        element: (
            <Layout>
                <LabList />
            </Layout>
        ),
    },
    {
        path: '/labs/:labCode',
        element: (
            <Layout>
                <LabDetail />
            </Layout>
        ),
    },
    {
        path: '/sessions/:sessionId',
        element: (
            <Layout>
                <SessionDetail />
            </Layout>
        ),
    },
])

export default router
