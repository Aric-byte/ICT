# 业务与诊断 API 快速验证（需先启动后端）
# 用法：后端运行后，在 PowerShell 窗口执行：.\scripts\test-api.ps1
$base = "http://localhost:8080/api/v1"
$teacher = @{ "X-Operator-Id" = "teacher-1"; "X-Operator-Role" = "AUTHOR" }
$student = @{ "X-Operator-Id" = "student-1"; "X-Operator-Role" = "STUDENT" }
$code = "LAB-" + (Get-Date -Format "HHmmss")

try {
    Write-Host ">>> 1. 验证健康检查端点..." -ForegroundColor Cyan
    $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"
    Write-Host "   Health Status: $($health.status)" -ForegroundColor Green

    Write-Host ">>> 2. 验证问答与诊断端点..." -ForegroundColor Cyan
    $askBody = @{
        question = "Java Spring Boot 报错: No qualifying bean of type 'UserService' available"
        techStack = "JAVA"
    } | ConvertTo-Json
    $qaRes = Invoke-RestMethod -Uri "$base/diagnosis/questions" -Method Post -ContentType "application/json" -Body $askBody
    Write-Host "   问答接口响应成功: code=$($qaRes.code)" -ForegroundColor Green
    Write-Host "   诊断结论: $($qaRes.data.restate)" -ForegroundColor Yellow

    Write-Host ">>> 3. 验证实验生命周期..." -ForegroundColor Cyan
    $body1 = @{ labCode = $code; name = "Java Web 入门实验"; techStack = "JAVA"; difficulty = "BEGINNER"; objective = "掌握 Spring Boot 常见问题排错"; estimatedMinutes = 30; tags = @("Java", "Spring") } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$base/labs" -Method Post -Headers $teacher -ContentType "application/json" -Body $body1
    Write-Host "   创建实验: code=$($r.code) labCode=$($r.data.labCode)" -ForegroundColor Green

    $body2 = @{ steps = @( @{stepNo = 1; title = "配置依赖"; instruction = "pom.xml 添加 starter-web"; completionCriteria = "编译通过"; hint = "spring-boot-starter-web" } ) } | ConvertTo-Json -Depth 6
    $r = Invoke-RestMethod -Uri "$base/labs/$code/steps" -Method Put -Headers $teacher -ContentType "application/json" -Body $body2
    Write-Host "   保存步骤: code=$($r.code)" -ForegroundColor Green

    $r = Invoke-RestMethod -Uri "$base/labs/$code/publish" -Method Post -Headers $teacher
    Write-Host "   发布实验: code=$($r.code)" -ForegroundColor Green
}
catch {
    Write-Host "执行出错: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host "验证完成。" -ForegroundColor Cyan
