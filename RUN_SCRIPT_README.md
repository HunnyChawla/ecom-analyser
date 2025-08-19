# EcomAnalyser Project Runner Script

This script provides a single-command solution to run the entire EcomAnalyser project on any machine. It automatically handles Docker setup, backend compilation, and frontend startup.

## 🚀 Quick Start

### Single Command to Run Everything
```bash
./run-ecomanalyser.sh
```

That's it! The script will:
1. ✅ Check system requirements
2. 🐳 Start Docker services (database + backend)
3. ⚙️ Compile and start the Spring Boot backend
4. 🌐 Start the React frontend
5. 📊 Display service status

## 📋 Prerequisites

Before running the script, ensure you have:

- **Docker** - [Install Docker](https://docs.docker.com/get-docker/)
- **Docker Compose** - Usually comes with Docker Desktop
- **curl** - For health checks (usually pre-installed on most systems)

## 🎯 Available Commands

### Start All Services (Default)
```bash
./run-ecomanalyser.sh start
# or simply
./run-ecomanalyser.sh
```

### Stop All Services
```bash
./run-ecomanalyser.sh stop
```

### Restart All Services
```bash
./run-ecomanalyser.sh restart
```

### Check Service Status
```bash
./run-ecomanalyser.sh status
```

### View Service Logs
```bash
./run-ecomanalyser.sh logs
```

### Clean Up Everything
```bash
./run-ecomanalyser.sh clean
```

### Show Help
```bash
./run-ecomanalyser.sh help
```

## 🌐 Access URLs

Once running, access your application at:

- **Frontend Application**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **Database**: localhost:5432 (PostgreSQL)

## 📁 Project Structure

The script expects this directory structure:
```
EcomAnalyser/
├── run-ecomanalyser.sh          # This script
├── docker-compose.yml           # Docker services
├── backend/                     # Spring Boot backend
├── frontend/                    # React frontend
└── README.md
```

## 🔧 What the Script Does

### 1. System Requirements Check
- ✅ Docker installation and status
- ✅ Docker Compose availability

### 2. Docker Services
- 🐳 Starts PostgreSQL database
- 🐳 Starts Spring Boot backend container
- 🐳 Starts React frontend container
- ⏳ Waits for all services to be ready

### 3. Service Health Checks
- 🔍 Verifies backend API is responding
- 🔍 Verifies frontend is accessible
- ⏳ Waits for services to be fully operational

### 5. Status Display
- 📊 Shows running services
- 🌐 Displays access URLs
- 📝 Provides usage instructions

## 🛠️ Troubleshooting

### Common Issues

#### Docker Not Running
```bash
# Start Docker Desktop or Docker daemon
# On macOS: open -a Docker
# On Linux: sudo systemctl start docker
```

#### Port Already in Use
```bash
# Check what's using the ports
lsof -i :8080  # Backend port
lsof -i :5173  # Frontend port
lsof -i :5432  # Database port

# Stop conflicting services or use different ports
```



### Manual Service Management

If you need to manage services manually:

```bash
# Start all Docker services
docker-compose up -d

# Start specific services only
docker-compose up -d db        # Database only
docker-compose up -d backend   # Backend only
docker-compose up -d frontend  # Frontend only

# Check Docker status
docker-compose ps

# View Docker logs
docker-compose logs -f
docker-compose logs backend    # Backend logs only
docker-compose logs frontend   # Frontend logs only
```

## 📝 Log Files

The script uses Docker logs for debugging:

- **Backend logs**: `docker-compose logs backend`
- **Frontend logs**: `docker-compose logs frontend`
- **Database logs**: `docker-compose logs db`
- **All services**: `docker-compose logs -f`

## 🔒 Security Notes

- The script runs services on localhost only
- Database credentials are defined in `docker-compose.yml`
- No external network access is required
- All services run in isolated containers

## 🚨 Important Notes

1. **Always run from project root**: The script must be executed from the EcomAnalyser project root directory
2. **Docker must be running**: Ensure Docker Desktop or Docker daemon is started
3. **Ports must be available**: Ports 8080, 5173, and 5432 must be free
4. **First run takes longer**: Initial Docker image building and dependency installation takes time
5. **Use Ctrl+C carefully**: The script handles interruptions gracefully
6. **Everything runs in Docker**: No local Java, Node.js, or Maven installation required

## 🎉 Success Indicators

When everything is running successfully, you should see:

```
[SUCCESS] EcomAnalyser is now running!
[INFO] Access the application at: http://localhost:5173
[INFO] Backend API at: http://localhost:8080
```

## 📞 Support

If you encounter issues:

1. Check the troubleshooting section above
2. Run `./run-ecomanalyser.sh status` to see service status
3. Run `./run-ecomanalyser.sh logs` to view service logs
4. Ensure all prerequisites are met
5. Check that ports are not already in use

---

**Happy Analyzing! 🚀📊**
