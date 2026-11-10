$ErrorActionPreference = "Stop"

Write-Host "Starting a temporary Cloudflare Quick Tunnel for http://localhost:8081"
Write-Host "CINEORA will detect the new trycloudflare.com URL automatically."
Write-Host "Keep this terminal open while reviewing the application."
cloudflared tunnel --metrics 127.0.0.1:20241 --url http://localhost:8081
