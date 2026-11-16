$ErrorActionPreference = "Stop"

# Keep the legacy command working, but delegate port discovery to the shared
# launcher instead of assuming that every developer runs Spring Boot on 8081.
& (Join-Path $PSScriptRoot "start-cineora-tunnel-auto.ps1")
