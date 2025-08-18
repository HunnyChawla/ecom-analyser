import { BrowserRouter, Routes, Route, Link, NavLink } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import { ProtectedRoute } from './components/ProtectedRoute'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import UploadData from './pages/UploadData'
import SkuGroupManagement from './pages/SkuGroupManagement'
import DataMerge from './pages/DataMerge'
import LossAnalysis from './pages/LossAnalysis'
import ReturnAnalysis from './pages/ReturnAnalysis'
import ReturnTracking from './pages/ReturnTracking'

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public route */}
          <Route path="/login" element={<Login />} />
          
          {/* Protected routes */}
          <Route path="/" element={
            <ProtectedRoute>
              <div className="min-h-screen bg-gray-50 text-gray-900">
                <Header />
                <main className="max-w-7xl mx-auto p-6">
                  <Dashboard />
                </main>
              </div>
            </ProtectedRoute>
          } />
          
          <Route path="/upload" element={
            <ProtectedRoute>
              <div className="min-h-screen bg-gray-50 text-gray-900">
                <Header />
                <main className="max-w-7xl mx-auto p-6">
                  <UploadData />
                </main>
              </div>
            </ProtectedRoute>
          } />
          
          <Route path="/sku-groups" element={
            <ProtectedRoute>
              <div className="min-h-screen bg-gray-50 text-gray-900">
                <Header />
                <main className="max-w-7xl mx-auto p-6">
                  <SkuGroupManagement />
                </main>
              </div>
            </ProtectedRoute>
          } />
          
          <Route path="/data-merge" element={
            <ProtectedRoute>
              <div className="min-h-screen bg-gray-50 text-gray-900">
                <Header />
                <main className="max-w-7xl mx-auto p-6">
                  <DataMerge />
                </main>
              </div>
            </ProtectedRoute>
          } />
          
          <Route path="/loss-analysis" element={
            <ProtectedRoute>
              <div className="min-h-screen bg-gray-50 text-gray-900">
                <Header />
                <main className="max-w-7xl mx-auto p-6">
                  <LossAnalysis />
                </main>
              </div>
            </ProtectedRoute>
          } />
          
          <Route path="/return-analysis" element={
            <ProtectedRoute>
              <div className="min-h-screen bg-gray-50 text-gray-900">
                <Header />
                <main className="max-w-7xl mx-auto p-6">
                  <ReturnAnalysis />
                </main>
              </div>
            </ProtectedRoute>
          } />
          
          <Route path="/return-tracking" element={
            <ProtectedRoute>
              <div className="min-h-screen bg-gray-50 text-gray-900">
                <Header />
                <main className="max-w-7xl mx-auto p-6">
                  <ReturnTracking />
                </main>
              </div>
            </ProtectedRoute>
          } />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}

export default App
