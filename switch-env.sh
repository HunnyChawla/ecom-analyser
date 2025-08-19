#!/bin/bash

# EcomAnalyser Environment Switcher
# Switch between development and production Docker Compose configurations

set -e

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

# Function to show help
show_help() {
    echo "EcomAnalyser Environment Switcher"
    echo
    echo "Usage: $0 [OPTION]"
    echo
    echo "Options:"
    echo "  dev         Switch to development environment (default)"
    echo "  prod        Switch to production environment"
    echo "  status      Show current environment status"
    echo "  help        Show this help message"
    echo
    echo "Examples:"
    echo "  $0              # Switch to development"
    echo "  $0 dev          # Switch to development"
    echo "  $0 prod         # Switch to production"
    echo "  $0 status       # Show current status"
    echo
}

# Function to check current environment
check_current_env() {
    if [ -f ".env.current" ]; then
        CURRENT_ENV=$(cat .env.current)
        echo "Current environment: $CURRENT_ENV"
    else
        echo "No environment file found. Defaulting to development."
    fi
}

# Function to switch to development
switch_to_dev() {
    print_status "Switching to development environment..."
    
    # Stop any running containers
    if docker-compose ps | grep -q "Up"; then
        print_status "Stopping existing containers..."
        docker-compose down
    fi
    
    # Create development environment file
    echo "dev" > .env.current
    
    # Start development services
    print_status "Starting development services..."
    docker-compose up -d
    
    print_success "Switched to development environment!"
    print_status "Frontend: http://localhost:5173 (with hot reload)"
    print_status "Backend: http://localhost:8080"
    print_status "Database: localhost:5432"
}

# Function to switch to production
switch_to_prod() {
    print_status "Switching to production environment..."
    
    # Stop any running containers
    if docker-compose ps | grep -q "Up"; then
        print_status "Stopping existing containers..."
        docker-compose down
    fi
    
    # Create production environment file
    echo "prod" > .env.current
    
    # Start production services
    print_status "Starting production services..."
    docker-compose -f docker-compose.prod.yml up -d
    
    print_success "Switched to production environment!"
    print_status "Frontend: http://localhost (port 80)"
    print_status "Backend: http://localhost:8080"
    print_status "Database: localhost:5432"
}

# Function to show status
show_status() {
    print_status "Environment Status:"
    echo
    
    if [ -f ".env.current" ]; then
        CURRENT_ENV=$(cat .env.current)
        echo "Current environment: $CURRENT_ENV"
    else
        echo "Current environment: development (default)"
    fi
    
    echo
    
    # Check running containers
    if docker-compose ps | grep -q "Up"; then
        echo "Running containers:"
        docker-compose ps
    else
        echo "No containers running"
    fi
    
    echo
    
    # Check ports
    echo "Port status:"
    if netstat -an 2>/dev/null | grep -q ":5173.*LISTEN"; then
        echo "Port 5173 (dev frontend): LISTENING"
    else
        echo "Port 5173 (dev frontend): NOT LISTENING"
    fi
    
    if netstat -an 2>/dev/null | grep -q ":80.*LISTEN"; then
        echo "Port 80 (prod frontend): LISTENING"
    else
        echo "Port 80 (prod frontend): NOT LISTENING"
    fi
    
    if netstat -an 2>/dev/null | grep -q ":8080.*LISTEN"; then
        echo "Port 8080 (backend): LISTENING"
    else
        echo "Port 8080 (backend): NOT LISTENING"
    fi
    
    if netstat -an 2>/dev/null | grep -q ":5432.*LISTEN"; then
        echo "Port 5432 (database): LISTENING"
    else
        echo "Port 5432 (database): LISTENING"
    fi
}

# Main execution
case "${1:-dev}" in
    "dev")
        switch_to_dev
        ;;
    "prod")
        switch_to_prod
        ;;
    "status")
        show_status
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
