$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Out = Join-Path $Root "out"

if (Test-Path $Out) {
    Remove-Item $Out -Recurse -Force
}
New-Item -ItemType Directory -Path $Out | Out-Null

$MainDir = Join-Path $Root "src\main\java"
$TestDir = Join-Path $Root "src\test\java"

$Sources = @()
if (Test-Path $MainDir) {
    $Sources += Get-ChildItem -Path $MainDir -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
}
if (Test-Path $TestDir) {
    $Sources += Get-ChildItem -Path $TestDir -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
}

if ($Sources.Count -eq 0) {
    throw "No Java source files found."
}

& javac -encoding UTF-8 -d $Out @Sources
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

& java -cp $Out com.example.compiler.test.TotalIntegrationTest
exit $LASTEXITCODE
