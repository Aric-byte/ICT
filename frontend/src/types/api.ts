export interface ApiResponse<T = unknown> {
    code: number
    message: string
    data: T
    traceId?: string
}