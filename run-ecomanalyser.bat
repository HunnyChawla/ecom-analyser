@echo off
REM EcomAnalyser Project Runner Script for Windows
REM This script provides a single-command solution to run the entire EcomAnalyser project
REM Author: EcomAnalyser Team
REM Version: 1.0

setlocal enabledelayedexpansion

REM Set colors for output
set "RED=[91m"
set "GREEN=[92m"
set "YELLOW=[93m"
set "BLUE=[94m"
set "NC=[0m"

REM Function to print colored output
:print_status
echo %BLUE%[INFO]%NC% %~1
goto :eof

:print_success
echo %GREEN%[SUCCESS]%NC% %~1
goto :eof

:print_warning
echo %YELLOW%[WARNING]%NC% %~1
goto :eof

:print_error
echo %RED%[ERROR]%NC% %~1
goto :eof

REM Function to check if command exists
:command_exists
where %~1 >nul 2>&1
if %errorlevel% equ 0 (
    set "exists=true"
) else (
    set "exists=false"
)
goto :eof

REM Function to check system requirements
:check_requirements
call :print_status "Checking system requirements..."

REM Check if Docker is installed
call :command_exists docker
if "!exists!"=="false" (
    call :print_error "Docker is not installed. Please install Docker first."
    call :print_status "Visit: https://docs.docker.com/get-docker/"
    exit /b 1
)

REM Check if Docker is running
docker info >nul 2>&1
if %errorlevel% neq 0 (
    call :print_error "Docker is not running. Please start Docker first."
    exit /b 1
)

REM Check if Docker Compose is available
call :command_exists docker-compose
if "!exists!"=="false" (
    docker compose version >nul 2>&1
    if %errorlevel% neq 0 (
        call :print_error "Docker Compose is not available. Please install Docker Compose first."
        exit /b 1
    )
)



call :print_success "All system requirements are met!"
goto :eof

REM Function to setup project
:setup_project
call :print_status "Setting up EcomAnalyser project..."

REM Get the directory where this script is located
set "SCRIPT_DIR=%~dp0"
cd /d "!SCRIPT_DIR!"

REM Check if we're in the right directory
if not exist "docker-compose.yml" (
    call :print_error "This script must be run from the EcomAnalyser project root directory."
    exit /b 1
)
if not exist "backend" (
    call :print_error "This script must be run from the EcomAnalyser project root directory."
    exit /b 1
)
if not exist "frontend" (
    call :print_error "This script must be run from the EcomAnalyser project root directory."
    exit /b 1
)

call :print_success "Project directory confirmed: !SCRIPT_DIR!"
goto :eof

REM Function to start Docker services
:start_docker_services
call :print_status "Starting Docker services (database and backend)..."

REM Stop any existing containers
docker-compose ps | findstr "Up" >nul 2>&1
if %errorlevel% equ 0 (
    call :print_status "Stopping existing containers..."
    docker-compose down
)

REM Start services
docker-compose up -d

REM Wait for services to be ready
call :print_status "Waiting for services to be ready..."
timeout /t 10 /nobreak >nul

REM Check if services are running
docker-compose ps | findstr "Up" >nul 2>&1
if %errorlevel% equ 0 (
    call :print_success "Docker services started successfully!"
) else (
    call :print_error "Failed to start Docker services. Check logs with: docker-compose logs"
    exit /b 1
)
goto :eof

REM Function to build and start backend
:start_backend
call :print_status "Building and starting backend..."

cd backend

REM Clean and compile
call :print_status "Compiling backend..."
mvn clean compile
if %errorlevel% neq 0 (
    call :print_error "Backend compilation failed!"
    exit /b 1
)
call :print_success "Backend compiled successfully!"

REM Check if backend is already running
curl -s http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    call :print_success "Backend is already running on port 8080"
) else (
    call :print_status "Starting backend service..."
    REM Start backend in background
    start /b mvn spring-boot:run > ..\backend.log 2>&1
    
    REM Wait for backend to be ready
    call :print_status "Waiting for backend to be ready..."
    for /l %%i in (1,1,30) do (
        curl -s http://localhost:8080/actuator/health >nul 2>&1
        if !errorlevel! equ 0 (
            call :print_success "Backend is running on port 8080"
            goto :backend_ready
        )
        timeout /t 1 /nobreak >nul
    )
    call :print_error "Backend failed to start within 30 seconds"
    exit /b 1
)

:backend_ready
cd ..
goto :eof

REM Function to start frontend
:start_frontend
call :print_status "Starting frontend..."

cd frontend

REM Check if node_modules exists, if not install dependencies
if not exist "node_modules" (
    call :print_status "Installing frontend dependencies..."
    npm install
)

REM Check if frontend is already running
curl -s http://localhost:5173 >nul 2>&1
if %errorlevel% equ 0 (
    call :print_success "Frontend is already running on port 5173"
) else (
    call :print_status "Starting frontend development server..."
    REM Start frontend in background
    start /b npm run dev > ..\frontend.log 2>&1
    
    REM Wait for frontend to be ready
    call :print_status "Waiting for frontend to be ready..."
    for /l %%i in (1,1,15) do (
        curl -s http://localhost:5173 >nul 2>&1
        if !errorlevel! equ 0 (
            call :print_success "Frontend is running on port 5173"
            goto :frontend_ready
        )
        timeout /t 1 /nobreak >nul
    )
    call :print_error "Frontend failed to start within 15 seconds"
    exit /b 1
)

:frontend_ready
cd ..
goto :eof

REM Function to display status
:show_status
call :print_status "Checking service status..."

echo.
echo ==========================================
echo            ECOMANALYSER STATUS
echo ==========================================

REM Docker services
docker-compose ps | findstr "Up" >nul 2>&1
if %errorlevel% equ 0 (
    echo 🐳 Docker Services: %GREEN%RUNNING%NC%
    docker-compose ps
) else (
    echo 🐳 Docker Services: %RED%STOPPED%NC%
)

echo.

REM Backend
curl -s http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ⚙️  Backend API: %GREEN%RUNNING%NC% (http://localhost:8080)
) else (
    echo ⚙️  Backend API: %RED%STOPPED%NC%
)

REM Frontend
curl -s http://localhost:5173 >nul 2>&1
if %errorlevel% equ 0 (
    echo 🌐 Frontend: %GREEN%RUNNING%NC% (http://localhost:5173)
) else (
    echo 🌐 Frontend: %RED%STOPPED%NC%
)

echo.
echo ==========================================
echo.
goto :eof

REM Function to display help
:show_help
echo EcomAnalyser Project Runner Script for Windows
echo.
echo Usage: %~nx0 [OPTION]
echo.
echo Options:
echo   start     Start all services (default)
echo   stop      Stop all services
echo   restart   Restart all services
echo   status    Show service status
echo   help      Show this help message
echo.
echo Examples:
echo   %~nx0              # Start all services
echo   %~nx0 start        # Start all services
echo   %~nx0 stop         # Stop all services
echo   %~nx0 status       # Show status
echo.
goto :eof

REM Function to stop services
:stop_services
call :print_status "Stopping all services..."

REM Stop Docker services
docker-compose ps | findstr "Up" >nul 2>&1
if %errorlevel% equ 0 (
    call :print_status "Stopping Docker services..."
    docker-compose down
)

call :print_success "All services stopped!"
goto :eof

REM Main execution
if "%1"=="" goto :start
if "%1"=="start" goto :start
if "%1"=="stop" goto :stop
if "%1"=="restart" goto :restart
if "%1"=="status" goto :status
if "%1"=="help" goto :show_help
if "%1"=="-h" goto :show_help
if "%1"=="--help" goto :show_help

echo %RED%[ERROR]%NC% Unknown option: %1
call :show_help
exit /b 1

:start
call :print_status "Starting EcomAnalyser project..."
call :check_requirements
if %errorlevel% neq 0 exit /b 1
call :setup_project
if %errorlevel% neq 0 exit /b 1
call :start_docker_services
if %errorlevel% neq 0 exit /b 1
call :start_backend
if %errorlevel% neq 0 exit /b 1
call :start_frontend
if %errorlevel% neq 0 exit /b 1
call :show_status
call :print_success "EcomAnalyser is now running!"
call :print_status "Access the application at: http://localhost:5173"
call :print_status "Backend API at: http://localhost:8080"
call :print_status "Use '%~nx0 status' to check service status"
call :print_status "Use '%~nx0 stop' to stop all services"
goto :eof

:stop
call :stop_services
goto :eof

:restart
call :print_status "Restarting EcomAnalyser project..."
call :stop_services
timeout /t 2 /nobreak >nul
call %~nx0 start
goto :eof

:status
call :setup_project
if %errorlevel% neq 0 exit /b 1
call :show_status
goto :eof
