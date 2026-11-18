import { createBrowserRouter } from 'react-router-dom'
import Home from '../pages/Home'
import LabList from '../pages/LabList'
import LabDetail from '../pages/LabDetail'
import SessionDetail from '../pages/SessionDetail'

const router = createBrowserRouter([
    { path: '/', element: <Home /> },
    { path: '/labs', element: <LabList /> },
    { path: '/labs/:labCode', element: <LabDetail /> },
    { path: '/sessions/:sessionId', element: <SessionDetail /> },
])

export default router