# CloudLab Copilot 后端启动脚本（动态路径，前台运行，关闭窗口即停止）
# 用法：右键 -> 使用 PowerShell 运行，或在终端中运行：.\scripts\run-backend.ps1
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = (Resolve-Path "$scriptDir\..").Path
$backendDir = Join-Path $projectRoot "backend"
$envFile = Join-Path $projectRoot ".env"

Set-Location $backendDir

if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
            Set-Item -Path "env:$($matches[1].Trim())" -Value $matches[2].Trim()
        }
    }
}

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " CloudLab Copilot 后端启动中..." -ForegroundColor Cyan
Write-Host " 工作目录: $backendDir" -ForegroundColor DarkGray
Write-Host " 健康检查: http://localhost:8080/actuator/health" -ForegroundColor Cyan
Write-Host " API 文档: http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
Write-Host " 关闭此窗口即停止服务" -ForegroundColor Yellow
Write-Host "=========================================" -ForegroundColor Cyan

$jar = Join-Path $backendDir "target\cloudlab-copilot-backend-1.0.0-SNAPSHOT.jar"
if (-not (Test-Path $jar)) {
    Write-Host "未找到构建产物，正在执行打包..." -ForegroundColor Yellow
    mvn clean package -DskipTests
}
java -jar $jar
