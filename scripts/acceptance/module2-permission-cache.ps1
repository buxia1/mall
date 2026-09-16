# 模块 2 验收脚本：改权限不重启即生效（Redis 权限缓存 + 失效）。
#
# 前置：mall-admin 已在 8080 运行，MySQL(3307)/Redis(6379) 就绪。
# 用法：powershell -NoProfile -ExecutionPolicy Bypass -File scripts/acceptance/module2-permission-cache.ps1
#
# 步骤：摘掉超级管理员的 /admin/** 授权 -> 不清缓存仍可访问（证明缓存生效）
#       -> 清缓存后立刻 403（证明无需重启）-> 恢复授权。
param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Mysql = "mysql",
    [string]$RedisCli = "redis-cli",
    [string]$DbHost = "127.0.0.1",
    [string]$DbPort = "3307",
    [string]$DbUser = "root",
    [string]$DbPassword = "root",
    [string]$RedisPort = "6379",
    [string]$RedisPassword = "123456"
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Stop"
$cacheKey = "mall:security:authorization:resources"
$roleId = 5
$token = $null

function Read-Utf8Body($stream) {
    if (-not $stream) { return "" }
    $ms = New-Object System.IO.MemoryStream
    $stream.CopyTo($ms)
    return [System.Text.Encoding]::UTF8.GetString($ms.ToArray())
}

function Call($method, $path, $body, $authToken) {
    $headers = @{}
    if ($authToken) { $headers["Authorization"] = "Bearer $authToken" }
    $params = @{ Uri = "$BaseUrl$path"; Method = $method; Headers = $headers; TimeoutSec = 20; UseBasicParsing = $true }
    if ($body) { $params["ContentType"] = "application/json"; $params["Body"] = $body }
    try {
        $r = Invoke-WebRequest @params
        return @{ Http = [int]$r.StatusCode; Body = (Read-Utf8Body $r.RawContentStream) }
    } catch {
        $resp = $_.Exception.Response
        $status = 0
        if ($resp) { $status = [int]$resp.StatusCode }
        $text = (Read-Utf8Body $resp.GetResponseStream())
        if ([string]::IsNullOrWhiteSpace($text) -and $_.ErrorDetails) { $text = $_.ErrorDetails.Message }
        return @{ Http = $status; Body = $text }
    }
}

function Sql($query) {
    # mysql 会往 stderr 写"命令行传密码不安全"的警告；本函数内降级为 Continue 并丢弃 stderr，
    # 避免调用方的 $ErrorActionPreference = "Stop" 把它当成终止错误。
    $ErrorActionPreference = "Continue"
    $out = & $Mysql -h $DbHost -P $DbPort -u $DbUser "-p$DbPassword" --default-character-set=utf8mb4 -N -B -e $query 2>$null
    return (($out -join "").Trim())
}

function EvictCache() {
    $ErrorActionPreference = "Continue"
    & $RedisCli -h $DbHost -p $RedisPort -a $RedisPassword del $cacheKey 2>$null | Out-Null
}

$pass = 0; $fail = 0
function Check($name, $ok, $detail) {
    if ($ok) { $script:pass++; Write-Output ("PASS  {0,-46} {1}" -f $name, $detail) }
    else { $script:fail++; Write-Output ("FAIL  {0,-46} {1}" -f $name, $detail) }
}

$resourceId = Sql "SELECT id FROM mall.ums_resource WHERE url='/admin/**' LIMIT 1;"
if (-not $resourceId) { Write-Output "找不到 /admin/** 资源，确认库已导入 sql/auth-schema.sql 与 sql/auth-seed.sql"; exit 1 }
Write-Output "使用资源 id=$resourceId (url=/admin/**)，角色 id=$roleId"
Write-Output ""

try {
    $login = Call POST "/admin/login" '{"username":"admin","password":"macro123"}' $null
    $token = (($login.Body | ConvertFrom-Json).data.token)

    EvictCache
    $r1 = Call GET "/admin/info" $null $token
    Check "1) 有权限 -> 200" ($r1.Http -eq 200) "HTTP $($r1.Http)"

    Sql "DELETE FROM mall.ums_role_resource_relation WHERE role_id=$roleId AND resource_id=$resourceId;" | Out-Null
    Write-Output "   已在数据库删除 role=$roleId -> resource=$resourceId 的授权"

    $r2 = Call GET "/admin/info" $null $token
    Check "2) 改库但不失效缓存 -> 仍 200（走缓存）" ($r2.Http -eq 200) "HTTP $($r2.Http)"

    EvictCache
    $r3 = Call GET "/admin/info" $null $token
    $b3 = ($r3.Body | ConvertFrom-Json)
    Check "3) 清缓存后不重启 -> 立刻 403" ($r3.Http -eq 403 -and $b3.code -eq 403) "HTTP $($r3.Http) code=$($b3.code) message=$($b3.message)"
} finally {
    if ($resourceId) {
        Sql "DELETE FROM mall.ums_role_resource_relation WHERE role_id=$roleId AND resource_id=$resourceId;" | Out-Null
        Sql "INSERT INTO mall.ums_role_resource_relation (role_id, resource_id) VALUES ($roleId, $resourceId);" | Out-Null
    }
    EvictCache
}

$r4 = Call GET "/admin/info" $null $token
Check "4) 恢复授权并清缓存 -> 恢复 200" ($r4.Http -eq 200) "HTTP $($r4.Http)"

Write-Output ""
Write-Output "RESULT: pass=$pass fail=$fail"
if ($fail -gt 0) { exit 1 }
