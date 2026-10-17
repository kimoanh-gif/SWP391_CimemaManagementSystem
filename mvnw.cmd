<# : batch portion
@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __MVNW_CMD__=
@SET __MVNW_PSMODULEP_SAVE=%PSModulePath%
@SET PSModulePath=
@FOR /F "usebackq tokens=1* delims==" %%A IN (`powershell -noprofile "& {$scriptDir='%~dp0'; $script='%__MVNW_ARG0_NAME__%'; icm -ScriptBlock ([Scriptblock]::Create((Get-Content -Raw '%~f0'))) -NoNewScope}"`) DO @(
  IF "%%A"=="MVN_CMD" (set __MVNW_CMD__=%%B) ELSE IF "%%B"=="" (echo %%A) ELSE (echo %%A=%%B)
)
@SET PSModulePath=%__MVNW_PSMODULEP_SAVE%
@SET __MVNW_PSMODULEP_SAVE=
@SET __MVNW_ARG0_NAME__=
@SET MVNW_USERNAME=
@SET MVNW_PASSWORD=
@IF NOT "%__MVNW_CMD__%"=="" ("%__MVNW_CMD__%" %*)
@IF ERRORLEVEL 1 exit /b %ERRORLEVEL%
@echo Cannot start Maven from wrapper >&2 && exit /b 1
@GOTO :EOF
: end batch / begin powershell #>

$ErrorActionPreference = "Stop"
if ($env:MVNW_VERBOSE -eq "true") {
  $VerbosePreference = "Continue"
}

$propertiesPath = Join-Path $scriptDir ".mvn/wrapper/maven-wrapper.properties"
$properties = Get-Content -Raw $propertiesPath | ConvertFrom-StringData
$distributionUrl = $properties.distributionUrl
if (!$distributionUrl) {
  Write-Error "cannot read distributionUrl property in $propertiesPath"
}

$distributionUrlName = $distributionUrl -replace '^.*/',''
$distributionUrlNameMain = $distributionUrlName -replace '\.[^.]*$','' -replace '-bin$',''
$MVN_CMD = $script -replace '^mvnw','mvn'

$MAVEN_M2_PATH = if ($env:MAVEN_USER_HOME) { $env:MAVEN_USER_HOME } else { "$HOME/.m2" }
New-Item -Path $MAVEN_M2_PATH -ItemType Directory -Force | Out-Null
$MAVEN_WRAPPER_DISTS = Join-Path $MAVEN_M2_PATH "wrapper/dists"
$MAVEN_HOME_PARENT = Join-Path $MAVEN_WRAPPER_DISTS $distributionUrlNameMain
$MAVEN_HOME_NAME = ([System.Security.Cryptography.SHA256]::Create().ComputeHash([byte[]][char[]]$distributionUrl) | ForEach-Object {$_.ToString("x2")}) -join ''
$MAVEN_HOME = Join-Path $MAVEN_HOME_PARENT $MAVEN_HOME_NAME

if (Test-Path -Path $MAVEN_HOME -PathType Container) {
  Write-Output "MVN_CMD=$MAVEN_HOME/bin/$MVN_CMD"
  exit 0
}

if (!$distributionUrlNameMain -or ($distributionUrlName -eq $distributionUrlNameMain)) {
  Write-Error "distributionUrl must end with *-bin.zip, but found $distributionUrl"
}

$TMP_DOWNLOAD_DIR_HOLDER = New-TemporaryFile
$TMP_DOWNLOAD_DIR = New-Item -ItemType Directory -Path "$TMP_DOWNLOAD_DIR_HOLDER.dir"
$TMP_DOWNLOAD_DIR_HOLDER.Delete() | Out-Null
trap {
  if ($TMP_DOWNLOAD_DIR.Exists) {
    try { Remove-Item $TMP_DOWNLOAD_DIR -Recurse -Force | Out-Null }
    catch { Write-Warning "Cannot remove $TMP_DOWNLOAD_DIR" }
  }
}

New-Item -ItemType Directory -Path $MAVEN_HOME_PARENT -Force | Out-Null
Write-Verbose "Downloading Maven from $distributionUrl"
$webclient = New-Object System.Net.WebClient
if ($env:MVNW_USERNAME -and $env:MVNW_PASSWORD) {
  $webclient.Credentials = New-Object System.Net.NetworkCredential($env:MVNW_USERNAME, $env:MVNW_PASSWORD)
}
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$archivePath = Join-Path $TMP_DOWNLOAD_DIR $distributionUrlName
$webclient.DownloadFile($distributionUrl, $archivePath)

$distributionSha256Sum = $properties.distributionSha256Sum
if ($distributionSha256Sum) {
  $actualHash = (Get-FileHash $archivePath -Algorithm SHA256).Hash.ToLower()
  if ($actualHash -ne $distributionSha256Sum) {
    Write-Error "Maven distribution SHA-256 validation failed."
  }
}

Expand-Archive $archivePath -DestinationPath $TMP_DOWNLOAD_DIR
$expectedPath = Join-Path $TMP_DOWNLOAD_DIR $distributionUrlNameMain
if (!(Test-Path -Path "$expectedPath/bin/$MVN_CMD" -PathType Leaf)) {
  Write-Error "Could not find Maven executable in downloaded archive."
}
Rename-Item -Path $expectedPath -NewName $MAVEN_HOME_NAME
Move-Item -Path (Join-Path $TMP_DOWNLOAD_DIR $MAVEN_HOME_NAME) -Destination $MAVEN_HOME_PARENT
Remove-Item $TMP_DOWNLOAD_DIR -Recurse -Force
Write-Output "MVN_CMD=$MAVEN_HOME/bin/$MVN_CMD"
