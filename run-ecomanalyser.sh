#!/bin/bash

# EcomAnalyser Project Runner Script
# This script sets up and runs the entire EcomAnalyser project on any machine
# Author: EcomAnalyser Team
# Version: 1.0

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check system requirements
check_requirements() {
    print_status "Checking system requirements..."
    
    # Check if Docker is installed
    if ! command_exists docker; then
        print_error "Docker is not installed. Please install Docker first."
        print_status "Visit: https://docs.docker.com/get-docker/"
        exit 1
    fi
    
    # Check if Docker is running
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker first."
        exit 1
    fi
    
    # Check if Docker Compose is available
    if ! command_exists docker-compose && ! docker compose version >/dev/null 2>&1; then
        print_error "Docker Compose is not available. Please install Docker Compose first."
        exit 1
    fi
    
    print_success "All system requirements are met!"
}

# Function to setup project
setup_project() {
    print_status "Setting up EcomAnalyser project..."
    
    # Get the directory where this script is located
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    cd "$SCRIPT_DIR"
    
    # Check if we're in the right directory
    if [ ! -f "docker-compose.yml" ] || [ ! -d "backend" ] || [ ! -d "frontend" ]; then
        print_error "This script must be run from the EcomAnalyser project root directory."
        exit 1
    fi
    
    print_success "Project directory confirmed: $SCRIPT_DIR"
}

# Function to start Docker services
start_docker_services() {
    print_status "Starting all Docker services (database, backend, and frontend)..."
    
    # Stop any existing containers
    if docker-compose ps | grep -q "Up"; then
        print_status "Stopping existing containers..."
        docker-compose down
    fi
    
    # Start all services
    docker-compose up -d
    
    # Wait for services to be ready
    print_status "Waiting for services to be ready..."
    sleep 20
    
    # Check if services are running
    if docker-compose ps | grep -q "Up"; then
        print_success "Docker services started successfully!"
    else
        print_error "Failed to start Docker services. Check logs with: docker-compose logs"
        exit 1
    fi
}

# Function to wait for services to be ready
wait_for_services() {
    print_status "Waiting for all services to be ready..."
    
    # Wait for backend to be ready
    print_status "Waiting for backend to be ready..."
    for i in {1..60}; do
        if curl -s http://localhost:8080/actuator/health >/dev/null 2>&1; then
            print_success "Backend is running on port 8080"
            break
        fi
        if [ $i -eq 60 ]; then
            print_error "Backend failed to start within 60 seconds"
            exit 1
        fi
        sleep 1
    done
    
    # Wait for frontend to be ready
    print_status "Waiting for frontend to be ready..."
    for i in {1..30}; do
        if curl -s http://localhost:5173 >/dev/null 2>&1; then
            print_success "Frontend is running on port 5173"
            break
        fi
        if [ $i -eq 30 ]; then
            print_error "Frontend failed to start within 30 seconds"
            exit 1
        fi
        sleep 1
    done
}



# Function to display status
show_status() {
    print_status "Checking service status..."
    
    echo
    echo "=========================================="
    echo "           ECOMANALYSER STATUS"
    echo "=========================================="
    
    # Docker services
    if docker-compose ps | grep -q "Up"; then
        echo -e "🐳 Docker Services: ${GREEN}RUNNING${NC}"
        docker-compose ps
    else
        echo -e "🐳 Docker Services: ${RED}STOPPED${NC}"
    fi
    
    echo
    
    # Backend
    if curl -s http://localhost:8080/actuator/health >/dev/null 2>&1; then
        echo -e "⚙️  Backend API: ${GREEN}RUNNING${NC} (http://localhost:8080)"
    else
        echo -e "⚙️  Backend API: ${RED}STOPPED${NC}"
    fi
    
    # Frontend
    if curl -s http://localhost:5173 >/dev/null 2>&1; then
        echo -e "🌐 Frontend: ${GREEN}RUNNING${NC} (http://localhost:5173)"
    else
        echo -e "🌐 Frontend: ${RED}STOPPED${NC}"
    fi
    
    echo
    echo "=========================================="
    echo
}

# Function to display help
show_help() {
    echo "EcomAnalyser Project Runner Script"
    echo
    echo "Usage: $0 [OPTION]"
    echo
    echo "Options:"
    echo "  start     Start all services (default)"
    echo "  stop      Stop all services"
    echo "  restart   Restart all services"
    echo "  status    Show service status"
    echo "  logs      Show service logs"
    echo "  clean     Clean up all services and data"
    echo "  help      Show this help message"
    echo
    echo "Examples:"
    echo "  $0              # Start all services"
    echo "  $0 start        # Start all services"
    echo "  $0 stop         # Stop all services"
    echo "  $0 status       # Show status"
    echo
}

# Function to stop services
stop_services() {
    print_status "Stopping all services..."
    
    # Stop Docker services
    if docker-compose ps | grep -q "Up"; then
        print_status "Stopping Docker services..."
        docker-compose down
    fi
    
    print_success "All services stopped!"
}

# Function to show logs
show_logs() {
    print_status "Showing service logs..."
    
    echo
    echo "=========================================="
    echo "              SERVICE LOGS"
    echo "=========================================="
    
    if [ -f "backend.log" ]; then
        echo -e "${BLUE}Backend Logs:${NC}"
        tail -20 backend.log
        echo
    fi
    
    if [ -f "frontend.log" ]; then
        echo -e "${BLUE}Frontend Logs:${NC}"
        tail -20 frontend.log
        echo
    fi
    
    echo -e "${BLUE}Docker Logs:${NC}"
    docker-compose logs --tail=10
}

# Function to clean up
clean_up() {
    print_warning "This will remove all data and containers. Are you sure? (y/N)"
    read -r response
    if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        print_status "Cleaning up all services and data..."
        
        # Stop services
        stop_services
        
        # Remove Docker volumes
        print_status "Removing Docker volumes..."
        docker volume rm ecomanalyser_db_data 2>/dev/null || true
        
        print_success "Cleanup completed!"
    else
        print_status "Cleanup cancelled."
    fi
}

# Main execution
main() {
    case "${1:-start}" in
        "start")
            print_status "Starting EcomAnalyser project..."
            check_requirements
            setup_project
            start_docker_services
            wait_for_services
            show_status
            print_success "EcomAnalyser is now running!"
            print_status "Access the application at: http://localhost:5173"
            print_status "Backend API at: http://localhost:8080"
            print_status "Use '$0 status' to check service status"
            print_status "Use '$0 stop' to stop all services"
            ;;
        "stop")
            stop_services
            ;;
        "restart")
            print_status "Restarting EcomAnalyser project..."
            stop_services
            sleep 2
            exec "$0" start
            ;;
        "status")
            setup_project
            show_status
            ;;
        "logs")
            setup_project
            show_logs
            ;;
        "clean")
            setup_project
            clean_up
            ;;
        "help"|"-h"|"--help")
            show_help
            ;;
        *)
            print_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
}

# Trap to handle script interruption
trap 'print_status "Interrupted. Use $0 stop to stop all services."; exit 1' INT TERM

# Run main function
main "$@"
