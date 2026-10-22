$ErrorActionPreference = 'Stop'
$taskProject = Split-Path -Parent $PSScriptRoot
$taskOriginal = Split-Path -Parent $taskProject
$taskJavaHome = $env:JAVA_HOME
if (-not $taskJavaHome) { $taskJavaHome = Join-Path $taskOriginal '.tools/jdk-25.0.4.1+1' }
$taskClasses = Join-Path $PSScriptRoot 'parity-classes'
$taskJar = Join-Path $taskProject 'baseline/medievalstatus-1.0.0.jar'
New-Item -ItemType Directory -Force -Path $taskClasses | Out-Null
& (Join-Path $taskJavaHome 'bin/javac.exe') -d $taskClasses `
    (Join-Path $taskProject 'src/main/java/dev/medievalstatus/core/Progression.java') `
    (Join-Path $taskProject 'src/test/java/dev/medievalstatus/OriginalJarParityTest.java') `
    (Join-Path $taskProject 'src/test/java/dev/medievalstatus/AssetParityTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Standalone parity compilation failed' }
& (Join-Path $taskJavaHome 'bin/java.exe') -cp $taskClasses dev.medievalstatus.OriginalJarParityTest $taskJar 2>&1 |
    Tee-Object (Join-Path $PSScriptRoot 'original-jar-progression-parity.txt')
if ($LASTEXITCODE -ne 0) { throw 'Original JAR progression parity failed' }
& (Join-Path $taskJavaHome 'bin/java.exe') -cp $taskClasses dev.medievalstatus.AssetParityTest $taskJar `
    (Join-Path $taskProject 'src/main/resources') 2>&1 |
    Tee-Object (Join-Path $PSScriptRoot 'asset-parity.txt')
if ($LASTEXITCODE -ne 0) { throw 'Resource parity failed' }
