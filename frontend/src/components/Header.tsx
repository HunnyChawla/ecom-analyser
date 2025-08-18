
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export default function Header() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="bg-gradient-to-r from-blue-600 to-indigo-700 shadow-lg border-b-4 border-blue-500">
      <div className="max-w-7xl mx-auto px-6 py-6">
        {/* Logo and Brand */}
        <div className="flex items-center justify-between mb-6">
          <Link to="/" className="flex items-center space-x-3 group">
            <div className="bg-white rounded-full p-2 shadow-lg group-hover:shadow-xl transition-all duration-300">
              <span className="text-2xl">📊</span>
            </div>
            <div>
              <h1 className="text-3xl font-bold text-white group-hover:text-blue-100 transition-colors">EcomAnalyser</h1>
              <p className="text-blue-100 text-sm">E-commerce Analytics Platform</p>
            </div>
          </Link>
          
          {/* User Info and Logout */}
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-3">
              <div className="bg-white bg-opacity-20 rounded-full p-2">
                <span className="text-white text-lg">👤</span>
              </div>
              <div className="text-white">
                <div className="text-sm opacity-90">Welcome back</div>
                <div className="font-semibold">{user?.firstName} {user?.lastName}</div>
              </div>
            </div>
            
            <button
              onClick={handleLogout}
              className="bg-white bg-opacity-20 hover:bg-opacity-30 text-white px-4 py-2 rounded-lg font-medium transition-all duration-200 hover:shadow-lg"
            >
              🚪 Logout
            </button>
          </div>
        </div>
        
        {/* Navigation Menu */}
        <nav className="flex flex-wrap items-center justify-center space-x-1">
          <NavLink 
            to="/" 
            end 
            className={({isActive}) => 
              `flex items-center space-x-2 px-4 py-3 rounded-lg font-medium transition-all duration-300 ${
                isActive 
                  ? 'bg-white text-blue-700 shadow-lg transform scale-105' 
                  : 'text-white hover:bg-white hover:bg-opacity-20 hover:transform hover:scale-105'
              }`
            }
          >
            <span>📈</span>
            <span>Dashboard</span>
          </NavLink>
          
          <NavLink 
            to="/upload" 
            className={({isActive}) => 
              `flex items-center space-x-2 px-4 py-3 rounded-lg font-medium transition-all duration-300 ${
                isActive 
                  ? 'bg-white text-blue-700 shadow-lg transform scale-105' 
                  : 'text-white hover:bg-white hover:bg-opacity-20 hover:transform hover:scale-105'
              }`
            }
          >
            <span>📤</span>
            <span>Upload</span>
          </NavLink>
          
          <NavLink 
            to="/sku-groups" 
            className={({isActive}) => 
              `flex items-center space-x-2 px-4 py-3 rounded-lg font-medium transition-all duration-300 ${
                isActive 
                  ? 'bg-white text-blue-700 shadow-lg transform scale-105' 
                  : 'text-white hover:bg-white hover:bg-opacity-20 hover:transform hover:scale-105'
              }`
            }
          >
            <span>🏷️</span>
            <span>SKU Groups</span>
          </NavLink>
          
          <NavLink 
            to="/data-merge" 
            className={({isActive}) => 
              `flex items-center space-x-2 px-4 py-3 rounded-lg font-medium transition-all duration-300 ${
                isActive 
                  ? 'bg-white text-blue-700 shadow-lg transform scale-105' 
                  : 'text-white hover:bg-white hover:bg-opacity-20 hover:transform hover:scale-105'
              }`
            }
          >
            <span>🔄</span>
            <span>Data Merge</span>
          </NavLink>
          
          <NavLink 
            to="/loss-analysis" 
            className={({isActive}) => 
              `flex items-center space-x-2 px-4 py-3 rounded-lg font-medium transition-all duration-300 ${
                isActive 
                  ? 'bg-white text-blue-700 shadow-lg transform scale-105' 
                  : 'text-white hover:bg-white hover:bg-opacity-20 hover:transform hover:scale-105'
              }`
            }
          >
            <span>📉</span>
            <span>Loss Analysis</span>
          </NavLink>
          
          <NavLink 
            to="/return-analysis" 
            className={({isActive}) => 
              `flex items-center space-x-2 px-4 py-3 rounded-lg font-medium transition-all duration-300 ${
                isActive 
                  ? 'bg-white text-blue-700 shadow-lg transform scale-105' 
                  : 'text-white hover:bg-white hover:bg-opacity-20 hover:transform hover:scale-105'
              }`
            }
          >
            <span>↩️</span>
            <span>Return Analysis</span>
          </NavLink>
          
          <NavLink 
            to="/return-tracking" 
            className={({isActive}) => 
              `flex items-center space-x-2 px-4 py-3 rounded-lg font-medium transition-all duration-300 ${
                isActive 
                  ? 'bg-white text-blue-700 shadow-lg transform scale-105' 
                  : 'text-white hover:bg-white hover:bg-opacity-20 hover:transform hover:scale-105'
              }`
            }
          >
            <span>📋</span>
            <span>Return Tracking</span>
          </NavLink>
        </nav>
      </div>
    </header>
  );
}
