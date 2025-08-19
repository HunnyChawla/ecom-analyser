# EcomAnalyser Project Runner Script for PowerShell
# This script provides a single-command solution to run the entire EcomAnalyser project
# Author: EcomAnalyser Team
# Version: 1.0

param(
    [Parameter(Position=0)]
    [ValidateSet("start", "stop", "restart", "status", "help")]
    [string]$Action = "start"
)

# Set error action preference
$ErrorActionPreference = "Stop"

# Colors for output
$Red = "Red"
$Green = "Green"
$Yellow = "Yellow"
$Blue = "Blue"
$Default = "White"

# Function to print colored output
function Write-Status {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor $Blue
}

function Write-Success {
    param([string]$Message)
    Write-Host "[SUCCESS] $Message" -ForegroundColor $Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[WARNING] $Message" -ForegroundColor $Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor $Red
}

# Function to check if command exists
function Test-Command {
    param([string]$Command)
    try {
        Get-Command $Command -ErrorAction Stop | Out-Null
        return $true
    }
    catch {
        return $false
    }
}

# Function to check system requirements
function Test-Requirements {
    Write-Status "Checking system requirements..."
    
    # Check if Docker is installed
    if (-not (Test-Command "docker")) {
        Write-Error "Docker is not installed. Please install Docker first."
        Write-Status "Visit: https://docs.docker.com/get-docker/"
        exit 1
    }
    
    # Check if Docker is running
    try {
        docker info | Out-Null
    }
    catch {
        Write-Error "Docker is not running. Please start Docker first."
        exit 1
    }
    
    # Check if Docker Compose is available
    if (-not (Test-Command "docker-compose")) {
        try {
            docker compose version | Out-Null
        }
        catch {
            Write-Error "Docker Compose is not available. Please install Docker Compose first."
            exit 1
        }
    }
    

    
    Write-Success "All system requirements are met!"
}

# Function to setup project
function Setup-Project {
    Write-Status "Setting up EcomAnalyser project..."
    
    # Get the directory where this script is located
    $ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
    Set-Location $ScriptDir
    
    # Check if we're in the right directory
    if (-not (Test-Path "docker-compose.yml") -or -not (Test-Path "backend") -or -not (Test-Path "frontend")) {
        Write-Error "This script must be run from the EcomAnalyser project root directory."
        exit 1
    }
    
    Write-Success "Project directory confirmed: $ScriptDir"
}

# Function to start Docker services
function Start-DockerServices {
    Write-Status "Starting Docker services (database and backend)..."
    
    # Stop any existing containers
    try {
        $running = docker-compose ps | Select-String "Up"
        if ($running) {
            Write-Status "Stopping existing containers..."
            docker-compose down
        }
    }
    catch {
        # No containers running
    }
    
    # Start services
    docker-compose up -d
    
    # Wait for services to be ready
    Write-Status "Waiting for services to be ready..."
    Start-Sleep -Seconds 10
    
    # Check if services are running
    try {
        $running = docker-compose ps | Select-String "Up"
        if ($running) {
            Write-Success "Docker services started successfully!"
        } else {
            Write-Error "Failed to start Docker services. Check logs with: docker-compose logs"
            exit 1
        }
    }
    catch {
        Write-Error "Failed to start Docker services. Check logs with: docker-compose logs"
        exit 1
    }
}

# Function to build and start backend
function Start-Backend {
    Write-Status "Building and starting backend..."
    
    Set-Location backend
    
    # Clean and compile
    Write-Status "Compiling backend..."
    mvn clean compile
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Backend compilation failed!"
        exit 1
    }
    Write-Success "Backend compiled successfully!"
    
    # Check if backend is already running
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 5
        if ($response.StatusCode -eq 200) {
            Write-Success "Backend is already running on port 8080"
        }
    }
    catch {
        Write-Status "Starting backend service..."
        # Start backend in background
        Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run" -WorkingDirectory (Get-Location) -WindowStyle Hidden -RedirectStandardOutput "../backend.log" -RedirectStandardError "../backend.log"
        
        # Wait for backend to be ready
        Write-Status "Waiting for backend to be ready..."
        for ($i = 1; $i -le 30; $i++) {
            try {
                $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 5
                if ($response.StatusCode -eq 200) {
                    Write-Success "Backend is running on port 8080"
                    break
                }
            }
            catch {
                # Backend not ready yet
            }
            
            if ($i -eq 30) {
                Write-Error "Backend failed to start within 30 seconds"
                exit 1
            }
            Start-Sleep -Seconds 1
        }
    }
    
    Set-Location ..
}

# Function to start frontend
function Start-Frontend {
    Write-Status "Starting frontend..."
    
    Set-Location frontend
    
    # Check if node_modules exists, if not install dependencies
    if (-not (Test-Path "node_modules")) {
        Write-Status "Installing frontend dependencies..."
        npm install
    }
    
    # Check if frontend is already running
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:5173" -UseBasicParsing -TimeoutSec 5
        if ($response.StatusCode -eq 200) {
            Write-Success "Frontend is already running on port 5173"
        }
    }
    catch {
        Write-Status "Starting frontend development server..."
        # Start frontend in background
        Start-Process -FilePath "npm" -ArgumentList "run", "dev" -WorkingDirectory (Get-Location) -WindowStyle Hidden -RedirectStandardOutput "../frontend.log" -RedirectStandardError "../frontend.log"
        
        # Wait for frontend to be ready
        Write-Status "Waiting for frontend to be ready..."
        for ($i = 1; $i -le 15; $i++) {
            try {
                $response = Invoke-WebRequest -Uri "http://localhost:5173" -UseBasicParsing -TimeoutSec 5
                if ($response.StatusCode -eq 200) {
                    Write-Success "Frontend is running on port 5173"
                    break
                }
            }
            catch {
                # Frontend not ready yet
            }
            
            if ($i -eq 15) {
                Write-Error "Frontend failed to start within 15 seconds"
                exit 1
            }
            Start-Sleep -Seconds 1
        }
    }
    
    Set-Location ..
}

# Function to display status
function Show-Status {
    Write-Status "Checking service status..."
    
    Write-Host ""
    Write-Host "=========================================="
    Write-Host "           ECOMANALYSER STATUS"
    Write-Host "=========================================="
    
    # Docker services
    try {
        $running = docker-compose ps | Select-String "Up"
        if ($running) {
            Write-Host "🐳 Docker Services: RUNNING" -ForegroundColor $Green
            docker-compose ps
        } else {
            Write-Host "🐳 Docker Services: STOPPED" -ForegroundColor $Red
        }
    }
    catch {
        Write-Host "🐳 Docker Services: STOPPED" -ForegroundColor $Red
    }
    
    Write-Host ""
    
    # Backend
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 5
        if ($response.StatusCode -eq 200) {
            Write-Host "⚙️  Backend API: RUNNING (http://localhost:8080)" -ForegroundColor $Green
        } else {
            Write-Host "⚙️  Backend API: STOPPED" -ForegroundColor $Red
        }
    }
    catch {
        Write-Host "⚙️  Backend API: STOPPED" -ForegroundColor $Red
    }
    
    # Frontend
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:5173" -UseBasicParsing -TimeoutSec 5
        if ($response.StatusCode -eq 200) {
            Write-Host "🌐 Frontend: RUNNING (http://localhost:5173)" -ForegroundColor $Green
        } else {
            Write-Host "🌐 Frontend: STOPPED" -ForegroundColor $Red
        }
    }
    catch {
        Write-Host "🌐 Frontend: STOPPED" -ForegroundColor $Red
    }
    
    Write-Host ""
    Write-Host "=========================================="
    Write-Host ""
}

# Function to display help
function Show-Help {
    Write-Host "EcomAnalyser Project Runner Script for PowerShell"
    Write-Host ""
    Write-Host "Usage: .\run-ecomanalyser.ps1 [OPTION]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  start     Start all services (default)"
    Write-Host "  stop      Stop all services"
    Write-Host "  restart   Restart all services"
    Write-Host "  status    Show service status"
    Write-Host "  help      Show this help message"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  .\run-ecomanalyser.ps1              # Start all services"
    Write-Host "  .\run-ecomanalyser.ps1 start        # Start all services"
    Write-Host "  .\run-ecomanalyser.ps1 stop         # Stop all services"
    Write-Host "  .\run-ecomanalyser.ps1 status       # Show status"
    Write-Host ""
}

# Function to stop services
function Stop-Services {
    Write-Status "Stopping all services..."
    
    # Stop Docker services
    try {
        $running = docker-compose ps | Select-String "Up"
        if ($running) {
            Write-Status "Stopping Docker services..."
            docker-compose down
        }
    }
    catch {
        # No services running
    }
    
    Write-Success "All services stopped!"
}

# Main execution
switch ($Action) {
    "start" {
        Write-Status "Starting EcomAnalyser project..."
        Test-Requirements
        Setup-Project
        Start-DockerServices
        Start-Backend
        Start-Frontend
        Show-Status
        Write-Success "EcomAnalyser is now running!"
        Write-Status "Access the application at: http://localhost:5173"
        Write-Status "Backend API at: http://localhost:8080"
        Write-Status "Use '.\run-ecomanalyser.ps1 status' to check service status"
        Write-Status "Use '.\run-ecomanalyser.ps1 stop' to stop all services"
    }
    "stop" {
        Stop-Services
    }
    "restart" {
        Write-Status "Restarting EcomAnalyser project..."
        Stop-Services
        Start-Sleep -Seconds 2
        & $MyInvocation.MyCommand.Path "start"
    }
    "status" {
        Setup-Project
        Show-Status
    }
    "help" {
        Show-Help
    }
    default {
        Write-Error "Unknown action: $Action"
        Show-Help
        exit 1
    }
}
