# Airplane QnA 로컬 실행 (Windows PowerShell 5.1+)
# 사용법:  powershell -ExecutionPolicy Bypass -File .\run-local.ps1
# 임베딩(8001) · 백엔드(8080) · 프론트엔드(3000)를 각각 새 PowerShell 창에서 띄웁니다.
$ErrorActionPreference = "Continue"  # PS 5.1: native stderr + Stop = 강제 종료되므로 Continue
$Root = $PSScriptRoot

function Has($cmd) { [bool](Get-Command $cmd -ErrorAction SilentlyContinue) }

# ---- 1) 필수 도구 확인 -------------------------------------------------
$missing = @()
if (-not (Has java)) { $missing += "Java 21 (JDK)" }
if (-not (Has mvn))  { $missing += "Maven (mvn)" }
if (-not (Has node)) { $missing += "Node.js 20+" }

# torch 2.8 은 Python 3.10~3.13 만 지원 → py 런처로 맞는 버전을 찾는다
$Py = $null
if (Has py) {
    foreach ($v in "3.11", "3.12", "3.13", "3.10") {
        & py -$v -c "import sys" 2>$null
        if ($LASTEXITCODE -eq 0) { $Py = @("py", "-$v"); break }
    }
}
if (-not $Py -and (Has python)) {
    $ver = & python -c "import sys; print('%d.%d' % sys.version_info[:2])" 2>$null
    if ($ver -in "3.10", "3.11", "3.12", "3.13") { $Py = @("python") }
}
if (-not $Py) { $missing += "Python 3.10~3.13 (3.11 권장, 설치 시 'Add python.exe to PATH' 체크)" }

if ($missing.Count -gt 0) {
    Write-Host "다음 도구가 없습니다. 설치 후 PowerShell 을 새로 열고 다시 실행하세요:" -ForegroundColor Red
    $missing | ForEach-Object { Write-Host "  - $_" }
    exit 1
}

# ---- 2) 최초 1회 설치 --------------------------------------------------
$Venv = Join-Path $Root "embedding-service\.venv"
if (-not (Test-Path "$Venv\Scripts\python.exe")) {
    Write-Host "[setup] Python 가상환경 생성 + 패키지 설치 (torch 포함, 수 분 소요)" -ForegroundColor Cyan
    $exe = $Py[0]; $args0 = @($Py | Select-Object -Skip 1)
    & $exe @args0 -m venv $Venv
    & "$Venv\Scripts\python.exe" -m pip install --upgrade pip
    & "$Venv\Scripts\python.exe" -m pip install -r (Join-Path $Root "embedding-service\requirements.txt")
    if ($LASTEXITCODE -ne 0) { Write-Host "pip install 실패" -ForegroundColor Red; exit 1 }
}
if (-not (Test-Path (Join-Path $Root "frontend\node_modules"))) {
    Write-Host "[setup] npm install" -ForegroundColor Cyan
    Push-Location (Join-Path $Root "frontend"); npm install; Pop-Location
}

# ---- 3) .env 로드 (ANTHROPIC_API_KEY, 없으면 추출형 모드) ---------------
$EnvFile = Join-Path $Root ".env"
$envCmd = ""
if (Test-Path $EnvFile) {
    foreach ($line in Get-Content $EnvFile) {
        if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
            $k = $Matches[1]; $v = $Matches[2].Trim().Trim('"').Trim("'")
            if ($v) { $envCmd += "`$env:$k='$v'; " }
        }
    }
}

# ---- 4) 서비스 3개 기동 ------------------------------------------------
function Launch($title, $dir, $cmd) {
    $full = "`$Host.UI.RawUI.WindowTitle='$title'; Set-Location '$dir'; $envCmd$cmd"
    Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $full
}
Launch "Embedding :8001" (Join-Path $Root "embedding-service") ".\.venv\Scripts\python.exe -m uvicorn app:app --host 127.0.0.1 --port 8001"
Launch "Backend :8080"   (Join-Path $Root "backend")           "mvn spring-boot:run"
Launch "Frontend :3000"  (Join-Path $Root "frontend")          "npm run dev"

Write-Host ""
Write-Host "Embedding -> http://127.0.0.1:8001/health (첫 기동 시 모델 다운로드 ~470MB)"
Write-Host "Backend   -> http://localhost:8080/api/organizations"
Write-Host "Frontend  -> http://localhost:3000" -ForegroundColor Green
Write-Host "종료: 열린 창 3개를 닫거나 각 창에서 Ctrl+C"
