# publish-test.ps1
# ---------------------------
# Compila y publica APK de TEST en GitHub Releases (prerelease).
# Se posiciona en el directorio del script y detecta owner/repo desde 'origin'.

param(
  [string]$Notes = "Build de prueba generado automaticamente"
)

# Trabajar desde el directorio del script
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $scriptDir

function Stop-OnError($msg) {
  Write-Error $msg
  Pop-Location
  exit 1
}

# Checks basicos
if (-not (Get-Command git -ErrorAction SilentlyContinue)) { Stop-OnError "git no esta instalado o no esta en PATH." }
if (-not (Get-Command gh  -ErrorAction SilentlyContinue)) { Stop-OnError "GitHub CLI (gh) no esta instalado. Instala con: winget install --id GitHub.cli -e" }

# Detectar owner/repo desde 'origin'
$remoteUrl = (git remote get-url origin) 2>$null
if (-not $remoteUrl) { Stop-OnError "No hay remote 'origin' configurado." }
if ($remoteUrl -match "github\.com[:/](?<owner>[^/]+)/(?<repo>[^/\.]+)(?:\.git)?$") {
  $ghRepo = "$($Matches['owner'])/$($Matches['repo'])"
} else {
  Stop-OnError "No se pudo detectar owner/repo a partir de: $remoteUrl"
}

# Verificar repo y rama actual
$branch = (git rev-parse --abbrev-ref HEAD) 2>$null
if (-not $branch) { Stop-OnError "Este directorio no es un repo git (no se encontro .git)." }
$branch = $branch.Trim()
if ($branch -ne "test") {
  Write-Host "Estas en la rama $branch. Cambiando a 'test'..."
  git checkout test | Out-Null
}

# Traer ultimos cambios
git pull --ff-only origin test | Out-Null

# Calcular nuevo tag de test vX.Y.Z-test
$tags = git tag --list "v*-test"
if (-not $tags) {
  $newTag = "v1.0.0-test"
} else {
  $versions = @()
  foreach ($t in $tags) {
    $clean = $t.TrimStart('v').TrimEnd('-test')
    try { $versions += [version]$clean } catch {}
  }
  if ($versions.Count -eq 0) { $newTag = "v1.0.0-test" }
  else {
    $last = ($versions | Sort-Object | Select-Object -Last 1)
    $newTag = "v{0}.{1}.{2}-test" -f $last.Major, $last.Minor, ($last.Build + 1)
  }
}
Write-Host "Nuevo tag: $newTag"

# Build debug
$gradlew = Join-Path $scriptDir "gradlew.bat"
if (-not (Test-Path $gradlew)) { Stop-OnError "No se encontro gradlew.bat en $scriptDir" }

Write-Host "Compilando APK de debug..."
& $gradlew assembleDebug
if ($LASTEXITCODE -ne 0) { Stop-OnError "Fallo el build assembleDebug." }

$apkPath = Join-Path $scriptDir "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apkPath)) { Stop-OnError "No se encontro el APK en $apkPath." }

# Release en GitHub (prerelease) con --repo
Write-Host "Publicando release en GitHub..."
$args = @('--repo', $ghRepo, $newTag, $apkPath, '--title', $newTag, '--notes', $Notes, '--target', 'test', '--prerelease')
gh release create @args
if ($LASTEXITCODE -ne 0) { Stop-OnError "No se pudo crear la release en GitHub." }

Write-Host "Release $newTag publicada con exito."
Pop-Location
