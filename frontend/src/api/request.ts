import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiResponse } from '../types/api'

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
        return response
    },
    (error) => {
        return Promise.reject(error)
    },
)

export default request