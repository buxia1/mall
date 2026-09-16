# 模块 2 验收脚本：后台认证与令牌链路。
#
# 前置：mall-admin 已在 8080 运行，MySQL(3307)/Redis(6379) 就绪。
# 用法：powershell -NoProfile -ExecutionPolicy Bypass -File scripts/acceptance/module2-auth.ps1
param(
    [string]$BaseUrl = "http://localhost:8080"
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Stop"

function Read-Utf8Body($stream) {
    if (-not $stream) { return "" }
    $ms = New-Object System.IO.MemoryStream
    $stream.CopyTo($ms)
    return [System.Text.Encoding]::UTF8.GetString($ms.ToArray())
}

function Call($method, $path, $body, $token) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    $params = @{ Uri = "$BaseUrl$path"; Method = $method; Headers = $headers; TimeoutSec = 20; UseBasicParsing = $true }
    if ($body) { $params["ContentType"] = "application/json"; $params["Body"] = $body }
    try {
        $r = Invoke-WebRequest @params
        return [pscustomobject]@{ Http = [int]$r.StatusCode; Body = (Read-Utf8Body $r.RawContentStream) }
    } catch {
        $resp = $_.Exception.Response
        $status = 0
        if ($resp) { $status = [int]$resp.StatusCode }
        $text = (Read-Utf8Body $resp.GetResponseStream())
        if ([string]::IsNullOrWhiteSpace($text) -and $_.ErrorDetails) { $text = $_.ErrorDetails.Message }
        return [pscustomobject]@{ Http = $status; Body = $text }
    }
}

function Json($text) { if ([string]::IsNullOrWhiteSpace($text)) { return $null } return ($text | ConvertFrom-Json) }

$pass = 0; $fail = 0
function Check($name, $ok, $detail) {
    if ($ok) { $script:pass++; Write-Output ("PASS  {0,-40} {1}" -f $name, $detail) }
    else { $script:fail++; Write-Output ("FAIL  {0,-40} {1}" -f $name, $detail) }
}

$login = Call POST "/admin/login" '{"username":"admin","password":"macro123"}' $null
$lj = Json $login.Body
$token = $lj.data.token
Check "POST /admin/login" ($login.Http -eq 200 -and $lj.code -eq 200 -and $token) "HTTP $($login.Http) code=$($lj.code) tokenHead='$($lj.data.tokenHead)' tokenLen=$($token.Length)"

$info = Call GET "/admin/info" $null $token
$ij = Json $info.Body
Check "GET /admin/info" ($info.Http -eq 200 -and $ij.code -eq 200 -and $ij.data.username -eq "admin") "HTTP $($info.Http) code=$($ij.code) username=$($ij.data.username) roles=$($ij.data.roles.Count) menus=$($ij.data.menus.Count)"

$noToken = Call GET "/admin/info" $null $null
$nj = Json $noToken.Body
Check "GET /admin/info no token -> 401" ($noToken.Http -eq 401 -and $nj.code -eq 401) "HTTP $($noToken.Http) code=$($nj.code) message=$($nj.message)"

$bad = Call GET "/admin/info" $null "not-a-real-token"
$bj = Json $bad.Body
Check "GET /admin/info bad token -> 401" ($bad.Http -eq 401 -and $bj.code -eq 401) "HTTP $($bad.Http) code=$($bj.code) message=$($bj.message)"

$forbidden = Call GET "/not-a-managed-resource" $null $token
$fj = Json $forbidden.Body
Check "GET /not-a-managed-resource -> 403" ($forbidden.Http -eq 403 -and $fj.code -eq 403) "HTTP $($forbidden.Http) code=$($fj.code) message=$($fj.message)"

$refresh = Call GET "/admin/refreshToken" $null $token
$rj = Json $refresh.Body
Check "GET /admin/refreshToken" ($refresh.Http -eq 200 -and $rj.code -eq 200 -and $rj.data.token -eq $token) "HTTP $($refresh.Http) code=$($rj.code) tokenUnchanged=$($rj.data.token -eq $token)"

$logout = Call POST "/admin/logout" $null $token
$oj = Json $logout.Body
Check "POST /admin/logout" ($logout.Http -eq 200 -and $oj.code -eq 200) "HTTP $($logout.Http) code=$($oj.code)"

$after = Call GET "/admin/info" $null $token
$aj = Json $after.Body
Check "GET /admin/info after logout -> 401" ($after.Http -eq 401 -and $aj.code -eq 401) "HTTP $($after.Http) code=$($aj.code) message=$($aj.message)"

$wrong = Call POST "/admin/login" '{"username":"admin","password":"definitely-wrong"}' $null
$wj = Json $wrong.Body
Check "POST /admin/login wrong password -> 500" ($wrong.Http -eq 200 -and $wj.code -eq 500) "HTTP $($wrong.Http) code=$($wj.code) message=$($wj.message)"

Write-Output ""
Write-Output "RESULT: pass=$pass fail=$fail"
if ($fail -gt 0) { exit 1 }
