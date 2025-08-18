import { BrowserRouter, Routes, Route, Link, NavLink } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import UploadData from './pages/UploadData'
import SkuGroupManagement from './pages/SkuGroupManagement'
import DataMerge from './pages/DataMerge'
import LossAnalysis from './pages/LossAnalysis'
import ReturnAnalysis from './pages/ReturnAnalysis'
import ReturnTracking from './pages/ReturnTracking'

function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen bg-gray-50 text-gray-900">
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
              
              {/* User Info Placeholder */}
              <div className="flex items-center space-x-3">
                <div className="bg-white bg-opacity-20 rounded-full p-2">
                  <span className="text-white text-lg">👤</span>
                </div>
                <div className="text-white">
                  <div className="text-sm opacity-90">Welcome back</div>
                  <div className="font-semibold">Admin User</div>
                </div>
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
        <main className="max-w-7xl mx-auto p-6">
          <Routes>
            <Route path="/" element={<Dashboard/>} />
            <Route path="/upload" element={<UploadData/>} />
            <Route path="/sku-groups" element={<SkuGroupManagement/>} />
            <Route path="/data-merge" element={<DataMerge/>} />
                                    <Route path="/loss-analysis" element={<LossAnalysis/>} />
                        <Route path="/return-analysis" element={<ReturnAnalysis/>} />
                        <Route path="/return-tracking" element={<ReturnTracking/>} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}

export default App
