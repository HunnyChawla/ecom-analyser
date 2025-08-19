#!/bin/bash

# Network Access Test Script for EcomAnalyser
# This script tests if your application is accessible via local IP

echo "🌐 Testing Network Access for EcomAnalyser"
echo "=========================================="

# Get local IP
LOCAL_IP=$(ifconfig | grep "inet " | grep -v 127.0.0.1 | awk '{print $2}' | head -1)
echo "📍 Your Local IP: $LOCAL_IP"
echo ""

# Test Frontend
echo "🧪 Testing Frontend (Port 5173)..."
if curl -s -m 10 "http://$LOCAL_IP:5173" > /dev/null; then
    echo "✅ Frontend is accessible at: http://$LOCAL_IP:5173"
else
    echo "❌ Frontend is NOT accessible at: http://$LOCAL_IP:5173"
fi

# Test Backend
echo "🧪 Testing Backend (Port 8080)..."
if curl -s -m 10 "http://$LOCAL_IP:8080/api/auth/health" > /dev/null; then
    echo "✅ Backend is accessible at: http://$LOCAL_IP:8080"
else
    echo "❌ Backend is NOT accessible at: http://$LOCAL_IP:8080"
fi

# Test Database Port
echo "🧪 Testing Database Port (Port 5432)..."
if nc -z -w5 $LOCAL_IP 5432 2>/dev/null; then
    echo "✅ Database port is accessible at: $LOCAL_IP:5432"
else
    echo "❌ Database port is NOT accessible at: $LOCAL_IP:5432"
fi

echo ""
echo "🔍 Network Access Summary:"
echo "=========================="
echo "Frontend:  http://$LOCAL_IP:5173"
echo "Backend:   http://$LOCAL_IP:8080"
echo "Database:  $LOCAL_IP:5432"
echo ""

# Check if ports are bound to network interface
echo "🔧 Port Binding Status:"
echo "======================="

if lsof -i :5173 | grep -q "$LOCAL_IP"; then
    echo "✅ Frontend port 5173 bound to $LOCAL_IP"
else
    echo "❌ Frontend port 5173 NOT bound to $LOCAL_IP"
fi

if lsof -i :8080 | grep -q "$LOCAL_IP"; then
    echo "✅ Backend port 8080 bound to $LOCAL_IP"
else
    echo "❌ Backend port 8080 NOT bound to $LOCAL_IP"
fi

if lsof -i :5432 | grep -q "$LOCAL_IP"; then
    echo "✅ Database port 5432 bound to $LOCAL_IP"
else
    echo "❌ Database port 5432 NOT bound to $LOCAL_IP"
fi

echo ""
echo "📱 To test from other devices:"
echo "1. Connect to the same WiFi network"
echo "2. Open browser and go to: http://$LOCAL_IP:5173"
echo "3. You should see the EcomAnalyser login page"
echo ""

# Test actual content
echo "🧪 Testing Frontend Content..."
FRONTEND_CONTENT=$(curl -s -m 10 "http://$LOCAL_IP:5173" | head -1)
if [[ "$FRONTEND_CONTENT" == *"<!doctype html>"* ]]; then
    echo "✅ Frontend is serving HTML content correctly"
else
    echo "❌ Frontend is not serving expected content"
fi

echo ""
echo "🎉 Network access test completed!"
echo "If all tests pass, your app should be accessible from other devices on the same network."
