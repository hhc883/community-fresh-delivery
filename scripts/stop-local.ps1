$ErrorActionPreference = 'Stop'
$statePath = Join-Path (Split-Path -Parent $PSScriptRoot) '.local\manual-demo\processes.json'
if (!(Test-Path -LiteralPath $statePath)) { Write-Host 'No manual demo launch is recorded.'; exit 0 }
$apps = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
foreach ($app in $apps) {
    $process = Get-Process -Id $app.pid -ErrorAction SilentlyContinue
    if (!$process) { continue }
    $recorded = if ($app.startedAt -is [DateTime]) { $app.startedAt.ToUniversalTime() } else { [DateTime]::Parse($app.startedAt).ToUniversalTime() }
    if ([Math]::Abs(($process.StartTime.ToUniversalTime() - $recorded).TotalSeconds) -lt 1) {
        Stop-Process -Id $process.Id
        Wait-Process -Id $process.Id -Timeout 15 -ErrorAction SilentlyContinue
        Write-Host ('Stopped ' + $app.name)
    } else {
        Write-Warning ('PID reused; refusing to stop unrelated process ' + $app.pid)
    }
}
Write-Host 'Application processes stopped. Demo data and MySQL/Redis are preserved.'
