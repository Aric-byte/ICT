export class BizError extends Error {
    code: number
    constructor(code: number, message: string) {
        super(message)
        this.name = 'BizError'
        this.code = code
    }
}