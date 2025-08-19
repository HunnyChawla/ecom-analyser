# EcomAnalyser Containerization Guide

This document explains the fully containerized setup of the EcomAnalyser project, including both development and production environments.

## 🐳 **Architecture Overview**

The EcomAnalyser project is now **fully containerized** with three main services:

### **Services**
1. **Database** - PostgreSQL 16 (Alpine)
2. **Backend** - Spring Boot application
3. **Frontend** - React application with Nginx

### **Environments**
- **Development** - Hot reloading, volume mounts, development tools
- **Production** - Optimized builds, Nginx serving, production-ready

## 🚀 **Quick Start**

### **Development Environment (Default)**
```bash
# Start all services with hot reloading
./run-ecomanalyser.sh

# Or manually
docker-compose up -d
```

### **Production Environment**
```bash
# Switch to production
./switch-env.sh prod

# Or manually
docker-compose -f docker-compose.prod.yml up -d
```

## 📁 **File Structure**

```
EcomAnalyser/
├── docker-compose.yml              # Development environment
├── docker-compose.prod.yml         # Production environment
├── run-ecomanalyser.sh            # Main runner script
├── switch-env.sh                  # Environment switcher
├── backend/
│   └── Dockerfile                 # Backend container
├── frontend/
│   ├── Dockerfile                 # Production frontend
│   ├── Dockerfile.dev             # Development frontend
│   ├── nginx.conf                 # Nginx configuration
│   └── .dockerignore              # Docker build optimization
└── README files...
```

## 🔧 **Service Details**

### **1. Database Service**
- **Image**: `postgres:16-alpine`
- **Port**: 5432
- **Volume**: Persistent data storage
- **Environment**: Database credentials

### **2. Backend Service**
- **Build**: Custom Spring Boot application
- **Port**: 8080
- **Dependencies**: Database service
- **Features**: JPA, REST APIs, Batch processing

### **3. Frontend Service**

#### **Development Mode**
- **Build**: `Dockerfile.dev`
- **Port**: 5173
- **Features**: Hot reloading, volume mounts
- **Runtime**: Node.js with Vite

#### **Production Mode**
- **Build**: `Dockerfile` (multi-stage)
- **Port**: 80
- **Features**: Nginx serving, optimized assets
- **Runtime**: Nginx Alpine

## 🌐 **Access URLs**

### **Development Environment**
- **Frontend**: http://localhost:5173 (with hot reload)
- **Backend API**: http://localhost:8080
- **Database**: localhost:5432

### **Production Environment**
- **Frontend**: http://localhost (port 80)
- **Backend API**: http://localhost:8080
- **Database**: localhost:5432

## 🛠️ **Development Workflow**

### **Starting Development**
```bash
# Start all services
./run-ecomanalyser.sh

# Check status
./run-ecomanalyser.sh status

# View logs
./run-ecomanalyser.sh logs
```

### **Code Changes**
- **Frontend**: Changes automatically reload (hot reload)
- **Backend**: Requires container restart for changes
- **Database**: Persistent across restarts

### **Stopping Services**
```bash
# Stop all services
./run-ecomanalyser.sh stop

# Or manually
docker-compose down
```

## 🚀 **Production Deployment**

### **Switching to Production**
```bash
# Switch to production environment
./switch-env.sh prod

# Check production status
./switch-env.sh status
```

### **Production Features**
- **Optimized builds** - Minified and compressed assets
- **Nginx serving** - Fast static file serving
- **Security headers** - XSS protection, content security policy
- **Gzip compression** - Reduced bandwidth usage
- **Caching** - Long-term asset caching

## 🔍 **Environment Management**

### **Environment Switcher Script**
```bash
# Show help
./switch-env.sh help

# Switch to development
./switch-env.sh dev

# Switch to production
./switch-env.sh prod

# Show current status
./switch-env.sh status
```

### **Manual Environment Control**
```bash
# Development
docker-compose up -d

# Production
docker-compose -f docker-compose.prod.yml up -d

# Stop current environment
docker-compose down
```

## 📊 **Monitoring and Logs**

### **Service Logs**
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs frontend
docker-compose logs backend
docker-compose logs db

# Production logs
docker-compose -f docker-compose.prod.yml logs -f
```

### **Service Status**
```bash
# Check running containers
docker-compose ps

# Check resource usage
docker stats

# Health checks
curl http://localhost:5173/health  # Frontend
curl http://localhost:8080/actuator/health  # Backend
```

## 🔧 **Customization**

### **Frontend Configuration**
- **Development**: Edit `frontend/Dockerfile.dev`
- **Production**: Edit `frontend/Dockerfile` and `frontend/nginx.conf`
- **Environment variables**: Set in docker-compose files

### **Backend Configuration**
- **Environment variables**: Set in docker-compose files
- **Build context**: Modify `backend/Dockerfile`
- **Dependencies**: Update `backend/pom.xml`

### **Database Configuration**
- **Version**: Change PostgreSQL version in docker-compose
- **Credentials**: Modify environment variables
- **Ports**: Adjust port mappings

## 🚨 **Troubleshooting**

### **Common Issues**

#### **Port Already in Use**
```bash
# Check what's using the ports
lsof -i :5173  # Frontend dev
lsof -i :80    # Frontend prod
lsof -i :8080  # Backend
lsof -i :5432  # Database

# Stop conflicting services
sudo lsof -ti:5173 | xargs kill -9
```

#### **Container Build Failures**
```bash
# Clean Docker cache
docker system prune -a

# Rebuild without cache
docker-compose build --no-cache

# Check build logs
docker-compose build frontend
```

#### **Service Not Starting**
```bash
# Check container logs
docker-compose logs frontend
docker-compose logs backend

# Check container status
docker-compose ps

# Restart specific service
docker-compose restart frontend
```

### **Debug Commands**
```bash
# Enter running container
docker-compose exec frontend sh
docker-compose exec backend sh

# Check container resources
docker stats

# Inspect container
docker inspect ecomanalyser_frontend
```

## 🔒 **Security Considerations**

### **Development Environment**
- **Volume mounts** - Source code accessible from host
- **Hot reloading** - File watching enabled
- **Debug ports** - Development tools accessible

### **Production Environment**
- **Read-only containers** - No source code access
- **Security headers** - XSS protection, content security
- **Minimal attack surface** - Only necessary services exposed
- **Restart policies** - Automatic recovery from failures

## 📈 **Performance Optimization**

### **Development Optimizations**
- **Volume mounts** - Fast file access
- **Hot reloading** - Instant code changes
- **Development tools** - Full debugging capabilities

### **Production Optimizations**
- **Multi-stage builds** - Smaller final images
- **Asset compression** - Gzip and minification
- **Caching strategies** - Long-term asset caching
- **Nginx optimization** - Fast static file serving

## 🔄 **Migration from Local Development**

### **Before (Local Setup)**
- Java 17+ installed locally
- Node.js 16+ installed locally
- Maven and npm available
- Port conflicts possible
- Environment differences

### **After (Containerized)**
- Only Docker required locally
- Consistent environment everywhere
- No local tool installation
- Isolated port usage
- Reproducible builds

## 🎯 **Best Practices**

1. **Use environment switcher** for different deployments
2. **Check logs** when troubleshooting
3. **Use production compose** for staging/production
4. **Monitor resource usage** with `docker stats`
5. **Regular cleanup** with `docker system prune`
6. **Version control** Docker files with application code
7. **Health checks** for service monitoring
8. **Backup volumes** for database persistence

---

## 🎉 **Benefits of Containerization**

✅ **Consistency** - Same environment everywhere  
✅ **Simplicity** - Only Docker needed locally  
✅ **Isolation** - No conflicts with local tools  
✅ **Scalability** - Easy to deploy to multiple environments  
✅ **Reproducibility** - Identical builds every time  
✅ **Maintenance** - Centralized dependency management  
✅ **Security** - Isolated service boundaries  
✅ **Performance** - Optimized for each environment  

---

**Happy Containerizing! 🐳🚀**
