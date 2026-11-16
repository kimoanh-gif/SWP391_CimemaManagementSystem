<#
  CINEORA - Cloudflare Quick Tunnel that works on any team member's machine.

  Usage (from the project folder, while the app is running):
      powershell -ExecutionPolicy Bypass -File .\tools\start-cineora-tunnel-auto.ps1
      powershell -ExecutionPolicy Bypass -File .\tools\start-cineora-tunnel-auto.ps1 -Port 8081

  What it does:
    1. Finds cloudflared (PATH, or the copy this script downloads to %LOCALAPPDATA%\cineora-cloudflared).
       If none is found it downloads the official Windows build from Cloudflare's GitHub releases.
       No admin rights, winget or chocolatey needed, and PATH is not changed.
    2. Detects the port the app is listening on (configured server.port first, then 8080 / 8081), unless -Port is given.
    3. Starts the tunnel with metrics on 127.0.0.1:20241 - the address PublicUrlResolver reads -
       so ticket QR codes (Customer and Staff) automatically use the new trycloudflare.com URL.

  Keep this window open while testing. Press Ctrl+C to stop the tunnel (the public URL stops working).
#>
param(
    [int]$Port = 0
)

$ErrorActionPreference = "Stop"
$metrics = "127.0.0.1:20241"

function Test-PortListening([int]$p) {
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $async = $client.BeginConnect("127.0.0.1", $p, $null, $null)
        $ok = $async.AsyncWaitHandle.WaitOne(400) -and $client.Connected
        $client.Close()
        return $ok
    } catch { return $false }
}

# ---------- 1. cloudflared ----------
$cloudflared = $null
$cmd = Get-Command cloudflared -ErrorAction SilentlyContinue
if ($cmd) { $cloudflared = $cmd.Source }

$localDir = Join-Path $env:LOCALAPPDATA "cineora-cloudflared"
$localExe = Join-Path $localDir "cloudflared.exe"
if (-not $cloudflared -and (Test-Path $localExe)) { $cloudflared = $localExe }

if (-not $cloudflared) {
    Write-Host "cloudflared not found - downloading the official build from github.com/cloudflare/cloudflared ..."
    New-Item -ItemType Directory -Force -Path $localDir | Out-Null
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $arch = if ([Environment]::Is64BitOperatingSystem) { "amd64" } else { "386" }
    $url = "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-$arch.exe"
    Invoke-WebRequest -Uri $url -OutFile $localExe -UseBasicParsing
    $sig = Get-AuthenticodeSignature $localExe
    if ($sig.Status -ne "Valid" -or $sig.SignerCertificate.Subject -notmatch "Cloudflare") {
        Remove-Item $localExe -Force
        throw "Downloaded cloudflared is not signed by Cloudflare - aborted. Install it manually instead."
    }
    $cloudflared = $localExe
    Write-Host "Saved to $localExe"
}

# ---------- 2. application port ----------
if ($Port -le 0) {
    $configuredPort = 0
    $props = Join-Path $PSScriptRoot "..\src\main\resources\application.properties"
    if (Test-Path $props) {
        $line = Select-String -Path $props -Pattern '^\s*server\.port\s*=\s*\$?\{?[A-Z_]*:?(\d+)' | Select-Object -First 1
        if ($line) { $configuredPort = [int]$line.Matches[0].Groups[1].Value }
    }
    $candidates = @($configuredPort, 8080, 8081) | Where-Object { $_ -gt 0 } | Select-Object -Unique
    foreach ($candidate in $candidates) {
        if (Test-PortListening $candidate) { $Port = $candidate; break }
    }
    if ($Port -le 0 -and $configuredPort -gt 0) { $Port = $configuredPort }
}
if ($Port -le 0) { $Port = 8080 }

if (-not (Test-PortListening $Port)) {
    Write-Warning "Nothing is listening on http://localhost:$Port yet. Start the CINEORA app first (the tunnel will still start)."
}

# ---------- 3. tunnel ----------
Write-Host ""
Write-Host "Starting Cloudflare Quick Tunnel -> http://localhost:$Port"
Write-Host "CINEORA detects the trycloudflare.com URL through $metrics; ticket QR codes refresh within ~5 seconds."
Write-Host "Keep this window open while testing. Ctrl+C stops the tunnel."
Write-Host ""
& $cloudflared tunnel --metrics $metrics --url "http://localhost:$Port"
