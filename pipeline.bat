@echo off
setlocal enabledelayedexpansion

:: =========================
:: CONFIG
:: =========================
set LOG_DIR=logs
set LOG_FILE=%LOG_DIR%\pipeline.log

if not exist %LOG_DIR% mkdir %LOG_DIR%

:: =========================
:: MENU
:: =========================
:menu
echo.
echo ==============================
echo        PIPELINE JAVA
echo ==============================
echo 1 - Rodar tudo (full pipeline)
echo 2 - Só build
echo 3 - Só infra
echo 4 - Rodar API + Worker (paralelo)
echo 5 - Sair
echo ==============================
set /p option=Escolha:

if "%option%"=="1" goto full
if "%option%"=="2" goto build
if "%option%"=="3" goto infra
if "%option%"=="4" goto run_parallel
if "%option%"=="5" exit

goto menu

:: =========================
:: FULL PIPELINE
:: =========================
:full
call :log "==============================="
call :log "INICIANDO FULL PIPELINE"
call :log "==============================="

call :clean
if errorlevel 1 goto error

call :base_install
if errorlevel 1 goto error

call :shared
if errorlevel 1 goto error

call :infra
if errorlevel 1 goto error

call :run_parallel
goto end

:: =========================
:: BUILD ONLY
:: =========================
:build
call :log "BUILD ONLY"

call :clean
call :base_install
call :shared

goto end

:: =========================
:: INFRA ONLY
:: =========================
:infra
call :log "SUBINDO INFRA"

call scripts\dev-env.cmd >> %LOG_FILE% 2>&1
if errorlevel 1 goto error

goto end

:: =========================
:: PARALLEL RUN
:: =========================
:run_parallel
call :log "INICIANDO API + WORKER EM PARALELO"

start "API" cmd /c "mvnw.cmd -f api\pom.xml spring-boot:run >> logs\api.log 2>&1"
start "WORKER" cmd /c "mvnw.cmd -f worker\pom.xml spring-boot:run >> logs\worker.log 2>&1"

call :wait_api

goto end

:: =========================
:: WAIT API HEALTHCHECK
:: =========================
:wait_api
call :log "AGUARDANDO API SUBIR..."

set /a retries=0

:retry
curl -s http://localhost:8080/actuator/health >nul

if errorlevel 1 (
    set /a retries+=1
    if !retries! GEQ 20 (
        call :log "API não subiu a tempo"
        goto error
    )

    timeout /t 3 >nul
    goto retry
)

call :log "API ONLINE!"
goto :eof

:: =========================
:: CLEAN
:: =========================
:clean
call :step "CLEAN ROOT"
call mvnw.cmd clean >> %LOG_FILE% 2>&1
if errorlevel 1 exit /b 1
exit /b 0

:: =========================
:: BASE INSTALL
:: =========================
:base_install
call :step "BASE INSTALL (-N)"
call mvnw.cmd -N install >> %LOG_FILE% 2>&1
if errorlevel 1 exit /b 1
exit /b 0

:: =========================
:: SHARED MODULE
:: =========================
:shared
call :step "BUILD SHARED"
call mvnw.cmd -pl shared install >> %LOG_FILE% 2>&1
if errorlevel 1 exit /b 1
exit /b 0

:: =========================
:: LOG HELPERS
:: =========================
:log
echo [%date% %time%] %~1
echo [%date% %time%] %~1 >> %LOG_FILE%
exit /b

:step
echo.
call :log ">>> %~1"
exit /b

:: =========================
:: ERROR HANDLER
:: =========================
:error
call :log "PIPELINE FALHOU"
echo.
echo ❌ Erro na pipeline. Veja logs em %LOG_FILE%
pause
exit /b 1

:: =========================
:: END
:: =========================
:end
call :log "PIPELINE FINALIZADA COM SUCESSO"
echo.
echo ✅ Finalizado com sucesso
pause
goto menu