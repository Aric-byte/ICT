# CloudLab Copilot 后端启动脚本（前台运行，关闭窗口即停止）
# 用法：右键 -> 使用 PowerShell 运行，或在 PowerShell 中执行本脚本
Set-Location "D:\VScode Project\ICT\backend"
Get-Content "D:\VScode Project\ICT\.env" | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
        Set-Item -Path "env:$($matches[1].Trim())" -Value $matches[2].Trim()
    }
}
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " CloudLab Copilot 后端启动中..." -ForegroundColor Cyan
Write-Host " 健康检查: http://localhost:8080/actuator/health" -ForegroundColor Cyan
Write-Host " 关闭此窗口即停止服务" -ForegroundColor Yellow
Write-Host "=========================================" -ForegroundColor Cyan
java -jar "boot\target\cloudlab-copilot-boot-1.0.0-SNAPSHOT.jar"