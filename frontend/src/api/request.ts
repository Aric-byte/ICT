import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiResponse } from '../types/api'

const request: AxiosInstance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '',
    timeout: 30000,
})

request.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        config.headers.set('X-Operator-Id', localStorage.getItem('operatorId') || 'student-1')
        config.headers.set('X-Operator-Role', localStorage.getItem('operatorRole') || 'STUDENT')
        return config
    },
    (error) => Promise.reject(error),
)

request.interceptors.response.use(
    (response: AxiosResponse<ApiResponse>) => {
        const body = response.data
        if (body && typeof body.code === 'number' && body.code !== 200) {
            return Promise.reject(new Error(body.message || '请求失败'))
        }
        return response
    },
    (error) => {
        const msg = error.response?.data?.message || error.message || '网络错误'
        return Promise.reject(new Error(msg))
    },
)

export default request