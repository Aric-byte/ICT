@echo off
chcp 65001 >nul
echo =========================================
echo  CloudLab Copilot 前端启动中...
echo  访问地址: http://localhost:5173
echo  关闭此窗口即停止服务
echo =========================================
cd /d "%~dp0..\frontend"
npm run dev
pause
