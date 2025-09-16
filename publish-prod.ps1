# publish-prod.ps1
# ---------------------------
# Compila y publica PRODUCCION. Autodetecta .aab/.apk (firmado/unsigned) y pasa --repo.
param(
  [string]$Notes = "Release de produccion generada automaticamente",
  [switch]$AllowUnsigned
)

# Trabajar desde el directorio del script
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $scriptDir

function Stop-OnError($msg) { Write-Error $msg; Pop-Location; exit 1 }

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

# Rama prod
$branch = (git rev-parse --abbrev-ref HEAD) 2>$null
if (-not $branch) { Stop-OnError "Este directorio no es un repo git (no se encontro .git)." }
$branch = $branch.Trim()
if ($branch -ne "prod") { Write-Host "Estas en $branch. Cambiando a 'prod'..."; git checkout prod | Out-Null }
git pull --ff-only origin prod | Out-Null

# Tag vX.Y.Z (sin -test)
$lastTag = (git tag --list "v*" | Where-Object { $_ -notlike "*-test" } | Sort-Object { $_ -as [version] } | Select-Object -Last 1)
if (-not $lastTag) { $newTag = "v1.0.0" }
else {
  $v = $lastTag.TrimStart("v").Split(".")
  if ($v.Count -lt 3) { Stop-OnError "El tag $lastTag no sigue el formato vX.Y.Z" }
  $newTag = "v{0}.{1}.{2}" -f $v[0], $v[1], ([int]$v[2] + 1)
}
Write-Host "Nuevo tag: $newTag"

# Build release (APK y AAB)
$gradlew = Join-Path $scriptDir "gradlew.bat"
if (-not (Test-Path $gradlew)) { Stop-OnError "No se encontro gradlew.bat en $scriptDir" }

Write-Host "Compilando release..."
& $gradlew clean assembleRelease bundleRelease
if ($LASTEXITCODE -ne 0) { Stop-OnError "Fallo el build release. Revisa signingConfigs en app/build.gradle." }

# Detectar artefacto (.aab preferido)
$bundle = Get-ChildItem -Path (Join-Path $scriptDir "app\build\outputs\bundle\release") -Filter *.aab -ErrorAction SilentlyContinue
$apkSigned = Get-ChildItem -Path (Join-Path $scriptDir "app\build\outputs\apk") -Recurse -ErrorAction SilentlyContinue `
  | Where-Object { $_.FullName -match "\\release\\" -and $_.Name -match "release.*\.apk$" -and $_.Name -notmatch "unsigned|unaligned" } `
  | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$apkUnsigned = Get-ChildItem -Path (Join-Path $scriptDir "app\build\outputs\apk") -Recurse -ErrorAction SilentlyContinue `
  | Where-Object { $_.FullName -match "\\release\\" -and $_.Name -match "unsigned.*\.apk$" } `
  | Sort-Object LastWriteTime -Descending | Select-Object -First 1

$artifact = $null
if ($bundle) {
  $artifact = $bundle | Sort-Object LastWriteTime -Descending | Select-Object -First 1
  Write-Host ("Artefacto seleccionado (AAB): " + $artifact.FullName)
} elseif ($apkSigned) {
  $artifact = $apkSigned
  Write-Host ("Artefacto seleccionado (APK firmado): " + $artifact.FullName)
} elseif ($apkUnsigned) {
  if (-not $AllowUnsigned) {
    Stop-OnError ("Se encontro solo APK UNSIGNED: {0}. Configura signingConfigs.release o ejecuta con -AllowUnsigned para subirlo igual." -f $apkUnsigned.FullName)
  }
  $artifact = $apkUnsigned
  Write-Host ("Artefacto seleccionado (APK unsigned): " + $artifact.FullName)
} else {
  Stop-OnError "No se encontro ningun artefacto en outputs (ni .aab ni .apk)."
}

# Release en GitHub con --repo
Write-Host "Publicando release en GitHub..."
$args = @('--repo', $ghRepo, $newTag, $artifact.FullName, '--title', $newTag, '--notes', $Notes, '--target', 'prod')
gh release create @args
if ($LASTEXITCODE -ne 0) { Stop-OnError "No se pudo crear la release en GitHub." }

Write-Host "Release $newTag publicada con exito."
Pop-Location
