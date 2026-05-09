# PostgreSQL 初始化脚本 (Windows PowerShell)
# 使用前请确认 PostgreSQL 的 bin 目录已加入系统 PATH，或修改下面的 $pgPath

$pgPath = "C:\Program Files\PostgreSQL\18\bin"
$env:Path = "$pgPath;$env:Path"

$hostName = "localhost"
$port = "5432"
$user = "postgres"
$password = Read-Host "请输入 PostgreSQL 管理员密码 (postgres)" -AsSecureString
$BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
$plainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)

# 设置环境变量供 psql 使用
$env:PGPASSWORD = $plainPassword

Write-Host "正在创建数据库 weanalyzer..."
& psql -h $hostName -p $port -U $user -c "CREATE DATABASE weanalyzer WITH ENCODING = 'UTF8' LC_COLLATE = 'zh_CN.UTF-8' LC_CTYPE = 'zh_CN.UTF-8' TEMPLATE = template0;" 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "数据库可能已存在，继续执行..."
}

Write-Host "正在执行建表脚本..."
& psql -h $hostName -p $port -U $user -d weanalyzer -f "e:\微信公众号数据分析工具\sql\init.sql"

if ($LASTEXITCODE -eq 0) {
    Write-Host "数据库初始化完成！"
} else {
    Write-Host "初始化失败，请检查错误信息。"
}

$env:PGPASSWORD = $null
