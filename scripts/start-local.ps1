param(
    [string]$Database = 'cfd_demo',
    [string]$RuntimeRoot = '',
    [string]$PythonBin = '',
    [string]$JavaBin = ''
)
if ($Database -notmatch '^cfd_demo[a-zA-Z0-9_]*$') { throw 'Local launcher only accepts an explicitly named cfd_demo database.' }
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$project = Split-Path -Parent $repo
$runtime = if ($RuntimeRoot) { $RuntimeRoot } else { Join-Path $project 'community-fresh-delivery-env-completion\.local\runtime' }
$mysqlBase = Join-Path $runtime 'mysql-8.0.44-winx64'
$mysqlData = Join-Path $project 'community-fresh-delivery-env-completion\.local\mysql-data'
$redisBase = Join-Path $runtime 'redis\Redis-8.10.2-Windows-x64-msys2'
$python = if ($PythonBin) { $PythonBin } else { $command = Get-Command python -ErrorAction SilentlyContinue; if ($command) { $command.Source } else { Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' } }
$java = if ($JavaBin) { $JavaBin } else { $command = Get-Command java -ErrorAction SilentlyContinue; if ($command) { $command.Source } else { Join-Path $project '.dev-tools\jdk-17.0.20.1+1\bin\java.exe' } }
$mysql = Join-Path $mysqlBase 'bin\mysql.exe'
$mysqld = Join-Path $mysqlBase 'bin\mysqld.exe'
$redis = Join-Path $redisBase 'redis-server.exe'
$bundle = $repo
$jar = Join-Path $repo 'backEnd\sky-server\target\sky-server-1.0-SNAPSHOT.jar'
$logs = Join-Path $repo '.local\manual-demo'
$database = $Database
foreach ($path in @($python,$java,$mysql,$mysqld,$redis,$jar,(Join-Path $mysqlData 'auto.cnf'))) {
    if (!(Test-Path -LiteralPath $path)) { throw "Missing local dependency: $path" }
}
foreach ($port in @(8080,5173,5177)) {
    if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) {
        throw "Port $port is occupied. Stop that application or run scripts/stop-local.ps1 before restarting. No process was stopped."
    }
}
New-Item -ItemType Directory -Path $logs -Force | Out-Null
foreach ($relative in @('front/dist/build/h5/index.html','front-admin/dist/index.html','front-admin-sky/dist/index.html')) {
    if (!(Test-Path -LiteralPath (Join-Path $repo $relative))) { throw "Frontend build missing: $relative" }
}
# Java runs a copy so Windows never locks Maven's build output.
$runtimeJar = Join-Path $logs ('server-' + [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss-fff') + '.jar')
Copy-Item -LiteralPath $jar -Destination $runtimeJar
$jar = $runtimeJar

function Wait-LocalPort([int]$port, [int]$seconds) {
    $deadline = [DateTime]::UtcNow.AddSeconds($seconds)
    do {
        $client = New-Object System.Net.Sockets.TcpClient
        try {
            $task = $client.ConnectAsync('127.0.0.1', $port)
            if ($task.Wait(400) -and $client.Connected) { return }
        } catch {} finally { $client.Dispose() }
        Start-Sleep -Milliseconds 400
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "Port $port did not become ready. Read logs in $logs"
}

function Ensure-Dependency([int]$port, [string]$exe, [string[]]$arguments, [string]$workingDirectory, [string]$name) {
    $listener = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($listener) {
        $owner = Get-CimInstance Win32_Process -Filter ('ProcessId=' + $listener.OwningProcess)
        if (!$owner -or ![string]::Equals($owner.ExecutablePath, $exe, [StringComparison]::OrdinalIgnoreCase)) {
            throw "Port $port belongs to another dependency installation. Refusing to use an unknown database/cache."
        }
    } else {
        $child = Start-Process -FilePath $exe -ArgumentList $arguments -WorkingDirectory $workingDirectory -WindowStyle Hidden `
            -RedirectStandardOutput (Join-Path $logs ($name + '.stdout.log')) `
            -RedirectStandardError (Join-Path $logs ($name + '.stderr.log')) -PassThru
        Start-Sleep -Milliseconds 500
        $child.Refresh()
        if ($child.HasExited -and $child.ExitCode -ne 0) {
            $detail = Get-Content -LiteralPath (Join-Path $logs ($name + '.stderr.log')) -Tail 10 -ErrorAction SilentlyContinue
            throw ("Dependency $name exited with code " + $child.ExitCode + ": " + ($detail -join ' '))
        }
    }
    Wait-LocalPort $port 60
}

Ensure-Dependency 13306 $mysqld @('--no-defaults',('--basedir=' + $mysqlBase),('--datadir=' + $mysqlData),'--port=13306','--bind-address=127.0.0.1','--mysqlx=OFF','--console') $mysqlBase 'mysql'
Ensure-Dependency 16379 $redis @('--bind','127.0.0.1','--port','16379','--appendonly','no','--dir',$logs) $redisBase 'redis'

# All writes are confined to this dedicated local demo database. Existing data is preserved.
$env:DB_HOST = '127.0.0.1'
$env:DB_PORT = '13306'
$env:DB_NAME = $database
$env:DB_USER = 'root'
$env:DB_PASSWORD = ''
$env:MYSQL_PWD = ''
$env:MYSQL_BIN = $mysql
$sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$database';"
$count = & $mysql --no-defaults --protocol=TCP --host=127.0.0.1 --port=13306 --user=root --batch --skip-column-names ('--execute=' + $sql)
if ($LASTEXITCODE -ne 0) { throw 'Cannot access the known portable demo MySQL. Read its log; do not change another MySQL installation.' }
if ([int]$count -eq 0) {
    foreach ($action in @('init','migrate','seed')) {
        & $python (Join-Path $bundle 'scripts\database.py') $action --confirm-database $database
        if ($LASTEXITCODE -ne 0) { throw "Database $action failed; no existing database was reset." }
    }
} else {
    Write-Host "Keeping existing demo data in $database (no init, seed or reset)."
}

$env:REDIS_HOST = '127.0.0.1'
$env:REDIS_PORT = '16379'
$env:REDIS_PASSWORD = ''
$env:SERVER_PORT = '8080'
$env:SERVER_ADDRESS = '127.0.0.1'
$env:AUTH_MOCK_LOGIN = 'true'
$env:AUTH_DEV_CODE = '123456'
$env:WECHAT_MOCK_LOGIN = 'true'
$env:SKY_SCHEDULING_ENABLED = 'true'
$apps = New-Object System.Collections.ArrayList
$statePath = Join-Path $logs 'processes.json'
function Start-DemoApp([string]$name, [string]$exe, [string[]]$arguments, [string]$workingDirectory) {
    $quotedArguments = @($arguments | ForEach-Object { '"' + $_.Replace('"','\"') + '"' });
    $process = Start-Process -FilePath $exe -ArgumentList $quotedArguments -WorkingDirectory $workingDirectory -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $logs ($name + '.stdout.log')) `
        -RedirectStandardError (Join-Path $logs ($name + '.stderr.log')) -PassThru
    $null = $apps.Add([pscustomobject]@{name=$name;pid=$process.Id;startedAt=$process.StartTime.ToUniversalTime().ToString('o')})
    ConvertTo-Json -InputObject @($apps.ToArray()) | Set-Content -LiteralPath $statePath -Encoding UTF8
}
try {
    Start-DemoApp 'backend' $java @('-Duser.timezone=Asia/Shanghai','-jar',$jar,'--spring.profiles.active=dev') $bundle
    Wait-LocalPort 8080 90
    $staticServer = Join-Path $repo 'scripts\serve-frontends.py'
    Start-DemoApp 'user-h5' $python @($staticServer,'--directory',(Join-Path $repo 'front\dist\build\h5'),'--backend','http://127.0.0.1:8080','--port','5173') $repo
    Start-DemoApp 'admin' $python @($staticServer,'--directory',(Join-Path $repo 'front-admin-sky\dist'),'--extensions',(Join-Path $repo 'front-admin\dist'),'--backend','http://127.0.0.1:8080','--port','5177','--spa') $repo
    Wait-LocalPort 5173 15
    Wait-LocalPort 5177 15
    Write-Host ''
    Write-Host 'User site:  http://127.0.0.1:5173'
    Write-Host 'Admin site: http://127.0.0.1:5177'
    Write-Host 'Backend:    http://127.0.0.1:8080'
    Write-Host 'Demo admin: admin / 123456; development login code: 123456 (no real SMS/payment).'
    Write-Host "Database: $database; logs: $logs"
    Write-Host 'Stop the three app processes with scripts/stop-local.ps1; MySQL/Redis are preserved.'
} catch {
    Write-Warning ('Startup incomplete. Run scripts/stop-local.ps1 to stop only the app processes from this launch. ' + $_.Exception.Message)
    throw
}
