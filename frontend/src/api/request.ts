import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiResponse } from '../types/api'
import { BizError } from '../types/BizError'

const request: AxiosInstance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    timeout: 30000,
})

request.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        return config
    },
    (error) => {
        return Promise.reject(error)
    },
)

request.interceptors.response.use(
    (response: AxiosResponse<ApiResponse>) => {
        const data = response.data
        if (data && data.code !== 200) {
            const error = new BizError(data.code, data.message)
            console.error(`[BizError] ${data.code}: ${data.message}`)
            return Promise.reject(error)
        }
        return response
    },
    (error) => {
        if (error.response) {
            const status = error.response.status
            if (status === 401) {
                console.warn('未授权，请重新登录')
            } else if (status === 403) {
                console.warn('无权限访问')
            } else if (status >= 500) {
                console.error('服务异常，请稍后重试')
            }
        } else {
            console.error('网络异常，请检查连接')
        }
        return Promise.reject(error)
    },
)

export default request