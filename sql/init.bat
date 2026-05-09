@echo off
setlocal

set "PGPATH=C:\Program Files\PostgreSQL\18\bin"
set "PATH=%PGPATH%;%PATH%"

set PGHOST=localhost
set PGPORT=5432
set PGUSER=postgres

set /p PGPASSWORD=Please enter PostgreSQL password: 

echo.
echo Creating database weanalyzer...
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -c "CREATE DATABASE weanalyzer WITH ENCODING = 'UTF8';" 2>nul
if errorlevel 1 (
    echo Database may already exist, continuing...
)

echo.
echo Executing init script...
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d weanalyzer -f "E:\\微信公众号数据分析工具\\sql\\init.sql"

if errorlevel 1 (
    echo.
    echo Init failed.
    pause
    exit /b 1
) else (
    echo.
    echo Database initialized successfully!
)

endlocal
pause
