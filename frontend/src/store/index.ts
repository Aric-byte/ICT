import { create } from 'zustand'

interface AppState {
    appName: string
}

export const useAppStore = create<AppState>(() => ({
    appName: 'CloudLab Copilot',
}))