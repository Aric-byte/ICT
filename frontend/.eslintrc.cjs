// 注意：ESLint 8.57.0 已 EOL，不再接收安全修复。
// 待前端业务代码落地时迁移至 ESLint 9.x flat config（eslint.config.js）。
module.exports = {
    root: true,
    env: { browser: true, es2020: true },
    extends: [
        'eslint:recommended',
        'plugin:@typescript-eslint/recommended',
        'plugin:react/recommended',
        'plugin:react-hooks/recommended',
    ],
    ignorePatterns: ['dist', '.eslintrc.cjs'],
    parser: '@typescript-eslint/parser',
    parserOptions: { ecmaVersion: 'latest', sourceType: 'module' },
    plugins: ['react-refresh'],
    settings: { react: { version: 'detect' } },
    rules: {
        'react/react-in-jsx-scope': 'off',
        'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
    },
}