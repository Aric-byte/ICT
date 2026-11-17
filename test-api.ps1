# 业务 API 快速验证（需先启动后端）
# 用法：后端运行后，在另一个 PowerShell 窗口执行本脚本
$base = "http://localhost:8080/api/v1"
$teacher = @{ "X-Operator-Id" = "teacher-1"; "X-Operator-Role" = "AUTHOR" }
$student = @{ "X-Operator-Id" = "student-1"; "X-Operator-Role" = "STUDENT" }
$code = "LAB-" + (Get-Date -Format "HHmmss")

try {
    $body1 = @{ labCode = $code; name = "MySQL 入门实验"; techStack = "MYSQL"; difficulty = "BEGINNER"; objective = "掌握 MySQL 基本操作"; estimatedMinutes = 30; tags = @("MySQL", "入门") } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$base/labs" -Method Post -Headers $teacher -ContentType "application/json" -Body $body1
    Write-Host "1. 创建实验: code=$($r.code) labCode=$($r.data.labCode)" -ForegroundColor Green

    $body2 = @{ steps = @( @{stepNo = 1; title = "连接数据库"; instruction = "mysql -u root -p"; completionCriteria = "成功连接"; hint = "mysql -u root -p" }, @{stepNo = 2; title = "创建表"; instruction = "CREATE TABLE"; completionCriteria = "表创建成功"; hint = "CREATE TABLE t(id int)" } ) } | ConvertTo-Json -Depth 6
    $r = Invoke-RestMethod -Uri "$base/labs/$code/steps" -Method Put -Headers $teacher -ContentType "application/json" -Body $body2
    Write-Host "2. 保存步骤: code=$($r.code)" -ForegroundColor Green

    $body3 = @{ preconditions = @( @{sortNo = 1; type = "ENV"; level = "REQUIRED"; description = "已安装 MySQL 客户端" } ) } | ConvertTo-Json -Depth 6
    $r = Invoke-RestMethod -Uri "$base/labs/$code/preconditions" -Method Put -Headers $teacher -ContentType "application/json" -Body $body3
    Write-Host "3. 保存前置条件: code=$($r.code)" -ForegroundColor Green

    $r = Invoke-RestMethod -Uri "$base/labs/$code/publish" -Method Post -Headers $teacher
    Write-Host "4. 发布: code=$($r.code)" -ForegroundColor Green

    $r = Invoke-RestMethod -Uri "$base/labs" -Headers $teacher
    Write-Host "5. 目录查询: total=$($r.data.total)" -ForegroundColor Green

    $r = Invoke-RestMethod -Uri "$base/labs/$code" -Headers $teacher
    $labId = $r.data.labId
    Write-Host "6. 详情: labId=$labId versionNo=$($r.data.versionNo) steps=$($r.data.steps.Count)" -ForegroundColor Green

    $body7 = @{ labId = $labId; versionNo = 1; preconditionConfirmed = $true } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$base/sessions" -Method Post -Headers $student -ContentType "application/json" -Body $body7
    $sessionId = $r.data.sessionId
    Write-Host "7. 开始会话: sessionId=$sessionId status=$($r.data.status)" -ForegroundColor Green

    $body8 = @{ expectStepNo = 1 } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$base/sessions/$sessionId/advance" -Method Post -Headers $student -ContentType "application/json" -Body $body8
    Write-Host "8. 推进: currentStep=$($r.data.currentStepNo)" -ForegroundColor Green

    $r = Invoke-RestMethod -Uri "$base/sessions/$sessionId/context" -Headers $student
    Write-Host "9. 上下文: label=$($r.data.stateLabel) currentStep=$($r.data.currentStepNo) completed=$($r.data.completedSteps.Count)" -ForegroundColor Green
}
catch {
    Write-Host "错误: $($_.Exception.Message)" -ForegroundColor Red
}
Read-Host "按回车退出"