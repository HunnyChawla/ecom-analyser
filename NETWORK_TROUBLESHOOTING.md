# Network Access Troubleshooting Guide

If you're unable to access your EcomAnalyser application from other devices on the same network, follow this troubleshooting guide step by step.

## 🔍 **Step-by-Step Troubleshooting**

### **1. Verify Application is Running**

```bash
# Check if all containers are running
docker-compose ps

# Expected output should show:
# - ecomanalyser_db (Up)
# - ecomanalyser_backend (Up) 
# - ecomanalyser_frontend (Up)
```

### **2. Check Port Bindings**

```bash
# Verify ports are bound to your local IP
lsof -i :5173  # Frontend
lsof -i :8080  # Backend
lsof -i :5432  # Database

# Look for entries like:
# COMMAND  PID USER   FD   TYPE DEVICE SIZE/OFF NODE NAME
# com.dock 123 user   12u  IPv6 0x...      0t0  TCP 192.168.1.8:5173 (LISTEN)
```

### **3. Test Local Access**

```bash
# Test from your machine
curl http://localhost:5173
curl http://localhost:8080/api/auth/health

# Test from your local IP
curl http://192.168.1.8:5173
curl http://192.168.1.8:8080/api/auth/health
```

### **4. Check Network Connectivity**

```bash
# Test network connectivity
ping 192.168.1.8

# Check your network interface
ifconfig | grep "inet 192.168.1.8"
```

### **5. Verify Firewall Settings**

```bash
# Check macOS firewall status
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate

# If firewall is enabled, allow Docker
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --add /usr/bin/docker
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --add /usr/bin/java
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --add /usr/bin/node
```

## 🚨 **Common Issues and Solutions**

### **Issue 1: Ports Not Bound to Network Interface**

**Symptoms**: Can access locally but not from network
**Solution**: Update docker-compose.yml with specific IP binding

```yaml
ports:
  - "192.168.1.8:5173:5173"  # Frontend
  - "192.168.1.8:8080:8080"  # Backend
  - "192.168.1.8:5432:5432"  # Database
```

### **Issue 2: Docker Network Configuration**

**Symptoms**: Containers running but ports not accessible
**Solution**: Restart Docker services

```bash
# Stop all services
docker-compose down

# Start with explicit network binding
docker-compose up -d

# Verify port bindings
docker port ecomanalyser_frontend
docker port ecomanalyser_backend
```

### **Issue 3: macOS Network Interface Issues**

**Symptoms**: IP address changes or network interface down
**Solution**: Check and restart network services

```bash
# Check network interface status
ifconfig en0

# Restart network interface (if needed)
sudo ifconfig en0 down
sudo ifconfig en0 up

# Get new IP address
ifconfig | grep "inet " | grep -v 127.0.0.1
```

### **Issue 4: Router/Network Configuration**

**Symptoms**: Can't access from any network device
**Solution**: Check router settings

1. **Access router admin** (usually 192.168.1.1)
2. **Check DHCP settings** - ensure your device has static IP
3. **Verify port forwarding** (if needed)
4. **Check network isolation** - ensure devices are on same network

## 🔧 **Advanced Configuration**

### **Explicit Network Binding**

Update your `docker-compose.yml`:

```yaml
version: '3.9'
services:
  frontend:
    ports:
      - "192.168.1.8:5173:5173"  # Bind to specific IP
    environment:
      - VITE_HOST=0.0.0.0
      - VITE_PORT=5173
  
  backend:
    ports:
      - "192.168.1.8:8080:8080"  # Bind to specific IP
    environment:
      - SERVER_ADDRESS=0.0.0.0
      - SERVER_PORT=8080
```

### **Custom Docker Network**

Create a custom network for better control:

```yaml
networks:
  ecomanalyser_network:
    driver: bridge
    ipam:
      config:
        - subnet: 192.168.1.0/24

services:
  frontend:
    networks:
      - ecomanalyser_network
  backend:
    networks:
      - ecomanalyser_network
  db:
    networks:
      - ecomanalyser_network
```

## 📱 **Testing from Other Devices**

### **Mobile Device Testing**

1. **Connect to same WiFi network**
2. **Open browser** (Chrome, Safari, Firefox)
3. **Navigate to**: `http://192.168.1.8:5173`
4. **Expected**: EcomAnalyser login page loads

### **Computer Testing**

1. **Same network** (WiFi or Ethernet)
2. **Open any browser**
3. **Navigate to**: `http://192.168.1.8:5173`
4. **Expected**: Application loads with full functionality

### **Network Scanner Testing**

Use network scanning tools to verify accessibility:

```bash
# Install nmap (if not available)
brew install nmap

# Scan your local network
nmap -p 5173,8080,5432 192.168.1.8

# Expected output:
# PORT     STATE  SERVICE
# 5173/tcp open   unknown
# 8080/tcp open   http-alt
# 5432/tcp open   postgresql
```

## 🚀 **Quick Fix Commands**

### **Complete Reset and Restart**

```bash
# Stop everything
docker-compose down

# Remove all containers and networks
docker system prune -f

# Rebuild and start
docker-compose build --no-cache
docker-compose up -d

# Wait for services to start
sleep 20

# Test access
curl http://192.168.1.8:5173
curl http://192.168.1.8:8080/api/auth/health
```

### **Check Service Health**

```bash
# Frontend health
curl -I http://192.168.1.8:5173

# Backend health
curl -I http://192.168.1.8:8080/api/auth/health

# Database connectivity
telnet 192.168.1.8 5432
```

## 🔍 **Diagnostic Commands**

### **Network Diagnostics**

```bash
# Check all listening ports
sudo lsof -i -P | grep LISTEN

# Check Docker network
docker network ls
docker network inspect ecomanalyser_default

# Check container logs
docker-compose logs frontend
docker-compose logs backend
docker-compose logs db
```

### **System Diagnostics**

```bash
# Check network interfaces
ifconfig -a

# Check routing table
netstat -rn

# Check firewall rules
sudo pfctl -s rules
```

## 📞 **Still Having Issues?**

If you're still unable to access the application from other devices:

1. **Check router settings** - ensure no network isolation
2. **Verify IP address** - ensure it hasn't changed
3. **Test with different devices** - try multiple phones/computers
4. **Check network segmentation** - ensure all devices are on same subnet
5. **Contact network admin** - if on corporate/restricted network

## 🎯 **Success Indicators**

You'll know it's working when:

✅ **Local access works**: `http://localhost:5173`  
✅ **Network access works**: `http://192.168.1.8:5173`  
✅ **Other devices can access**: Mobile phones, other computers  
✅ **All services respond**: Frontend, backend, database  
✅ **No firewall blocks**: Ports are open and accessible  

---

## 🎉 **Quick Test Checklist**

- [ ] All containers running (`docker-compose ps`)
- [ ] Ports bound to network IP (`lsof -i :5173`)
- [ ] Local access working (`curl localhost:5173`)
- [ ] Network access working (`curl 192.168.1.8:5173`)
- [ ] Firewall disabled or configured
- [ ] Same network for all devices
- [ ] IP address hasn't changed

**Follow this guide step by step, and your network access should work! 🚀🌐**
