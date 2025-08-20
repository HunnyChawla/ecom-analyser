import axios from 'axios';

// Get API URL from environment or use default
const getApiUrl = () => {
  // Check if we're in development mode
  if (import.meta.env.DEV) {
    // In development, use the local IP for network access
    return 'http://192.168.1.8:8080';
  }
  // In production, use environment variable or default
  return import.meta.env.VITE_API_URL || 'http://192.168.1.8:8080';
};

const api = axios.create({ 
  baseURL: getApiUrl(),
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle token expiration
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid, redirect to login
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export { api };
