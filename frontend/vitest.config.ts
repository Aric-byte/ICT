import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import swc from 'unplugin-swc'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
    plugins: [
        swc.vite({
            module: { type: 'es6' },
            jsc: {
                target: 'es2022',
                parser: { syntax: 'typescript', tsx: true },
                transform: { react: { runtime: 'automatic' } },
            },
        }),
        react(),
    ],
    test: {
        environment: 'jsdom',
        globals: true,
        setupFiles: [],
    },
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url)),
        },
    },
})