param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$MavenHome = $env:MAVEN_HOME,
    [string]$Test,
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$mavenArgs = @('-B', '-ntp', '-f', (Join-Path $repoRoot 'backEnd\pom.xml'),
    "-Dmaven.repo.local=$(Join-Path $repoRoot '.local\m2')", 'test')
if ($Test) {
    $mavenArgs += "-Dtest=$Test"
    $mavenArgs += '-Dsurefire.failIfNoSpecifiedTests=false'
}
if ($Offline) { $mavenArgs += '-o' }

if ($JavaHome) {
    if (-not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\java.exe'))) {
        throw "Java executable not found under JavaHome: $JavaHome"
    }
    $env:JAVA_HOME = $JavaHome
}
if ($MavenHome) {
    $maven = Join-Path $MavenHome 'bin\mvn.cmd'
    if (-not (Test-Path -LiteralPath $maven)) { throw "Maven not found: $maven" }
} else {
    $maven = (Get-Command mvn -ErrorAction Stop).Source
}

& $maven @mavenArgs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
