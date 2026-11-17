@echo off
chcp 65001 >nul
echo =========================================
echo  CloudLab Copilot 后端启动中...
echo  健康检查: http://localhost:8080/actuator/health
echo  关闭此窗口即停止服务
echo =========================================
powershell -ExecutionPolicy Bypass -File "%~dp0run-backend.ps1"
pause