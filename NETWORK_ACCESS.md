# Network Access Configuration for EcomAnalyser

This document explains how to access your EcomAnalyser application from other devices on your local network.

## 🌐 **Network Access URLs**

### **Your Local IP Address: 192.168.1.8**

### **Development Environment**
- **Frontend Application**: http://192.168.1.8:5173
- **Backend API**: http://192.168.1.8:8080
- **Database**: 192.168.1.8:5432

### **Production Environment**
- **Frontend Application**: http://192.168.1.8 (port 80)
- **Backend API**: http://192.168.1.8:8080
- **Database**: 192.168.1.8:5432

## 🔧 **Configuration Changes Made**

### **Backend (Spring Boot)**
- **Server binding**: `0.0.0.0:8080` (all network interfaces)
- **CORS**: Configured to allow network access
- **Security**: Network-accessible endpoints

### **Frontend (React/Vite)**
- **Vite server**: `host: '0.0.0.0'` (all network interfaces)
- **Proxy configuration**: API calls routed to backend
- **Development mode**: Hot reloading works on network

### **Docker Configuration**
- **Port bindings**: `0.0.0.0:PORT:PORT` (all interfaces)
- **Network access**: Services accessible from host network
- **Container networking**: Proper inter-service communication

## 📱 **Accessing from Other Devices**

### **On the Same Network**
1. **Find your computer's IP**: `192.168.1.8`
2. **Open browser** on other device
3. **Navigate to**: `http://192.168.1.8:5173`

### **Mobile Devices**
- **Android**: Chrome, Firefox, Safari
- **iOS**: Safari, Chrome
- **URL**: `http://192.168.1.8:5173`

### **Other Computers**
- **Windows**: Edge, Chrome, Firefox
- **macOS**: Safari, Chrome, Firefox
- **Linux**: Any modern browser
- **URL**: `http://192.168.1.8:5173`

## 🚨 **Security Considerations**

### **Development Environment**
- **Network accessible** - Other devices can access
- **No authentication** - Development mode
- **Hot reloading** - Code changes visible to all users

### **Production Environment**
- **Network accessible** - Other devices can access
- **Authentication required** - Login system active
- **Secure endpoints** - API protected

## 🔍 **Troubleshooting Network Access**

### **Can't Access from Other Devices**

#### **Check Firewall Settings**
```bash
# macOS - Allow incoming connections
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --add /usr/bin/java
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --add /usr/bin/node

# Windows - Allow through Windows Firewall
# Control Panel > System and Security > Windows Firewall > Allow an app
```

#### **Check Network Configuration**
```bash
# Verify IP address
ifconfig | grep "inet " | grep -v 127.0.0.1

# Test local connectivity
ping 192.168.1.8

# Check if ports are listening
netstat -an | grep :5173
netstat -an | grep :8080
```

#### **Check Docker Network**
```bash
# Verify containers are running
docker-compose ps

# Check container logs
docker-compose logs frontend
docker-compose logs backend

# Verify port bindings
docker port ecomanalyser_frontend
docker port ecomanalyser_backend
```

### **Common Issues**

#### **Port Already in Use**
```bash
# Check what's using the ports
lsof -i :5173
lsof -i :8080

# Stop conflicting services
sudo lsof -ti:5173 | xargs kill -9
sudo lsof -ti:8080 | xargs kill -9
```

#### **Docker Network Issues**
```bash
# Restart Docker services
docker-compose down
docker-compose up -d

# Check Docker network
docker network ls
docker network inspect ecomanalyser_default
```

## 🌍 **Advanced Network Configuration**

### **Custom Network Interface**
If you want to bind to a specific network interface:

```yaml
# In docker-compose.yml
ports:
  - "192.168.1.8:5173:5173"  # Specific IP
  - "0.0.0.0:8080:8080"      # All interfaces
```

### **Network Port Forwarding**
For external network access (router configuration):

1. **Access router admin** (usually 192.168.1.1)
2. **Port forwarding rules**:
   - Port 5173 → 192.168.1.8:5173 (Frontend)
   - Port 8080 → 192.168.1.8:8080 (Backend)
3. **External access**: `http://YOUR_PUBLIC_IP:5173`

### **VPN Access**
For secure remote access:

1. **Set up VPN** (OpenVPN, WireGuard)
2. **Configure routing** to include local network
3. **Access via VPN IP**: `http://VPN_IP:5173`

## 📊 **Testing Network Access**

### **Local Testing**
```bash
# Test from your machine
curl http://localhost:5173
curl http://192.168.1.8:5173
curl http://localhost:8080/actuator/health
curl http://192.168.1.8:8080/actuator/health
```

### **Network Testing**
```bash
# Test from another device on network
curl http://192.168.1.8:5173
curl http://192.168.1.8:8080/actuator/health
```

### **Browser Testing**
1. **Open browser** on another device
2. **Navigate to**: `http://192.168.1.8:5173`
3. **Verify**: Application loads correctly
4. **Test functionality**: Login, navigation, features

## 🎯 **Best Practices**

1. **Use HTTPS** in production for security
2. **Implement authentication** for network access
3. **Monitor access logs** for security
4. **Regular security updates** for containers
5. **Network segmentation** for production deployments
6. **Backup configurations** for easy recovery

## 🔄 **Updating Network Configuration**

### **After IP Changes**
If your local IP changes:

1. **Update this file** with new IP
2. **Restart services**: `./run-ecomanalyser.sh restart`
3. **Update other devices** with new URL
4. **Test connectivity** from all devices

### **Network Changes**
If you change networks:

1. **Check new IP**: `ifconfig` or `ipconfig`
2. **Update configurations** if needed
3. **Test access** from new network devices
4. **Update documentation** with new IPs

---

## 🎉 **Network Access Ready!**

Your EcomAnalyser application is now accessible from:
- **Local machine**: `http://localhost:5173`
- **Network devices**: `http://192.168.1.8:5173`
- **Mobile devices**: Same network IP
- **Other computers**: Same network IP

**Happy Networking! 🌐📱💻**
